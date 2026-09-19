export interface Holding {
  id: string
  ticker: string
  shares: number
  buyPrice: number
  // Currency the buy price was entered in. Absent on holdings created before this
  // field existed — treat as 'USD' (the prior, only, assumption) wherever read.
  buyPriceCurrency?: Currency
  buyDate?: string
  notes?: string
}

export interface HoldingInput {
  ticker: string
  shares: number
  buyPrice: number
  buyPriceCurrency: Currency
  buyDate?: string
  notes?: string
}

export type Currency = 'USD' | 'KRW'
export type WindowMode = 'widget' | 'full'

export interface WindowBounds {
  x: number
  y: number
  width: number
  height: number
}

export interface Settings {
  refreshIntervalMs: number
  displayCurrency: Currency
  windowMode: WindowMode
  startAtLogin?: boolean
  widgetBounds?: WindowBounds
  fullBounds?: WindowBounds
}

export interface QuoteInfo {
  price: number
  currency: string
  asOf: string
}

export type QuoteMap = Record<string, QuoteInfo>

export interface FxRates {
  /** Units of each currency per 1 USD, e.g. { USD: 1, KRW: 1388.28 }. */
  rates: Record<string, number>
  asOf: string
}

export interface AppState {
  holdings: Holding[]
  settings: Settings
  quotes: QuoteMap
  fx: FxRates | null
}

export interface QuotesUpdatePayload {
  quotes: QuoteMap
  fx: FxRates
}
