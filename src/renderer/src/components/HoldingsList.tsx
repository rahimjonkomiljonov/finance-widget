import type { Currency } from '../../../shared/types'
import type { DerivedHolding } from '../lib/derive'
import HoldingRow from './HoldingRow'

interface Props {
  holdings: DerivedHolding[]
  currency: Currency
  fxRate: number | null
  compact?: boolean
  onEdit: (holding: DerivedHolding) => void
  onDelete: (id: string) => void
}

function HoldingsList({
  holdings,
  currency,
  fxRate,
  compact = false,
  onEdit,
  onDelete
}: Props): JSX.Element {
  if (holdings.length === 0) {
    return <p className="empty-state">No holdings yet. Add a stock to get started.</p>
  }

  return (
    <div className="holdings-table-wrapper">
      <table className="holdings-table">
        <thead>
          <tr>
            <th>Ticker</th>
            {!compact && <th>Shares</th>}
            {!compact && <th>Buy price</th>}
            {!compact && <th>Current price</th>}
            <th>Value</th>
            <th>Gain/Loss</th>
            {!compact && <th>Allocation</th>}
            <th></th>
          </tr>
        </thead>
        <tbody>
          {holdings.map((h) => (
            <HoldingRow
              key={h.id}
              holding={h}
              currency={currency}
              fxRate={fxRate}
              compact={compact}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default HoldingsList
