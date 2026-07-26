# TaxiMeter — Code Audit

Audited against the working tree on 2026-07-26 (uncommitted v1.9 pass, versionCode 18).
Every claim below cites the file it was verified in.

## 1. Product purpose

An offline-first Android taxi meter for working drivers: GPS-driven fare metering with
per-country tariffs (36 countries, `app/src/main/java/com/myapp/taximeter/data/GlobalFareDatabase.kt`),
driver tariff overrides, local trip history, earnings dashboard, and CSV export.
Kotlin + Views (no Compose), single activity, `minSdk 24 / targetSdk 35`
(`app/build.gradle.kts`). Package and applicationId are `com.myapp.taximeter`.

## 2. What exists and works (evidence)

| Feature | Evidence |
|---|---|
| GPS metering: 2 m noise filter, waiting time below 5 km/h, 1–2 s updates | `MainActivity.kt` `handleNewLocation()`, `startLocationUpdates()` |
| Fare engine: base/km/min/waiting + night/weekend/airport surcharges, traffic/surge/vehicle multipliers, minimum-fare top-up, currency-aware rounding | `domain/FareEngine.kt` |
| 36-country tariff table with currency decimals | `data/GlobalFareDatabase.kt` (36 `FareConfig` entries) |
| Driver tariff override persisted in Room, reset-to-default | `data/FareRepository.kt`, `MainActivity.kt` `showRateEditorDialog()` |
| Trip persistence + history dialog with totals, clear-all confirm | `data/local/Daos.kt`, `MainActivity.kt` `showTripHistoryDialog()`, `ui/history/TripHistoryAdapter.kt` |
| Earnings dashboard (today/7d/30d + view-based bar chart) | `domain/EarningsCalculator.kt`, `MainActivity.kt` `showEarningsDialog()` |
| CSV export (RFC-4180 quoting, Locale.US numbers) | `domain/TripCsvBuilder.kt`, `MainActivity.kt` `exportTripHistoryCsv()` |
| Trip summary + share receipt, route-pin fare estimate (Haversine) | `MainActivity.kt` `showTripSummary()`, `drawRouteAndEstimate()` |
| Reset/back safeguards while metering, keep-screen-on toggle | `MainActivity.kt` `confirmResetRide()`, back callback, `applyKeepScreenOn()` |
| Full dark theme + dark map style | `res/values-night/`, `res/raw/map_style_dark.json`, `onMapReady()` |

## 3. Broken / incomplete areas

- **Waiting time is miscounted.** `handleNewLocation()` adds `1.0/60.0` minutes (1 s)
  per location callback, but updates are requested at a 2000 ms interval
  (`startLocationUpdates()`), so waiting time can undercount by up to 2x.
- **Active ride dies on rotation / process death.** `rideState`, `startTime`,
  `totalDistance`, `totalWaitingMinutes`, `pausedTime` are plain `MainActivity` fields;
  there is no `onSaveInstanceState`, no orientation lock in `AndroidManifest.xml`, and
  the ViewModel holds none of this. Rotating the phone mid-ride resets the meter to 0.
- **No foreground service.** Metering only runs while the activity is alive; Android
  will throttle or stop location updates when the app is backgrounded or the screen
  locks with the toggle off. A notification channel is created
  (`createNotificationChannel()`) but no notification is ever posted, and
  `POST_NOTIFICATIONS` is declared but never requested at runtime.
- **`totalFareUsd` is wrong for every non-USD trip.** `stopRide()` passes
  `usdRate = 1.0` ("Simple parity for now"), so the stored USD column equals the local
  fare (`ui/main/MainViewModel.kt` `saveCompletedTrip()`).
- **Short trips vanish silently.** `stopRide()` only saves when `totalDistance > 0.05`
  km — no summary, no toast, fare discarded without feedback.
- **Country picker exposes 20 of 36 tariffs.** The legacy `countryRates` list in
  `MainActivity.kt` (20 entries) gates the picker; `GlobalFareDatabase` has 36. Name→ISO
  mapping (`codeToIso()`) is a hardcoded `when` that falls back to `"MA"` for unknowns.
- **Vehicle types have no UI.** `MainViewModel.changeVehicleType()` has zero callers;
  ECONOMY is the only reachable type despite engine multipliers for 4 types.
- **Dead settings.** `SettingsRepository.themeMode`, `notificationsEnabled`,
  `showFxCurrencies`, `languageCode` are read/written by nothing in the UI.
- **Dead schema.** `UserProfileEntity`/`UserProfileDao` and
  `TripRepository.observeStats()` are unused; `TripEntity.weatherSummary` and
  `polylineEncoded` are always null.
