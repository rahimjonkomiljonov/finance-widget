import type { Currency } from '../../../shared/types'

interface Props {
  currency: Currency
  onChange: (currency: Currency) => void
}

function CurrencyToggle({ currency, onChange }: Props): JSX.Element {
  return (
    <div className="currency-toggle">
      <button
        className={currency === 'USD' ? 'active' : ''}
        onClick={() => onChange('USD')}
        disabled={currency === 'USD'}
      >
        USD
      </button>
      <button
        className={currency === 'KRW' ? 'active' : ''}
        onClick={() => onChange('KRW')}
        disabled={currency === 'KRW'}
      >
        KRW
      </button>
    </div>
  )
}

export default CurrencyToggle
