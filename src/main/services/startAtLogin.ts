import { app } from 'electron'
import { getSettings, updateSettings } from '../store'

/**
 * Registers/unregisters the app with the OS login items so the widget is already
 * on screen after a reboot. Defaults to enabled — a desktop widget the user has to
 * launch by hand isn't much of a widget — and stays whatever they last chose.
 */
export function applyStartAtLogin(enabled?: boolean): boolean {
  const desired = enabled ?? getSettings().startAtLogin ?? true

  // No-op in dev: registering the bare electron.exe would launch an empty shell at login.
  if (app.isPackaged) {
    // Electron writes the Run-key value verbatim and doesn't quote it. The install path
    // contains spaces ("C:\Program Files\Finance Widget\..."), and an unquoted value makes
    // Windows try to launch "C:\Program" at login and silently fail — so quote it here.
    app.setLoginItemSettings({ openAtLogin: desired, path: `"${app.getPath('exe')}"` })
  }

  updateSettings({ startAtLogin: desired })
  return desired
}
