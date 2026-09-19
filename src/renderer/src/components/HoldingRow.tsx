import type { Currency } from '../../../shared/types'
import type { DerivedHolding } from '../lib/derive'
import { formatCurrency, formatPercent } from '../lib/formatCurrency'

interface Props {
  holding: DerivedHolding
  currency: Currency
  fxRate: number | null
  compact?: boolean
  onEdit: (holding: DerivedHolding) => void
  onDelete: (id: string) => void
}

function HoldingRow({
  holding,
  currency,
  fxRate,
  compact = false,
  onEdit,
  onDelete
}: Props): JSX.Element {
  const gainLossClass =
    holding.gainLossAbsUsd === null ? '' : holding.gainLossAbsUsd >= 0 ? 'gain' : 'loss'

  return (
    <tr>
      <td>{holding.ticker}</td>
      {!compact && <td>{holding.shares}</td>}
      {!compact && (
        <td>
          {holding.buyPriceUsd !== null ? formatCurrency(holding.buyPriceUsd, currency, fxRate) : '—'}
        </td>
      )}
      {!compact && (
        <td>
          {holding.currentPriceUsd !== null
            ? formatCurrency(holding.currentPriceUsd, currency, fxRate)
            : '—'}
        </td>
      )}
      <td>
        {holding.marketValueUsd !== null ? formatCurrency(holding.marketValueUsd, currency, fxRate) : '—'}
      </td>
      <td className={gainLossClass}>
        {holding.gainLossAbsUsd === null
          ? '—'
          : compact
            ? formatPercent(holding.gainLossPct)
            : `${formatCurrency(holding.gainLossAbsUsd, currency, fxRate)} (${formatPercent(holding.gainLossPct)})`}
      </td>
      {!compact && (
        <td>{holding.allocationPct !== null ? `${holding.allocationPct.toFixed(1)}%` : '—'}</td>
      )}
      <td className="row-actions">
        <button onClick={() => onEdit(holding)} aria-label={`Edit ${holding.ticker}`}>
          {compact ? '✎' : 'Edit'}
        </button>
        <button onClick={() => onDelete(holding.id)} aria-label={`Delete ${holding.ticker}`}>
          {compact ? '✕' : 'Delete'}
        </button>
      </td>
    </tr>
  )
}

export default HoldingRow
