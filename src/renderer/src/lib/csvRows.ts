import type { DerivedHolding } from './derive'

export function buildCsvRows(holdings: DerivedHolding[]): Record<string, string | number>[] {
  return holdings.map((h) => ({
    Ticker: h.ticker,
    Shares: h.shares,
    'Buy Price (original)': h.buyPrice,
    'Buy Price Currency': h.buyPriceCurrency ?? 'USD',
    'Buy Price (USD)': h.buyPriceUsd ?? '',
    'Current Price (USD)': h.currentPriceUsd ?? '',
    'Market Value (USD)': h.marketValueUsd ?? '',
    'Cost Basis (USD)': h.costBasisUsd ?? '',
    'Gain/Loss (USD)': h.gainLossAbsUsd ?? '',
    'Gain/Loss (%)': h.gainLossPct !== null ? h.gainLossPct.toFixed(2) : '',
    'Allocation (%)': h.allocationPct !== null ? h.allocationPct.toFixed(2) : '',
    'Buy Date': h.buyDate ?? '',
    Notes: h.notes ?? ''
  }))
}
