import type { PriceProvider } from './priceProvider'
import { getQuoteCache, setQuoteCache, getFxRatesCache, setFxRatesCache } from '../store'
import type { QuotesUpdatePayload } from '../../shared/types'

export const MIN_REFRESH_INTERVAL_MS = 60_000

export type RefreshPayload = QuotesUpdatePayload

export class RefreshScheduler {
  private timer: ReturnType<typeof setInterval> | null = null

  constructor(
    private provider: PriceProvider,
    private intervalMs: number,
    private getTickers: () => string[],
    private onUpdate: (payload: RefreshPayload) => void
  ) {
    this.intervalMs = Math.max(MIN_REFRESH_INTERVAL_MS, intervalMs)
  }

  start(): void {
    this.restartTimer()
    void this.doFetch()
  }

  stop(): void {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  }

  setInterval(ms: number): void {
    this.intervalMs = Math.max(MIN_REFRESH_INTERVAL_MS, ms)
    this.restartTimer()
  }

  async refreshNow(): Promise<RefreshPayload> {
    const result = await this.doFetch()
    this.restartTimer()
    return result
  }

  private restartTimer(): void {
    if (this.timer) clearInterval(this.timer)
    this.timer = setInterval(() => void this.doFetch(), this.intervalMs)
  }

  private async doFetch(): Promise<RefreshPayload> {
    const tickers = this.getTickers()

    let quotes = getQuoteCache()
    try {
      const fresh = await this.provider.getQuotes(tickers)
      quotes = { ...quotes, ...fresh }
      setQuoteCache(quotes)
    } catch (err) {
      console.error('Failed to fetch quotes, falling back to cache:', err)
    }

    // Quotes come back in each listing's own currency, so a rate is needed for
    // every currency present. KRW is always included for the display-currency toggle.
    const currencies = new Set<string>(['KRW'])
    for (const ticker of tickers) {
      const currency = quotes[ticker]?.currency
      if (currency) currencies.add(currency)
    }

    let fx = getFxRatesCache()
    try {
      const fresh = await this.provider.getRates([...currencies])
      // Merge so a partial failure never drops a rate we already had.
      fx = { rates: { ...fx?.rates, ...fresh }, asOf: new Date().toISOString() }
      setFxRatesCache(fx)
    } catch (err) {
      console.error('Failed to fetch exchange rates, falling back to cache:', err)
    }

    const payload: RefreshPayload = { quotes, fx: fx ?? { rates: {}, asOf: '' } }
    this.onUpdate(payload)
    return payload
  }
}
