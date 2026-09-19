import Store from 'electron-store'
import type { Holding, Settings, QuoteMap, FxRates } from '../shared/types'

interface StoreSchema {
  holdings: Holding[]
  settings: Settings
  quoteCache: QuoteMap
  fxRatesCache: FxRates | null
}

const defaults: StoreSchema = {
  holdings: [],
  settings: {
    refreshIntervalMs: 5 * 60_000,
    displayCurrency: 'USD',
    windowMode: 'widget',
    startAtLogin: true
  },
  quoteCache: {},
  fxRatesCache: null
}

export const store = new Store<StoreSchema>({ defaults })

export function getHoldings(): Holding[] {
  return store.get('holdings')
}

export function setHoldings(holdings: Holding[]): void {
  store.set('holdings', holdings)
}

export function getSettings(): Settings {
  return store.get('settings')
}

export function updateSettings(patch: Partial<Settings>): Settings {
  const next = { ...getSettings(), ...patch }
  store.set('settings', next)
  return next
}

export function getQuoteCache(): QuoteMap {
  return store.get('quoteCache')
}

export function setQuoteCache(quotes: QuoteMap): void {
  store.set('quoteCache', quotes)
}

export function getFxRatesCache(): FxRates | null {
  return store.get('fxRatesCache')
}

export function setFxRatesCache(fx: FxRates): void {
  store.set('fxRatesCache', fx)
}
