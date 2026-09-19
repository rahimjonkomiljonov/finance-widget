import { FormEvent, useState } from 'react'
import type { DerivedHolding } from '../lib/derive'
import { buildCsvRows } from '../lib/csvRows'

interface Props {
  holdings: DerivedHolding[]
  refreshIntervalMs: number
  startAtLogin: boolean
  onSetRefreshInterval: (ms: number) => void
  onSetStartAtLogin: (enabled: boolean) => void
  onClose: () => void
}

function SettingsPanel({
  holdings,
  refreshIntervalMs,
  startAtLogin,
  onSetRefreshInterval,
  onSetStartAtLogin,
  onClose
}: Props): JSX.Element {
  const [minutes, setMinutes] = useState(String(Math.round(refreshIntervalMs / 60_000)))
  const [exportStatus, setExportStatus] = useState<string | null>(null)

  function handleIntervalSubmit(e: FormEvent): void {
    e.preventDefault()
    const mins = Math.max(1, Number(minutes) || 1)
    onSetRefreshInterval(mins * 60_000)
  }

  async function handleExport(): Promise<void> {
    setExportStatus(null)
    const rows = buildCsvRows(holdings)
    const result = await window.api.exportCsv(rows)
    if (result.canceled) {
      setExportStatus(null)
    } else {
      setExportStatus(`Saved to ${result.filePath}`)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Settings</h2>

        <form onSubmit={handleIntervalSubmit}>
          <label>
            Auto-refresh interval (minutes)
            <input
              type="number"
              min="1"
              step="1"
              value={minutes}
              onChange={(e) => setMinutes(e.target.value)}
            />
          </label>
          <div className="modal-actions">
            <button type="submit">Save interval</button>
          </div>
        </form>

        <hr className="modal-divider" />

        <label className="checkbox-row">
          <input
            type="checkbox"
            checked={startAtLogin}
            onChange={(e) => onSetStartAtLogin(e.target.checked)}
          />
          Start automatically when Windows starts
        </label>

        <hr className="modal-divider" />

        <div>
          <button onClick={handleExport} disabled={holdings.length === 0}>
            Export to CSV
          </button>
          {exportStatus && <p className="export-status">{exportStatus}</p>}
        </div>

        <div className="modal-actions">
          <button type="button" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  )
}

export default SettingsPanel
