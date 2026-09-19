import { BrowserWindow, ipcMain } from 'electron'
import { v4 as uuidv4 } from 'uuid'
import {
  getHoldings,
  setHoldings,
  getSettings,
  updateSettings,
  getQuoteCache,
  getFxRatesCache
} from './store'
import type { RefreshScheduler } from './services/refreshScheduler'
import { exportHoldingsToCsv, type CsvExportResult } from './services/csvExport'
import { setWidgetOnTop, switchWindowMode } from './windowManager'
import { refreshTrayMenu } from './tray'
import { applyStartAtLogin } from './services/startAtLogin'
import type { AppState, Currency, Holding, HoldingInput, WindowMode } from '../shared/types'

function getState(): AppState {
  return {
    holdings: getHoldings(),
    settings: getSettings(),
    quotes: getQuoteCache(),
    fx: getFxRatesCache()
  }
}

export function registerIpcHandlers(scheduler: RefreshScheduler): void {
  ipcMain.handle('state:get', () => getState())

  ipcMain.handle('holdings:add', (_e, input: HoldingInput): Holding => {
    const holding: Holding = {
      id: uuidv4(),
      ticker: input.ticker.trim().toUpperCase(),
      shares: input.shares,
      buyPrice: input.buyPrice,
      buyPriceCurrency: input.buyPriceCurrency,
      buyDate: input.buyDate,
      notes: input.notes
    }
    setHoldings([...getHoldings(), holding])
    void scheduler.refreshNow()
    return holding
  })

  ipcMain.handle(
    'holdings:update',
    (_e, id: string, patch: Partial<HoldingInput>): Holding | null => {
      const holdings = getHoldings()
      const next = holdings.map((h) =>
        h.id === id
          ? { ...h, ...patch, ticker: patch.ticker ? patch.ticker.trim().toUpperCase() : h.ticker }
          : h
      )
      setHoldings(next)
      return next.find((h) => h.id === id) ?? null
    }
  )

  ipcMain.handle('holdings:remove', (_e, id: string): boolean => {
    setHoldings(getHoldings().filter((h) => h.id !== id))
    return true
  })

  ipcMain.handle('settings:setCurrency', (_e, currency: Currency) => {
    return updateSettings({ displayCurrency: currency })
  })

  ipcMain.handle('quotes:refresh', () => scheduler.refreshNow())

  ipcMain.handle('settings:setInterval', (_e, ms: number) => {
    scheduler.setInterval(ms)
    return updateSettings({ refreshIntervalMs: ms })
  })

  ipcMain.handle(
    'export:csv',
    (event, rows: Record<string, string | number>[]): Promise<CsvExportResult> =>
      exportHoldingsToCsv(rows, BrowserWindow.fromWebContents(event.sender))
  )

  ipcMain.handle('window:setMode', (_e, mode: WindowMode) => {
    switchWindowMode(mode)
  })

  ipcMain.handle('settings:setWidgetOnTop', (_e, enabled: boolean) => {
    setWidgetOnTop(enabled)
    refreshTrayMenu()
    return getSettings()
  })

  ipcMain.handle('settings:setStartAtLogin', (_e, enabled: boolean) => {
    applyStartAtLogin(enabled)
    return getSettings()
  })
}
