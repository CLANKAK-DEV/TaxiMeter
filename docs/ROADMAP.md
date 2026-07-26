# TaxiMeter — Roadmap

Prioritized from the findings in `docs/AUDIT.md`, plus every deferred item found in
code comments and existing docs. Evidence paths included so items stay verifiable.

## P0 — correctness and money (do before any release)

1. **Fix waiting-time accrual.** Use real elapsed time between fixes instead of a
   fixed 1 s per callback (`MainActivity.handleNewLocation` adds `1.0/60.0` min while
   updates arrive every 1–2 s per `startLocationUpdates`).
2. **Make an active ride survive rotation and process death.** Move `rideState`,
   `startTime`, `totalDistance`, `totalWaitingMinutes`, `pausedTime` out of
   `MainActivity` into `MainViewModel` + `SavedStateHandle`.
3. **Foreground service for metering** so GPS keeps flowing when the screen locks or
   the driver switches apps; post the ride notification on the already-created
   channel (`MainActivity.createNotificationChannel` currently posts nothing) and
   request `POST_NOTIFICATIONS` at runtime.
4. **Stop storing fabricated USD fares.** `stopRide()` passes `usdRate = 1.0`
   (comment: "Simple parity for now or fetch from FX repo") — store `null` until a
   real FX source exists.
5. **Unit tests for `FareEngine` and `EarningsCalculator`** (then `TripCsvBuilder`).
   Coverage is currently zero; both are pure Kotlin. Full plan: `docs/TESTING.md`.
6. **Gate CI.** `.github/workflows/playstore-release.yml` ships straight to the Play
   production track on push to `master` with no tests/lint — add
   `testDebugUnitTest` + `lintDebug` steps and consider an internal track first.

## P1 — architecture debt and trust

7. **Decompose `MainActivity.kt` (978 lines — the main architecture debt).** Extract:
   ride/location tracking (service or controller class), each dialog
   (country picker, tariff editor, history, earnings) into its own class or
   `DialogFragment`, `CountryRateAdapter` into `ui/`, and the chart builder
   (`buildEarningsBars`) into a custom view.
8. **Single source of tariff truth.** Delete the 20-entry `countryRates` list,
   `codeToIso()`, and `getFlagByCountryCode()` from `MainActivity`; drive the picker
   from `GlobalFareDatabase` (36 configs) so all countries are selectable.
9. **Remove unused dependencies**: `firebase-auth`, `firebase-database`,
   `firebase-firestore-ktx`, ZXing (both artifacts), `easypermissions` — zero call
   sites (`app/build.gradle.kts`). Keep Retrofit/Moshi only if item 14 is imminent.
10. **Real Room migrations.** Replace `fallbackToDestructiveMigration()`
    (`TaxiMeterApp.kt`) before any schema change, or v2 wipes every driver's ledger.
11. **Backup posture.** Wire `res/xml/backup_rules.xml` / `data_extraction_rules.xml`
    (currently unreferenced templates) into the manifest, deciding explicitly whether
    `taximeter.db` is backed up (`docs/SECURITY.md`).
12. **Maps key hygiene**: restrict + rotate the committed key
    (`res/values/strings.xml`), move to Secrets Gradle Plugin (README already
    suggests it); remove committed `app/release/app-release.aab` and gitignore
    `*.aab`/`*.apk`.
13. **Complete `MapView` lifecycle forwarding** (`onStart/onStop/onSaveInstanceState`
    missing in `MainActivity`).

## P2 — deferred features already staged in code/docs

14. **FX / remote data integrations.** Deferred explicitly: Retrofit kept "for future
    API integrations (e.g. weather, FX rates)" (`app/build.gradle.kts` comment),
    README Permissions table ("(future) FX/remote tariff data"), unused
    `SettingsRepository.showFxCurrencies`, and `TripEntity.totalFareUsd`/
    `weatherSummary` columns waiting for data.
15. **Vehicle-type selector UI.** Engine multipliers and
    `MainViewModel.changeVehicleType()` exist with no caller — ECONOMY is the only
    reachable type.
16. **Airport-trip toggle.** `updateUI()` passes `isAirportTrip = false` with the
    comment "Could add a toggle in UI"; `airportSurcharge` data exists for all 36
    countries.
17. **Per-city tariffs and night hours.** `GlobalFareDatabase.kt` notes night hours
    "could be made per-city"; `majorCities` data and the city-aware override key
    (`FareRepository.buildKey`) are already in place.
18. **Route polyline storage** (`TripEntity.polylineEncoded`, always null) and a trip
    detail screen; real road routing instead of the Haversine dashed line.
19. **Localization**: fill the empty `values-ar/` and `values-fr/` dirs, move
    `FareEngine` component labels into resources, wire the dead
    `languageCode`/`preferredLanguageCode` settings (`docs/ACCESSIBILITY.md`).
20. **User profile & lifetime stats.** `UserProfileEntity`/`UserProfileDao` and
    `TripRepository.observeStats()` are built but unused.
21. **Accessibility fixes** from `docs/ACCESSIBILITY.md`: live-region announcements,
    chart text alternative, 200% font-scale audit, TalkBack pass.

## Explicit non-goals (until decided otherwise)

- No accounts/cloud sync (would contradict `docs/PRIVACY.md`'s offline-first stance).
- No ads: AdMob was deliberately removed in this pass (`CHANGELOG.md`); reintroducing
  it would invalidate the manifest's disabled-analytics posture.
