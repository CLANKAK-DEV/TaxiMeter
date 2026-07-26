# TaxiMeter — Implemented Features (with evidence)

Inventory of what the app actually does today, with the files that implement each
behavior. Paths are relative to the repo root; all under
`app/src/main/java/com/myapp/taximeter/` unless noted.

## Metering

- **GPS distance tracking** — Fused Location Provider, `PRIORITY_HIGH_ACCURACY`,
  2000 ms interval / 1000 ms minimum; per-fix delta with a 2 m noise floor.
  `MainActivity.kt` (`startLocationUpdates`, `handleNewLocation`).
- **Speed + waiting-time detection** — speed from `Location.speed` (m/s → km/h);
  waiting time accrues while speed < 5 km/h during an active ride.
  `MainActivity.kt` (`handleNewLocation`). *Known defect: adds a fixed 1 s per
  callback regardless of the real 1–2 s update interval.*
- **Ride states** — AVAILABLE / ACTIVE / PAUSED enum with pause-time subtraction and
  color/label/status-dot updates. `MainActivity.kt` (`RideState`, `handleStartStop`,
  `handlePauseResume`, `updateStatusDisplay`).
- **Live fare recalculation** — 1 s UI tick rebuilds `TripMetrics` and recomputes the
  fare through the ViewModel. `MainActivity.kt` (`startUIUpdateLoop`, `updateUI`),
  `ui/main/MainViewModel.kt` (`recalculateFare`).

## Fare computation

- **Pure fare engine** — base + per-km + per-minute + waiting, night (22:00–06:00)
  and weekend percentage surcharges, flat airport surcharge, traffic and surge
  multipliers, vehicle-type multipliers (ECONOMY 1.0 / COMFORT 1.25 / PREMIUM 1.6 /
  MOTO 0.8), minimum-fare top-up computed pre-clamp, rounding to the currency's
  decimal count. `domain/FareEngine.kt`.
- **Night/weekend detection** — device clock, hour ≥ 22 or < 6; Sat/Sun.
  `MainActivity.kt` (`isNightTime`, `isWeekend`). Traffic multiplier 1.2 auto-applied
  below 10 km/h (`updateUI`). Airport flag and surge are wired but constant
  (`false` / `1.0`) — no UI. Vehicle type is always ECONOMY — no UI selector.
- **Per-country tariffs** — 36 `FareConfig` entries (country, cities, currency with
  decimals, all rate fields). `data/GlobalFareDatabase.kt`.
- **Driver tariff overrides** — per country/city/vehicle key, nullable field-level
  override merged over the country default; save / reset-to-default.
  `data/FareRepository.kt`, `domain/FareEngine.kt` (`resolveEffectiveConfig`),
  `MainActivity.kt` (`showRateEditorDialog`, `parseRateInput` — accepts comma
  decimals, rejects negatives), `res/layout/dialog_settings.xml`.

## Country / currency selection

- **Searchable country picker** — 20-entry legacy list with flag, currency code, and
  $/km rate; selection persists via `SettingsRepository` and reloads the fare profile.
  `MainActivity.kt` (`showCountryPickerDialog`, `CountryRateAdapter`, `countryRates`),
  `res/layout/dialog_country_picker.xml`, `res/layout/item_currency.xml`,
  `ui/main/MainViewModel.kt` (`updateLocationContext`). *Only 20 of the 36 tariff
  configs are reachable from this picker.*

## Trips & history

- **Trip persistence** — completed rides (> 0.05 km) saved to Room `trips` table with
  times, distance, duration, waiting, fare, currency, country/city, vehicle type.
  `ui/main/MainViewModel.kt` (`saveCompletedTrip`), `data/local/Entities.kt`,
  `data/local/Daos.kt` (`TripDao`), `data/TripRepository.kt`.
- **Trip history dialog** — 20 most recent trips, totals strip (count / km /
  earnings, mixed-currency guard), empty state, confirmed Clear All.
  `MainActivity.kt` (`showTripHistoryDialog`), `ui/history/TripHistoryAdapter.kt`,
  `res/layout/dialog_trip_history.xml`, `res/layout/item_trip_history.xml`.
- **Trip summary card** — fare, distance, duration, average speed after each ride.
  `MainActivity.kt` (`showTripSummary`, `addSummaryRow`), `res/layout/summary_row.xml`.
- **Share receipt** — text receipt via `ACTION_SEND` chooser from the
  `share_trip_template` string. `MainActivity.kt` (`shareLastTrip`),
  `res/values/strings.xml`.
- **CSV export** — full history serialized deterministically (RFC 4180 quoting,
  Locale.US numbers, header constant); copy written to
  `getExternalFilesDir(null)/exports/` and content shared as `text/csv`.
  `domain/TripCsvBuilder.kt`, `MainActivity.kt` (`exportTripHistoryCsv`),
  `ui/main/MainViewModel.kt` (`buildTripHistoryCsv`).

## Earnings

- **Dashboard** — today / last-7-days / last-30-days trips, km, earnings; pure
  bucketing relative to local start-of-day; mixed-currency detection.
  `domain/EarningsCalculator.kt`, `MainActivity.kt` (`showEarningsDialog`,
  `bindEarnings`), `res/layout/dialog_earnings.xml`.
- **7-day bar chart** — plain-View bars scaled to the max day, dimmed stubs for zero
  days, weekday labels. `MainActivity.kt` (`buildEarningsBars`),
  `res/drawable/bar_earnings.xml`, `res/values/dimens.xml` (chart dimens).

## Map & route

- **Google Map with location layer** — my-location FAB, camera follow while active.
  `MainActivity.kt` (`onMapReady`, `centerOnMyLocation`, `updateMapCamera`).
- **Pickup/destination pins + estimate** — tap-mode FABs, dashed polyline, Haversine
  distance, base+per-km estimate shown as a Toast. `MainActivity.kt`
  (`enterMapTapMode`, `handleMapTap`, `drawRouteAndEstimate`, `haversineKm`).
  *Straight-line only; not road routing.*
- **Dark map style** — `res/raw/map_style_dark.json` applied in night mode
  (`onMapReady`); `map_style.json` / `map_style_light.json` also present.

## Safeguards & settings

- **Reset confirmation while metering**; single-tap reset when idle.
  `MainActivity.kt` (`confirmResetRide`).
- **Back-press guard during a ride** — confirm dialog via `OnBackPressedCallback`.
  `MainActivity.kt` (`setupListeners`).
- **Keep-screen-on toggle** — persisted; `FLAG_KEEP_SCREEN_ON` only while metering
  and enabled. `MainActivity.kt` (`applyKeepScreenOn`),
  `data/SettingsRepository.kt` (`keepScreenOnDuringRide`).
- **Runtime location permission** — request on launch, metering starts immediately on
  grant. `MainActivity.kt` (`requestPermissions`, `onRequestPermissionsResult`).

## Theming

- **Full light/dark token set** — `res/values/colors.xml`, `res/values-night/`
  (colors, styles, themes), dimens/type scale per `docs/DESIGN_SYSTEM.md`.
