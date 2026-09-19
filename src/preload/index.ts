import { contextBridge, ipcRenderer } from 'electron'
import { electronAPI } from '@electron-toolkit/preload'
import type {
  AppState,
  Currency,
  Holding,
  HoldingInput,
  QuotesUpdatePayload,
  Settings,
  WindowMode
} from '../shared/types'

const api = {
  getState: (): Promise<AppState> => ipcRenderer.invoke('state:get'),
  addHolding: (input: HoldingInput): Promise<Holding> => ipcRenderer.invoke('holdings:add', input),
  updateHolding: (id: string, patch: Partial<HoldingInput>): Promise<Holding | null> =>
    ipcRenderer.invoke('holdings:update', id, patch),
  removeHolding: (id: string): Promise<boolean> => ipcRenderer.invoke('holdings:remove', id),
  setCurrency: (currency: Currency): Promise<Settings> =>
    ipcRenderer.invoke('settings:setCurrency', currency),
  refreshNow: (): Promise<QuotesUpdatePayload> => ipcRenderer.invoke('quotes:refresh'),
  setRefreshInterval: (ms: number): Promise<Settings> =>
    ipcRenderer.invoke('settings:setInterval', ms),
  onQuotesUpdated: (callback: (payload: QuotesUpdatePayload) => void): (() => void) => {
    const listener = (_event: Electron.IpcRendererEvent, payload: QuotesUpdatePayload): void =>
      callback(payload)
    ipcRenderer.on('quotes:updated', listener)
    return () => ipcRenderer.removeListener('quotes:updated', listener)
  },
  exportCsv: (
    rows: Record<string, string | number>[]
  ): Promise<{ canceled: boolean; filePath?: string }> => ipcRenderer.invoke('export:csv', rows),
  setWindowMode: (mode: WindowMode): Promise<void> => ipcRenderer.invoke('window:setMode', mode),
  setStartAtLogin: (enabled: boolean): Promise<Settings> =>
    ipcRenderer.invoke('settings:setStartAtLogin', enabled)
}

if (process.contextIsolated) {
  try {
    contextBridge.exposeInMainWorld('electron', electronAPI)
    contextBridge.exposeInMainWorld('api', api)
  } catch (error) {
    console.error(error)
  }
} else {
  // @ts-ignore (define in dts)
  window.electron = electronAPI
  // @ts-ignore (define in dts)
  window.api = api
}

export type Api = typeof api
