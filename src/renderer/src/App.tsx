import { useEffect, useMemo, useState } from 'react'
import type { Currency, FxRates, Holding, HoldingInput, QuoteMap, WindowMode } from '../../shared/types'
import HoldingsList from './components/HoldingsList'
import AddEditHoldingModal from './components/AddEditHoldingModal'
import AllocationPieChart from './components/AllocationPieChart'
import CurrencyToggle from './components/CurrencyToggle'
import SettingsPanel from './components/SettingsPanel'
import WindowModeToggle from './components/WindowModeToggle'
import { computePortfolioTotals, deriveHoldings } from './lib/derive'
import { formatCurrency, formatPercent } from './lib/formatCurrency'

function getInitialWindowMode(): WindowMode {
  const params = new URLSearchParams(window.location.search)
  return params.get('mode') === 'full' ? 'full' : 'widget'
}

function App(): JSX.Element {
  const [windowMode] = useState<WindowMode>(getInitialWindowMode)
  const [holdings, setHoldings] = useState<Holding[]>([])
  const [quotes, setQuotes] = useState<QuoteMap>({})
  const [fx, setFx] = useState<FxRates | null>(null)
  const [currency, setCurrencyState] = useState<Currency>('USD')
  const [refreshIntervalMs, setRefreshIntervalMs] = useState(5 * 60_000)
  const [startAtLogin, setStartAtLoginState] = useState(true)
  const [widgetOnTop, setWidgetOnTopState] = useState(false)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [modalState, setModalState] = useState<
    { mode: 'add' } | { mode: 'edit'; holding: Holding } | null
  >(null)

  useEffect(() => {
    window.api.getState().then((state) => {
      setHoldings(state.holdings)
      setQuotes(state.quotes)
      setFx(state.fx)
      setCurrencyState(state.settings.displayCurrency)
      setRefreshIntervalMs(state.settings.refreshIntervalMs)
      setStartAtLoginState(state.settings.startAtLogin ?? true)
      setWidgetOnTopState(state.settings.widgetOnTop === true)
      setLoading(false)
    })

    const unsubscribe = window.api.onQuotesUpdated((payload) => {
      setQuotes(payload.quotes)
      setFx(payload.fx)
    })
    return unsubscribe
  }, [])

  const rates = useMemo(() => fx?.rates ?? {}, [fx])
  const fxRate = rates.KRW ?? null
  const derived = useMemo(() => deriveHoldings(holdings, quotes, rates), [holdings, quotes, rates])
  const totals = useMemo(() => computePortfolioTotals(derived), [derived])

  async function handleCurrencyChange(next: Currency): Promise<void> {
    setCurrencyState(next)
    await window.api.setCurrency(next)
  }

  async function handleSetRefreshInterval(ms: number): Promise<void> {
    setRefreshIntervalMs(ms)
    await window.api.setRefreshInterval(ms)
  }

  async function handleSetStartAtLogin(enabled: boolean): Promise<void> {
    setStartAtLoginState(enabled)
    await window.api.setStartAtLogin(enabled)
  }

  async function handleSetWidgetOnTop(enabled: boolean): Promise<void> {
    setWidgetOnTopState(enabled)
    await window.api.setWidgetOnTop(enabled)
  }

  // The tray menu can flip this too, so re-read it whenever the panel opens.
  function openSettings(): void {
    window.api.getState().then((state) => setWidgetOnTopState(state.settings.widgetOnTop === true))
    setSettingsOpen(true)
  }

  async function handleRefresh(): Promise<void> {
    setRefreshing(true)
    try {
      const payload = await window.api.refreshNow()
      setQuotes(payload.quotes)
      setFx(payload.fx)
    } finally {
      setRefreshing(false)
    }
  }

  async function handleAdd(input: HoldingInput): Promise<void> {
    const holding = await window.api.addHolding(input)
    setHoldings((prev) => [...prev, holding])
    setModalState(null)
  }

  async function handleUpdate(id: string, input: HoldingInput): Promise<void> {
    const updated = await window.api.updateHolding(id, input)
    if (updated) {
      setHoldings((prev) => prev.map((h) => (h.id === id ? updated : h)))
    }
    setModalState(null)
  }

  async function handleDelete(id: string): Promise<void> {
    await window.api.removeHolding(id)
    setHoldings((prev) => prev.filter((h) => h.id !== id))
  }

  if (loading) {
    return <div className="app-shell">Loading...</div>
  }

  const gainLossClass = totals.totalGainLossUsd >= 0 ? 'gain' : 'loss'

  return (
    <div className={`app-shell app-shell--${windowMode}`}>
      <header className="app-header">
        <h1>Finance Widget</h1>
        <div className="header-actions">
          <CurrencyToggle currency={currency} onChange={handleCurrencyChange} />
          <button onClick={handleRefresh} disabled={refreshing} aria-label="Refresh" title="Refresh">
            {windowMode === 'widget' ? '↻' : refreshing ? 'Refreshing...' : 'Refresh'}
          </button>
          <button onClick={() => setModalState({ mode: 'add' })} aria-label="Add stock" title="Add stock">
            {windowMode === 'widget' ? '+' : '+ Add Stock'}
          </button>
          <button onClick={openSettings} aria-label="Settings" title="Settings">
            ⚙
          </button>
          <WindowModeToggle mode={windowMode} onChange={(m) => window.api.setWindowMode(m)} />
        </div>
      </header>

      {fxRate && <p className="fx-line">1 USD = {fxRate.toFixed(2)} KRW</p>}

      {holdings.length > 0 && (
        <div className="totals-bar">
          <div>
            <div className="stat-label">Total value</div>
            <div className="stat-value">
              {formatCurrency(totals.totalMarketValueUsd, currency, fxRate)}
            </div>
          </div>
          <div>
            <div className="stat-label">Total gain/loss</div>
            <div className={`stat-value ${gainLossClass}`}>
              {formatCurrency(totals.totalGainLossUsd, currency, fxRate)} (
              {formatPercent(totals.totalGainLossPct)})
            </div>
          </div>
        </div>
      )}

      <AllocationPieChart
        holdings={derived}
        currency={currency}
        fxRate={fxRate}
        height={windowMode === 'widget' ? 160 : 220}
      />

      <HoldingsList
        holdings={derived}
        currency={currency}
        fxRate={fxRate}
        compact={windowMode === 'widget'}
        onEdit={(holding) => setModalState({ mode: 'edit', holding })}
        onDelete={handleDelete}
      />

      {modalState?.mode === 'add' && (
        <AddEditHoldingModal mode="add" onSubmit={handleAdd} onClose={() => setModalState(null)} />
      )}
      {modalState?.mode === 'edit' && (
        <AddEditHoldingModal
          mode="edit"
          initial={modalState.holding}
          onSubmit={(input) => handleUpdate(modalState.holding.id, input)}
          onClose={() => setModalState(null)}
        />
      )}

      {settingsOpen && (
        <SettingsPanel
          holdings={derived}
          refreshIntervalMs={refreshIntervalMs}
          startAtLogin={startAtLogin}
          widgetOnTop={widgetOnTop}
          showWidgetOnTop={windowMode === 'widget'}
          onSetRefreshInterval={handleSetRefreshInterval}
          onSetStartAtLogin={handleSetStartAtLogin}
          onSetWidgetOnTop={handleSetWidgetOnTop}
          onClose={() => setSettingsOpen(false)}
        />
      )}
    </div>
  )
}

export default App
