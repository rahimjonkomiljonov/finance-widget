import type { Holding, QuoteMap } from '../../../shared/types'

export interface DerivedHolding extends Holding {
  buyPriceUsd: number | null
  currentPriceUsd: number | null
  marketValueUsd: number | null
  costBasisUsd: number | null
  gainLossAbsUsd: number | null
  gainLossPct: number | null
  allocationPct: number | null
}

export interface PortfolioTotals {
  totalMarketValueUsd: number
  totalCostBasisUsd: number
  totalGainLossUsd: number
  totalGainLossPct: number | null
}

/**
 * Converts an amount to USD. Returns null when no rate is known for the currency,
 * so an unconvertible value shows as "—" rather than a silently wrong number.
 */
export function toUsd(
  amount: number,
  currency: string,
  rates: Record<string, number>
): number | null {
  if (currency === 'USD') return amount
  const rate = rates[currency]
  return rate ? amount / rate : null
}

export function deriveHoldings(
  holdings: Holding[],
  quotes: QuoteMap,
  rates: Record<string, number>
): DerivedHolding[] {
  const withValues = holdings.map((h) => {
    const quote = quotes[h.ticker]
    // Quotes are denominated in the listing's own currency (e.g. 005930.KS is KRW).
    const currentPriceUsd = quote ? toUsd(quote.price, quote.currency, rates) : null

    // buyPriceCurrency is absent on holdings created before this field existed —
    // those were always USD.
    const buyPriceUsd = toUsd(h.buyPrice, h.buyPriceCurrency ?? 'USD', rates)

    const marketValueUsd = currentPriceUsd !== null ? h.shares * currentPriceUsd : null
    const costBasisUsd = buyPriceUsd !== null ? h.shares * buyPriceUsd : null
    const gainLossAbsUsd =
      marketValueUsd !== null && costBasisUsd !== null ? marketValueUsd - costBasisUsd : null
    const gainLossPct =
      gainLossAbsUsd !== null && costBasisUsd !== null && costBasisUsd > 0
        ? (gainLossAbsUsd / costBasisUsd) * 100
        : null

    return {
      ...h,
      buyPriceUsd,
      currentPriceUsd,
      marketValueUsd,
      costBasisUsd,
      gainLossAbsUsd,
      gainLossPct,
      allocationPct: null as number | null
    }
  })

  const totalMarketValueUsd = withValues.reduce((sum, h) => sum + (h.marketValueUsd ?? 0), 0)

  return withValues.map((h) => ({
    ...h,
    allocationPct:
      totalMarketValueUsd > 0 && h.marketValueUsd !== null
        ? (h.marketValueUsd / totalMarketValueUsd) * 100
        : null
  }))
}

export function computePortfolioTotals(derived: DerivedHolding[]): PortfolioTotals {
  const totalMarketValueUsd = derived.reduce((sum, h) => sum + (h.marketValueUsd ?? 0), 0)
  const totalCostBasisUsd = derived.reduce((sum, h) => sum + (h.costBasisUsd ?? 0), 0)
  const totalGainLossUsd = totalMarketValueUsd - totalCostBasisUsd
  const totalGainLossPct = totalCostBasisUsd > 0 ? (totalGainLossUsd / totalCostBasisUsd) * 100 : null

  return { totalMarketValueUsd, totalCostBasisUsd, totalGainLossUsd, totalGainLossPct }
}
