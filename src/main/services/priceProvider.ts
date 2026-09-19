import YahooFinanceImport from 'yahoo-finance2'
import type { QuoteMap } from '../../shared/types'

// electron-vite's esbuild bundling of this externalized CJS dependency does a
// plain `require()` without unwrapping `.default`, so the import can be either
// the constructor itself or `{ default: constructor }` depending on build mode.
const YahooFinance = (
  (YahooFinanceImport as unknown as { default?: typeof YahooFinanceImport }).default ??
  YahooFinanceImport
) as typeof YahooFinanceImport

export interface PriceProvider {
  getQuotes(tickers: string[]): Promise<QuoteMap>
  /** Returns units of each requested currency per 1 USD. */
  getRates(currencies: string[]): Promise<Record<string, number>>
}

export class YahooFinanceProvider implements PriceProvider {
  private client = new YahooFinance({ suppressNotices: ['yahooSurvey'] })

  async getQuotes(tickers: string[]): Promise<QuoteMap> {
    if (tickers.length === 0) return {}

    const results = tickers.length === 1
      ? [await this.client.quote(tickers[0])]
      : await this.client.quote(tickers)

    const now = new Date().toISOString()
    const quotes: QuoteMap = {}
    for (const q of results) {
      if (!q?.symbol || typeof q.regularMarketPrice !== 'number') continue
      quotes[q.symbol] = {
        price: q.regularMarketPrice,
        currency: q.currency ?? 'USD',
        asOf: now
      }
    }
    return quotes
  }

  async getRates(currencies: string[]): Promise<Record<string, number>> {
    const needed = [...new Set(currencies)].filter((c) => c && c !== 'USD')
    const rates: Record<string, number> = { USD: 1 }
    if (needed.length === 0) return rates

    // "<CUR>=X" quotes how many units of CUR one USD buys.
    const symbols = needed.map((c) => `${c}=X`)
    const results =
      symbols.length === 1 ? [await this.client.quote(symbols[0])] : await this.client.quote(symbols)

    for (const r of results) {
      // Keyed off the quote's own currency rather than the symbol, since Yahoo
      // normalizes e.g. "KRW=X" to "USDKRW=X" in the returned symbol.
      if (!r?.currency || typeof r.regularMarketPrice !== 'number') continue
      rates[r.currency] = r.regularMarketPrice
    }

    if (Object.keys(rates).length === 1) {
      throw new Error('No exchange rates available')
    }
    return rates
  }
}
