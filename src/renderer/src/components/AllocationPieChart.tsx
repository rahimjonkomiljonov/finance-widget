import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, TooltipProps } from 'recharts'
import type { Currency } from '../../../shared/types'
import { valueByTicker, type DerivedHolding } from '../lib/derive'
import { formatCurrency } from '../lib/formatCurrency'

// Dark-mode categorical palette (validated for CVD-safe adjacency, fixed order — never cycled).
// Pie slices can be compared non-adjacently, so beyond these 8 slots remaining
// holdings fold into a neutral "Other" bucket rather than reusing/generating hues.
const CATEGORICAL_DARK = [
  '#3987e5', // blue
  '#d95926', // orange
  '#199e70', // aqua
  '#c98500', // yellow
  '#d55181', // magenta
  '#008300', // green
  '#9085e9', // violet
  '#e66767' // red
]
const OTHER_COLOR = '#898781'
const MAX_SLICES = CATEGORICAL_DARK.length

interface SliceDatum {
  name: string
  value: number
}

interface Props {
  holdings: DerivedHolding[]
  currency: Currency
  fxRate: number | null
  height?: number
}

function buildSlices(holdings: DerivedHolding[]): SliceDatum[] {
  // One slice per stock: several lots of the same ticker are combined.
  const sorted = valueByTicker(holdings)
    .filter((t) => t.valueUsd > 0)
    .sort((a, b) => b.valueUsd - a.valueUsd)

  const top = sorted.slice(0, MAX_SLICES)
  const rest = sorted.slice(MAX_SLICES)
  const otherValue = rest.reduce((sum, t) => sum + t.valueUsd, 0)

  const slices: SliceDatum[] = top.map((t) => ({ name: t.ticker, value: t.valueUsd }))
  if (otherValue > 0) slices.push({ name: 'Other', value: otherValue })
  return slices
}

function colorForIndex(name: string, index: number): string {
  return name === 'Other' ? OTHER_COLOR : CATEGORICAL_DARK[index % CATEGORICAL_DARK.length]
}

function CustomTooltip({
  active,
  payload,
  currency,
  fxRate,
  total
}: TooltipProps<number, string> & { currency: Currency; fxRate: number | null; total: number }): JSX.Element | null {
  if (!active || !payload?.length) return null
  const { name, value } = payload[0] as unknown as SliceDatum
  const pct = total > 0 ? (value / total) * 100 : 0

  return (
    <div className="chart-tooltip">
      <strong>{name}</strong>
      <div>{formatCurrency(value, currency, fxRate)}</div>
      <div className="chart-tooltip-pct">{pct.toFixed(1)}% of portfolio</div>
    </div>
  )
}

function AllocationPieChart({ holdings, currency, fxRate, height = 220 }: Props): JSX.Element | null {
  const data = buildSlices(holdings)
  if (data.length === 0) return null

  const total = data.reduce((sum, d) => sum + d.value, 0)
  const outerRadius = Math.min(85, height / 2 - 30)
  const innerRadius = Math.max(20, outerRadius - 35)

  return (
    <ResponsiveContainer width="100%" height={height}>
      <PieChart>
        <Pie
          data={data}
          dataKey="value"
          nameKey="name"
          innerRadius={innerRadius}
          outerRadius={outerRadius}
          paddingAngle={2}
        >
          {data.map((entry, i) => (
            <Cell key={entry.name} fill={colorForIndex(entry.name, i)} stroke="none" />
          ))}
        </Pie>
        <Tooltip content={<CustomTooltip currency={currency} fxRate={fxRate} total={total} />} />
        <Legend
          verticalAlign="bottom"
          height={36}
          formatter={(value: string) => <span className="chart-legend-label">{value}</span>}
        />
      </PieChart>
    </ResponsiveContainer>
  )
}

export default AllocationPieChart
