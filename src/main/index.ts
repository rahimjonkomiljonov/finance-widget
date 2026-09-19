import './devUserData'
import { app, BrowserWindow } from 'electron'
import { electronApp, optimizer } from '@electron-toolkit/utils'
import { registerIpcHandlers } from './ipcHandlers'
import { YahooFinanceProvider } from './services/priceProvider'
import { RefreshScheduler, type RefreshPayload } from './services/refreshScheduler'
import { getHoldings, getSettings } from './store'
import { createWindowForMode, getCurrentWindow, setQuitting } from './windowManager'
import { createTray } from './tray'
import { applyStartAtLogin } from './services/startAtLogin'

function broadcastQuotes(payload: RefreshPayload): void {
  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send('quotes:updated', payload)
  }
}

function showExistingWindow(): void {
  const win = getCurrentWindow()
  if (!win) return
  if (win.isMinimized()) win.restore()
  win.show()
  win.focus()
}

// The widget may already be running from Windows login; opening it again from the
// Start Menu should surface that window, not start a second copy with its own tray icon.
if (!app.requestSingleInstanceLock()) {
  app.quit()
} else {
  app.on('second-instance', showExistingWindow)
  app.whenReady().then(startApp)
}

function startApp(): void {
  electronApp.setAppUserModelId('com.rahimjon.financewidget')

  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  // Constructing the price provider must never block the window from appearing:
  // if it throws (e.g. a breaking change in the unofficial Yahoo Finance client),
  // fall back to a no-op scheduler so the app still starts, just without live prices.
  let scheduler: RefreshScheduler
  try {
    const provider = new YahooFinanceProvider()
    scheduler = new RefreshScheduler(
      provider,
      getSettings().refreshIntervalMs,
      () => getHoldings().map((h) => h.ticker),
      broadcastQuotes
    )
  } catch (err) {
    console.error('Failed to initialize price provider, prices will be unavailable:', err)
    scheduler = new RefreshScheduler(
      {
        getQuotes: async () => ({}),
        getRates: async () => {
          throw new Error('price provider unavailable')
        }
      },
      getSettings().refreshIntervalMs,
      () => [],
      broadcastQuotes
    )
  }

  registerIpcHandlers(scheduler)

  applyStartAtLogin()

  createWindowForMode(getSettings().windowMode)
  createTray()

  scheduler.start()

  app.on('activate', () => {
    const win = getCurrentWindow()
    if (win) {
      win.show()
    } else {
      createWindowForMode(getSettings().windowMode)
    }
  })
}

app.on('before-quit', () => {
  setQuitting(true)
})
