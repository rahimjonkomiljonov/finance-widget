import { app, Menu, Tray } from 'electron'
import { createFallbackIcon } from './services/icon'
import { getSettings } from './store'
import { getCurrentWindow, setQuitting, setWidgetOnTop, switchWindowMode } from './windowManager'

let tray: Tray | null = null

function bringToFront(): void {
  const win = getCurrentWindow()
  if (!win) return
  win.show()
  win.focus()
}

/** Rebuild the context menu so the "on top" checkbox matches the stored setting. */
export function refreshTrayMenu(): void {
  if (!tray) return
  const menu = Menu.buildFromTemplate([
    { label: 'Show Widget', click: () => switchWindowMode('widget') },
    { label: 'Show Full Window', click: () => switchWindowMode('full') },
    { label: 'Hide', click: () => getCurrentWindow()?.hide() },
    { type: 'separator' },
    {
      label: 'Keep widget on top of other windows',
      type: 'checkbox',
      checked: getSettings().widgetOnTop === true,
      click: (item) => {
        setWidgetOnTop(item.checked)
        refreshTrayMenu()
      }
    },
    { type: 'separator' },
    {
      label: 'Quit',
      click: () => {
        setQuitting(true)
        app.quit()
      }
    }
  ])
  tray.setContextMenu(menu)
}

export function createTray(): Tray {
  tray = new Tray(createFallbackIcon(16))
  tray.setToolTip('Finance Widget')
  refreshTrayMenu()

  // Without always-on-top the widget can sit behind other windows, so a tray click
  // brings it forward instead of hiding it. "Hide" in the menu does that.
  tray.on('click', bringToFront)

  return tray
}
