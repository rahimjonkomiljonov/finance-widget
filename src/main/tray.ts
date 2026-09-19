import { app, Menu, Tray } from 'electron'
import { createFallbackIcon } from './services/icon'
import { getCurrentWindow, setQuitting, switchWindowMode } from './windowManager'

let tray: Tray | null = null

export function createTray(): Tray {
  tray = new Tray(createFallbackIcon(16))
  tray.setToolTip('Finance Widget')

  const menu = Menu.buildFromTemplate([
    { label: 'Show Widget', click: () => switchWindowMode('widget') },
    { label: 'Show Full Window', click: () => switchWindowMode('full') },
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

  tray.on('click', () => {
    const win = getCurrentWindow()
    if (!win) return
    if (win.isVisible()) {
      win.hide()
    } else {
      win.show()
    }
  })

  return tray
}
