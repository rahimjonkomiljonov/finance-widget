# Finance Widget

A stock-portfolio widget for Windows and Android. Add holdings (ticker, shares, buy price in USD or KRW), see allocation and gain/loss, and switch the display between USD and KRW. Prices come from Yahoo Finance's unofficial API, so they can be delayed or occasionally unavailable. Nothing here is financial advice.

## Desktop (Windows) — Electron + React

- Donut allocation chart, per-holding gain/loss, USD/KRW toggle
- Always-on-top widget mode and full-window mode, tray icon, start at login
- Edit/remove holdings, CSV export, auto price refresh

```
npm install
npm run dev          # run in development
npm run dist:win     # build the installer and portable exe into release/
```

## Android — Kotlin + Jetpack Compose

- Home-screen widget listing each holding with the price and the change since the previous market close
- Tap the widget to open the app: totals, allocation donut, holdings, add/edit, CSV export
- Background refresh via WorkManager (15 min minimum), Korean (red up) or Western (green up) colours

```
cd android
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

Requires JDK 21 and the Android SDK (platform 37). The release build is signed with the debug keystore, so it is suitable for sideloading, not for the Play Store.

## Data

Holdings are stored locally on each device. There is no account and no server. Only ticker symbols are sent to Yahoo Finance to fetch prices.

## License

MIT. See [LICENSE](LICENSE).
