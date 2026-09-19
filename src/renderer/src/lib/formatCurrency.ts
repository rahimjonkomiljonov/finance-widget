import type { Currency } from '../../../shared/types'

export function formatCurrency(usdValue: number, currency: Currency, fxRate: number | null): string {
  if (currency === 'USD') {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(usdValue)
  }

  const krwValue = usdValue * (fxRate ?? 0)
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0
  }).format(krwValue)
}

export function formatPercent(pct: number | null): string {
  if (pct === null) return '—'
  const sign = pct > 0 ? '+' : ''
  return `${sign}${pct.toFixed(2)}%`
}
