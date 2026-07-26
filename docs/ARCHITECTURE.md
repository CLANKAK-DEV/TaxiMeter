# TaxiMeter — Architecture (as it is)

Kotlin, Android Views, single-activity. AGP 8.4.1, Kotlin 1.9.0, KSP
(`gradle/libs.versions.toml`, `app/build.gradle.kts`). `minSdk 24`, `targetSdk 35`,
Java/Kotlin target 1.8.

## Module / package structure

Single Gradle module `:app`. Real package layout under
`app/src/main/java/com/myapp/taximeter/`:

```
com.myapp.taximeter
├── TaxiMeterApp.kt          Application: builds Room DB + repositories (manual DI)
├── MainActivity.kt          978 lines: UI wiring, GPS, map, 6 dialogs, CSV I/O,
│                            chart rendering, CountryRateAdapter, legacy country list
├── data/
│   ├── GlobalFareDatabase.kt   Static list of 36 FareConfig (per-country tariffs)
│   ├── FareRepository.kt       Active profile = country default ⊕ driver override
│   ├── TripRepository.kt       Thin wrapper over TripDao
│   ├── SettingsRepository.kt   SharedPreferences ("taximeter_prefs")
│   ├── local/                  AppDatabase (v1, exportSchema=true), Daos.kt, Entities.kt
│   └── model/FareModels.kt     FareConfig, CurrencyInfo, DriverFareOverride,
│                               ActiveFareProfile, VehicleType (@Keep annotated)
├── domain/
│   ├── FareEngine.kt           Pure fare math + FareProfileResolver
│   ├── EarningsCalculator.kt   Pure day/7d/30d bucketing
│   └── TripCsvBuilder.kt       Pure CSV serialization (RFC 4180, Locale.US)
└── ui/
    ├── main/                   MainViewModel (+ factory), TripMetrics
    └── history/                TripHistoryAdapter
```

Note: `app/src/androidTest/java/com/example/taximeter/` still uses the old package
path (template test only).

## Dependency injection

None (no Hilt/Koin). `TaxiMeterApp.onCreate()` constructs `AppDatabase`
(`taximeter.db`, `fallbackToDestructiveMigration()`), `SettingsRepository`,
`FareRepository`, `TripRepository`; `MainActivity` pulls them via
`application as TaxiMeterApp` into `MainViewModelFactory`.

## Data flow

1. **Location → metrics (Activity-owned).** FusedLocationProvider high-accuracy
   updates every 2000 ms (min 1000 ms) → `MainActivity.handleNewLocation()` mutates
   `totalDistance` (2 m noise filter), `currentSpeed`, `totalWaitingMinutes`
   (speed < 5 km/h).
2. **Metrics → fare (ViewModel).** A 1 s `Handler` loop (`startUIUpdateLoop`) builds
   `TripMetrics` and calls `MainViewModel.recalculateFare()`, which runs
   `FareEngine.calculateFare(profile.fareConfig, input)` and posts a `FareBreakdown`
   LiveData that the Activity renders.
3. **Tariff resolution.** `FareRepository.getActiveProfile()` →
   `FareProfileResolver.resolve()`: `GlobalFareDatabase.findByCountryCode()` +
   optional Room override (key `COUNTRY_city_VEHICLETYPE`, `FareRepository.buildKey`).
4. **Trip completion.** `stopRide()` (only if distance > 0.05 km) →
   `MainViewModel.saveCompletedTrip()` → `TripRepository.saveTrip()` (Room insert).
5. **History/earnings/CSV.** `TripDao` Flows → `collectLatest` into LiveData
   (`observeHistory`, 20 most recent); earnings and CSV pull `getAllTrips().first()`
   and run the pure domain objects.

## State management — the split brain (debt)

- **ViewModel (survives rotation):** `activeProfile`, `fareBreakdown`, `tripHistory`,
  `earningsSummary` (LiveData), `currentVehicleType`, `lastCountryCode`/`lastCity`.
- **Activity (lost on rotation/process death):** `rideState`, `startTime`,
  `totalDistance`, `totalWaitingMinutes`, `pausedTime`, `pauseStartTime`,
  `lastTripShareText`, map markers. No `onSaveInstanceState`, no `SavedStateHandle`,
  no foreground service — an active ride does not survive configuration change.
- `MapView` lifecycle forwarding is incomplete: `onStart/onStop/onSaveInstanceState`
  are not forwarded (only resume/pause/destroy/lowMemory), which the Maps SDK requires.

## Persistence

| Store | Contents | Notes |
|---|---|---|
| Room `taximeter.db` v1 (`data/local/AppDatabase.kt`) | `trips`, `fare_overrides`, `user_profile` | Schema exported to `app/schemas/…/1.json`; `fallbackToDestructiveMigration()` will wipe data on any future version bump; `user_profile` table is written by nothing |
| SharedPreferences `taximeter_prefs` (`SettingsRepository.kt`) | country/city, language, theme, FX flag, notifications flag, keep-screen-on | Only `defaultCountryCode`, `defaultCity`, `keepScreenOnDuringRide` are actually used |
| App-external files `Android/data/com.myapp.taximeter/files/exports/` | CSV copies from export (`MainActivity.exportTripHistoryCsv`) | Best-effort; share via `ACTION_SEND` `EXTRA_TEXT` is the primary path |

## Threading

Coroutines via `viewModelScope` for all DB work; Room DAOs are `suspend`/`Flow`.
Location callbacks and the UI tick run on the main looper. No WorkManager/services.

## Declared but unused (dead weight)

- Libraries with zero imports in `app/src/main/java/`: `firebase-auth`,
  `firebase-database`, `firebase-firestore-ktx`, Retrofit + Moshi (+ codegen),
  `zxing:core`, `zxing-android-embedded`, `easypermissions` (`app/build.gradle.kts`).
- Code: `UserProfileEntity`/`UserProfileDao`, `TripRepository.observeStats()`,
  `MainViewModel.changeVehicleType()` (no caller → vehicle types unreachable from UI),
  `SettingsRepository.{themeMode, notificationsEnabled, showFxCurrencies, languageCode}`,
  `TripEntity.{weatherSummary, polylineEncoded}` (always null).
- Duplicate data: `MainActivity.countryRates` (20 entries) + `codeToIso()`/
  `getFlagByCountryCode()` shadow `GlobalFareDatabase` (36 entries).
