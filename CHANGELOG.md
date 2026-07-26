# Changelog

All notable changes to TaxiMeter are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions follow the
`versionName` in `app/build.gradle.kts`.

## [Unreleased] — 1.9 (versionCode 18), current uncommitted pass

### Added
- Layered architecture under the new `com.myapp.taximeter` source package
  (`app/src/main/java/com/myapp/`): `data/` (`FareRepository`, `TripRepository`,
  `SettingsRepository`, `GlobalFareDatabase`, Room `AppDatabase` + DAOs + entities),
  `domain/` (`FareEngine`, `EarningsCalculator`, `TripCsvBuilder`), `ui/main/`
  (`MainViewModel` + factory), `ui/history/` (`TripHistoryAdapter`), and
  `TaxiMeterApp` composition root (registered in `AndroidManifest.xml`).
- Room persistence (`taximeter.db`, schema v1) for trips and driver fare overrides,
  with exported schema checked in at `app/schemas/com.myapp.taximeter.data.local.AppDatabase/1.json`.
- Earnings dashboard: today / 7-day / 30-day totals and a view-based 7-day bar chart
  (`dialog_earnings.xml`, `EarningsCalculator.kt`, `bar_earnings.xml`, `ic_earnings.xml`).
- Trip history dialog with totals strip, clear-all confirmation, and CSV export
  (`dialog_trip_history.xml`, `item_trip_history.xml`, `TripCsvBuilder.kt`).
- Searchable country tariff picker (`dialog_country_picker.xml`).
- Trip-in-progress safeguards: confirmations on Reset and system Back while metering;
  persisted keep-screen-on toggle (`SettingsRepository.keepScreenOnDuringRide`).
- Design-system token set: `values/dimens.xml`, `values-night/colors.xml`, reworked
  `colors.xml`/`styles.xml`/`themes.xml`, dark map style `raw/map_style_dark.json`,
  new drawables (`hint_background`, `ic_share`, `ic_flag_finish`, …).
- Docs: `docs/DESIGN_SYSTEM.md`; screenshots under `screnTAxiMeter/`.

### Changed
- Source moved from `com.example.taximeter` to `com.myapp.taximeter`
  (old `app/src/main/java/com/example/taximeter/MainActivity.kt` deleted).
  The applicationId is `com.myapp.taximeter`; any build that shipped under
  `com.example.taximeter` is a **different application** to Android — this version
  installs alongside it and does **not** migrate its trips or settings.
- `versionCode` 17 → 18, `versionName` 1.8 → 1.9 (`app/build.gradle.kts`).
- Annotation processing migrated from kapt to KSP; Room compiler now runs via
  `ksp(...)` with `room.schemaLocation` export configured.
- Instrumented test updated to assert the new package name
  (`app/src/androidTest/java/com/example/taximeter/ExampleInstrumentedTest.kt`).
- README rewritten around the new feature set and architecture.
- Layouts/dialogs restyled to the token system (`activity_main.xml`,
  `dialog_settings.xml`, `dialog_currency.xml`, `item_currency.xml`, `summary_row.xml`).

### Removed
- AdMob: `play-services-ads` dependency and the AdMob `APPLICATION_ID` meta-data
  removed from `app/build.gradle.kts` / `AndroidManifest.xml`; `AD_ID` permission
  stripped via `tools:node="remove"`.
- `firebase-analytics` dependency removed; analytics/ad-personalization collection
  explicitly disabled via manifest meta-data.
- Placeholder unit test deleted (`app/src/test/java/com/example/taximeter/ExampleUnitTest.kt`)
  — unit-test coverage is now zero (see `docs/TESTING.md`).
- Legacy `background_image.png` drawable.

### Known issues in this pass
- Waiting-time accrual assumes 1 s per GPS callback while updates arrive every 1–2 s
  (`MainActivity.handleNewLocation`).
- Active-ride state is lost on rotation/process death; no foreground service.
- `totalFareUsd` is stored with a hardcoded 1.0 rate.
- Retrofit/Moshi, ZXing, easypermissions, and Firebase auth/database/firestore are
  declared but unused. See `docs/AUDIT.md` for the full list.

## [1.8] (versionCode 17) and earlier — committed history

Pre-refactor single-file app (`com.example.taximeter.MainActivity`, ~45 KB) with
AdMob and Firebase Analytics dependencies; released via
`.github/workflows/playstore-release.yml` (tag history: `V3`, `V4` commits).
