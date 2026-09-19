import { dialog, type BrowserWindow } from 'electron'
import { writeFile } from 'fs/promises'
import Papa from 'papaparse'

export interface CsvExportResult {
  canceled: boolean
  filePath?: string
}

export async function exportHoldingsToCsv(
  rows: Record<string, string | number>[],
  window: BrowserWindow | null
): Promise<CsvExportResult> {
  const options = {
    title: 'Export Portfolio to CSV',
    defaultPath: 'portfolio.csv',
    filters: [{ name: 'CSV', extensions: ['csv'] }]
  }
  const { canceled, filePath } = window
    ? await dialog.showSaveDialog(window, options)
    : await dialog.showSaveDialog(options)

  if (canceled || !filePath) return { canceled: true }

  const csv = Papa.unparse(rows)
  await writeFile(filePath, csv, 'utf-8')
  return { canceled: false, filePath }
}
