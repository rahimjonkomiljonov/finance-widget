import { BrowserWindow, screen, shell } from 'electron'
import { join } from 'path'
import { is } from '@electron-toolkit/utils'
import { getSettings, updateSettings } from './store'
import { createFallbackIcon } from './services/icon'
import type { WindowBounds, WindowMode } from '../shared/types'

const WIDGET_DEFAULT_SIZE = { width: 340, height: 480 }
const FULL_DEFAULT_SIZE = { width: 900, height: 650 }
const BOUNDS_SAVE_DEBOUNCE_MS = 500

let currentWindow: BrowserWindow | null = null
let currentMode: WindowMode = 'widget'
let isQuitting = false
let boundsSaveTimer: ReturnType<typeof setTimeout> | null = null

export function setQuitting(value: boolean): void {
  isQuitting = value
}

export function getCurrentWindow(): BrowserWindow | null {
  return currentWindow
}

export function getCurrentMode(): WindowMode {
  return currentMode
}

function boundsKeyFor(mode: WindowMode): 'widgetBounds' | 'fullBounds' {
  return mode === 'widget' ? 'widgetBounds' : 'fullBounds'
}

function saveBoundsDebounced(win: BrowserWindow, mode: WindowMode): void {
  if (boundsSaveTimer) clearTimeout(boundsSaveTimer)
  boundsSaveTimer = setTimeout(() => {
    if (win.isDestroyed()) return
    const bounds: WindowBounds = win.getBounds()
    updateSettings({ [boundsKeyFor(mode)]: bounds })
  }, BOUNDS_SAVE_DEBOUNCE_MS)
}

/** First-run widget placement: top-right of the work area, where desktop widgets live. */
function defaultWidgetPosition(): { x: number; y: number } {
  const { workArea } = screen.getPrimaryDisplay()
  return {
    x: workArea.x + workArea.width - WIDGET_DEFAULT_SIZE.width - 24,
    y: workArea.y + 24
  }
}

export function createWindowForMode(mode: WindowMode): BrowserWindow {
  const settings = getSettings()
  const savedBounds = mode === 'widget' ? settings.widgetBounds : settings.fullBounds
  const defaultSize =
    mode === 'widget'
      ? { ...WIDGET_DEFAULT_SIZE, ...defaultWidgetPosition() }
      : FULL_DEFAULT_SIZE

  const win = new BrowserWindow({
    ...defaultSize,
    ...savedBounds,
    frame: mode === 'full',
    alwaysOnTop: mode === 'widget' && settings.widgetOnTop === true,
    skipTaskbar: mode === 'widget',
    resizable: true,
    show: false,
    autoHideMenuBar: true,
    backgroundColor: '#1e1e1e',
    icon: createFallbackIcon(),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false
    }
  })

  currentWindow = win
  currentMode = mode

  win.on('ready-to-show', () => win.show())

  win.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
    return { action: 'deny' }
  })

  win.on('move', () => saveBoundsDebounced(win, mode))
  win.on('resize', () => saveBoundsDebounced(win, mode))

  win.on('close', (event) => {
    if (!isQuitting) {
      event.preventDefault()
      win.hide()
    }
  })

  const query = { mode }
  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    const url = new URL(process.env['ELECTRON_RENDERER_URL'])
    url.searchParams.set('mode', mode)
    win.loadURL(url.toString())
  } else {
    win.loadFile(join(__dirname, '../renderer/index.html'), { query })
  }

  return win
}

/** Pin or unpin the widget above other windows; applies to the open widget immediately. */
export function setWidgetOnTop(enabled: boolean): void {
  updateSettings({ widgetOnTop: enabled })
  if (currentMode === 'widget' && currentWindow && !currentWindow.isDestroyed()) {
    currentWindow.setAlwaysOnTop(enabled)
  }
}

export function switchWindowMode(mode: WindowMode): BrowserWindow {
  if (currentMode === mode && currentWindow && !currentWindow.isDestroyed()) {
    return currentWindow
  }

  const previous = currentWindow
  const win = createWindowForMode(mode)
  updateSettings({ windowMode: mode })

  if (previous && !previous.isDestroyed()) {
    previous.removeAllListeners('close')
    previous.destroy()
  }

  return win
}