- **Localization is scaffolding only.** `res/values-ar/` and `res/values-fr/` are empty
  directories; all 127 strings exist only in English (`res/values/strings.xml`).
  `FareEngine` bakes English component labels ("Base fare", …) into domain output.

## 4. Architecture weaknesses

- **`MainActivity.kt` is a 978-line god class** (~45 KB): GPS callbacks, map, 6 dialogs,
  CSV file I/O, a chart renderer, a RecyclerView adapter (`CountryRateAdapter`), and
  duplicate country/currency data that shadows `GlobalFareDatabase`. This is the single
  biggest debt item; see `docs/ROADMAP.md` P0.
- Ride state lives in the Activity, fare state in the ViewModel — two sources of truth
  glued together by a 1 s `Handler` loop (`startUIUpdateLoop()`).
- `MapView` lifecycle is only partially forwarded: `onResume/onPause/onDestroy/onLowMemory`
  but **not** `onStart/onStop/onSaveInstanceState`, which the Maps SDK requires.
- `Room.databaseBuilder(...).fallbackToDestructiveMigration()` (`TaxiMeterApp.kt`) will
  silently wipe all trips on the next schema version bump.
- Unused heavyweight dependencies: `firebase-auth`, `firebase-database`,
  `firebase-firestore-ktx`, Retrofit + Moshi, two ZXing artifacts, `easypermissions`
  (`app/build.gradle.kts`) — zero imports anywhere under `app/src/main/java/`.

## 5. UI/UX weaknesses

- Route "planning" is a straight dashed line + Haversine estimate, not road routing
  (`drawRouteAndEstimate()`); estimate ignores per-minute and minimum fare.
- Fare estimate and CSV-saved-path feedback are Toasts — transient, not reviewable.
- No trip detail view; history rows are read-only summaries (`TripHistoryAdapter.kt`).
- `String.format("%.2f", …)` without an explicit locale for the fare display
  (`observeViewModel()`, `updateUI()`) mixes device-locale digits with hardcoded units.
- Mixed-currency history shows a label instead of totals (`history_mixed_currencies`).

## 6. Performance risks

- 1 s UI loop runs forever, even when idle (`startUIUpdateLoop()` re-posts regardless
  of state; only the body is gated).
- `notifyDataSetChanged()` on every history emission (`TripHistoryAdapter.submitList`,
  `CountryRateAdapter.filter`) — no DiffUtil.
- Earnings and CSV load **all** trips into memory (`TripDao.getAllTrips()`); fine now,
  unbounded as history grows (no pagination, no SQL aggregation for earnings).
- ~6 unused libraries inflate APK size and R8 work (see §4).

## 7. Security risks (details in docs/SECURITY.md)

- Google Maps API key committed in plaintext: `res/values/strings.xml` line 3.
- `google-services.json` committed (`app/src/google-services.json`, Firebase project
  `taxi-dabd8`) with API keys and a stale `com.example.taximeter` client entry.
- `android:allowBackup="true"` with no `fullBackupContent`/`dataExtractionRules` wired
  in the manifest — the trip DB and prefs are cloud-backup eligible by default; the
  rules files in `res/xml/` are commented-out templates and are not referenced.
- CI deploys to the Play **production** track on every push to `master` with no test or
  lint gate (`.github/workflows/playstore-release.yml`).
- A signed release artifact is committed to the repo (`app/release/app-release.aab`).

## 8. Accessibility issues (details in docs/ACCESSIBILITY.md)

- Strong base: 48dp targets, 64dp primary buttons (`res/values/dimens.xml`), 17 `cd_*`
  content-description strings, decorative views excluded.
- Gaps: earnings bars are `IMPORTANT_FOR_ACCESSIBILITY_NO` with no text alternative;
  no `accessibilityLiveRegion` on fare/status; RTL and font-scale behavior untested;
  no translations despite `supportsRtl="true"`.

## 9. Prioritized recommendations

**P0 (correctness/money):** fix waiting-time accrual; move ride state into the
ViewModel + `SavedStateHandle`; add a foreground service for metering; stop writing
fabricated `totalFareUsd`; unit-test `FareEngine` and `EarningsCalculator` (coverage is
currently zero — see `docs/TESTING.md`).

**P1 (debt/trust):** decompose `MainActivity`; expose all 36 tariffs and delete the
duplicate 20-country list; remove unused Firebase/Retrofit/ZXing/easypermissions deps;
replace `fallbackToDestructiveMigration` with real migrations; restrict + rotate the
Maps key; gate CI deploys behind tests; remove the committed `.aab`.

**P2 (product):** vehicle-type UI, airport toggle, real FX rates, ar/fr translations,
trip detail screen, DiffUtil adapters. Full list: `docs/ROADMAP.md`.
