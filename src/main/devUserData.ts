import { app } from 'electron'
import { join } from 'path'

// Dev and test runs must never read or write the installed app's real portfolio.
// FINANCE_WIDGET_USER_DATA lets tests of a packaged build use a throwaway folder too.
// Must run before anything touches userData (electron-store, the single-instance lock).
const override = process.env['FINANCE_WIDGET_USER_DATA']
if (override) {
  app.setPath('userData', override)
} else if (!app.isPackaged) {
  app.setPath('userData', join(app.getPath('appData'), 'finance-widget-dev'))
}
