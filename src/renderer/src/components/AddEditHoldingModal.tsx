import { FormEvent, useState } from 'react'
import type { Currency, Holding, HoldingInput } from '../../../shared/types'

interface Props {
  mode: 'add' | 'edit'
  initial?: Holding
  onSubmit: (input: HoldingInput) => void
  onClose: () => void
}

function AddEditHoldingModal({ mode, initial, onSubmit, onClose }: Props): JSX.Element {
  const [ticker, setTicker] = useState(initial?.ticker ?? '')
  const [shares, setShares] = useState(initial ? String(initial.shares) : '')
  const [buyPrice, setBuyPrice] = useState(initial ? String(initial.buyPrice) : '')
  const [buyPriceCurrency, setBuyPriceCurrency] = useState<Currency>(
    initial?.buyPriceCurrency ?? 'USD'
  )
  const [buyDate, setBuyDate] = useState(initial?.buyDate ?? '')
  const [notes, setNotes] = useState(initial?.notes ?? '')
  const [error, setError] = useState<string | null>(null)

  function handleSubmit(e: FormEvent): void {
    e.preventDefault()

    const sharesNum = Number(shares)
    const buyPriceNum = Number(buyPrice)

    if (!ticker.trim()) {
      setError('Ticker is required.')
      return
    }
    if (!Number.isFinite(sharesNum) || sharesNum <= 0) {
      setError('Shares must be a positive number.')
      return
    }
    if (!Number.isFinite(buyPriceNum) || buyPriceNum <= 0) {
      setError('Buy price must be a positive number.')
      return
    }

    onSubmit({
      ticker: ticker.trim(),
      shares: sharesNum,
      buyPrice: buyPriceNum,
      buyPriceCurrency,
      buyDate: buyDate || undefined,
      notes: notes || undefined
    })
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{mode === 'add' ? 'Add Stock' : 'Edit Stock'}</h2>
        <form onSubmit={handleSubmit}>
          <label>
            Ticker
            <input
              value={ticker}
              onChange={(e) => setTicker(e.target.value)}
              placeholder="AAPL"
              autoFocus
            />
          </label>
          <label>
            Number of shares
            <input
              type="number"
              step="any"
              min="0"
              value={shares}
              onChange={(e) => setShares(e.target.value)}
              placeholder="10"
            />
          </label>
          <label>
            Buying price per share
            <div className="input-with-currency">
              <input
                type="number"
                step="any"
                min="0"
                value={buyPrice}
                onChange={(e) => setBuyPrice(e.target.value)}
                placeholder="150.00"
              />
              <div className="currency-toggle">
                <button
                  type="button"
                  className={buyPriceCurrency === 'USD' ? 'active' : ''}
                  onClick={() => setBuyPriceCurrency('USD')}
                  disabled={buyPriceCurrency === 'USD'}
                >
                  USD
                </button>
                <button
                  type="button"
                  className={buyPriceCurrency === 'KRW' ? 'active' : ''}
                  onClick={() => setBuyPriceCurrency('KRW')}
                  disabled={buyPriceCurrency === 'KRW'}
                >
                  KRW
                </button>
              </div>
            </div>
          </label>
          <label>
            Buy date (optional)
            <input type="date" value={buyDate} onChange={(e) => setBuyDate(e.target.value)} />
          </label>
          <label>
            Notes (optional)
            <input value={notes} onChange={(e) => setNotes(e.target.value)} />
          </label>

          {error && <p className="form-error">{error}</p>}

          <div className="modal-actions">
            <button type="button" onClick={onClose}>
              Cancel
            </button>
            <button type="submit">{mode === 'add' ? 'Add' : 'Save'}</button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default AddEditHoldingModal
