# TaxiMeter

A professional Android taxi meter for drivers. It turns an Android device into a
GPS-driven fare meter with global tariff intelligence, live route planning, trip
history, and a high-contrast, glanceable interface designed for use behind the
wheel — in daylight and at night.

---

## Features

### Live metering
- **GPS fare engine** — fare is computed continuously from distance (Fused
  Location Provider, 2 m noise filter), trip time, and waiting time (speed
  < 5 km/h), with automatic night (22:00–06:00) and weekend surcharges and a
  traffic multiplier at low speeds.
- **Glanceable HUD** — status, elapsed time, distance, and speed in tabular
  monospace figures that never jitter; a 56sp fare readout readable in
  sunlight.
- **Clear ride states** — Available / On Trip / Paused with color-coded status
  dot and start/stop button states.

### Global tariffs
- **30+ country tariff database** — base fare, per-km, per-minute, waiting
  rate, surcharges, and currency (symbol + decimal rules) per country.
- **Searchable country picker** — selecting a country reconfigures the fare
  engine and currency instantly, and is remembered across launches.
- **Driver tariff editor** — override base fare, per-km, per-minute, waiting
  rate, and night surcharge for your country/vehicle type; overrides persist in
  Room and can be reset to the country default at any time.

### Trips
- **Trip history** — every completed ride is saved locally (Room) and browsable
  from the history dialog with totals: trip count, total km, and earnings.
- **Trip summary + share** — end-of-ride summary (fare, distance, time, average
  speed) with a one-tap shareable text receipt (WhatsApp, SMS, email, …).
- **Route planning** — drop pickup/destination pins on the map for a dashed
  route preview with distance and fare estimate (Haversine).

### Driver tools
- **Earnings dashboard** — daily / last-7-days / last-30-days earnings, trip
  count, and kilometers computed from the Room trip table, with a simple
  7-day bar visualization built from plain views (no chart library). Opened
  from the Earnings button in the bottom sheet.
- **CSV export** — one tap in Trip History exports the full history as CSV
  (start/end time, distance, duration, waiting time, fare, currency, country,
  city, vehicle type). A copy is written to app-specific external storage
  (`Android/data/…/files/exports/`, no extra permission) and the content is
  shared via `ACTION_SEND` as `text/csv` (email, Drive, Files, …).
- **Trip-in-progress safeguard** — resetting the meter or backing out of the
  app while a ride is active requires explicit confirmation, so an unsaved
  fare can't be discarded by a stray tap. Stopping a ride normally is
  unaffected.
- **Keep-screen-on toggle** — an optional "Screen on" switch (persisted)
  holds the display awake only while the meter is running, for dashboard-
  mounted use.

### Design
- **Light and dark themes** — full `values-night` token set, including a dark
  Google Maps style so the map matches the app at night.
- **Accessibility** — 48dp+ touch targets, content descriptions on every
  control, WCAG-AA contrast in both themes.

See [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) for the complete palette,
typography, and component rules.

---

## Screens

| Screen | Description |
|---|---|
| **Meter (main)** | Map with HUD strip (status · time · km · km/h), FAB stack (my location, history, pickup pin, destination pin), and a bottom sheet with the fare readout, Start/Stop, Pause, Reset (confirmed while metering), plus a Screen-on switch and Earnings / Tariff shortcuts. |
| **Trip summary** | Overlay card after each ride: fare, distance, duration, average speed; Share and Close actions. |
| **Trip history** | Dialog with totals strip (trips / total km / earnings), scrollable trip list, Export CSV, and Clear All (confirmed). |
| **Earnings** | Dialog with Today / Last 7 days / Last 30 days rows (earnings, trips, km) and a view-based 7-day bar chart in brand yellow. |
| **Country picker** | Searchable list of countries with flag, currency code, and indicative $/km rate. |
| **Tariff editor** | Form for base fare, per-km, per-minute, waiting rate, and night surcharge %, with Save / Cancel / Reset-to-default. |

---

## Architecture

```
com.myapp.taximeter
├── MainActivity.kt          # UI wiring, GPS callbacks, map, dialogs
├── TaxiMeterApp.kt          # Application: Room + repository composition root
├── data/
│   ├── GlobalFareDatabase   # Static per-country FareConfig table
│   ├── FareRepository       # Active profile = country default + driver override
│   ├── TripRepository       # Trip persistence (Room)
│   ├── SettingsRepository   # SharedPreferences (country, city, prefs)
│   ├── local/               # Room: AppDatabase, DAOs, entities
│   └── model/               # FareConfig, CurrencyInfo, DriverFareOverride, …
├── domain/
│   └── FareEngine           # Pure, deterministic fare calculation
└── ui/
    ├── main/                # MainViewModel (+ factory), TripMetrics
    └── history/             # TripHistoryAdapter
```

- **Pattern**: MVVM. `MainViewModel` exposes `activeProfile`, `fareBreakdown`,
  and `tripHistory` as LiveData; `FareEngine` is a pure Kotlin object with no
  Android dependencies (unit-testable).
- **Storage**: Room (`trips`, `fare_overrides`, `user_profile` tables) +
  SharedPreferences for lightweight settings.
- **Location**: Fused Location Provider, high-accuracy updates every 1–2 s
  while the activity is alive.
- **Maps**: Google Maps SDK (`MapView`), with a night map style applied from
  `res/raw/map_style_dark.json` when the device is in dark mode.

---

## Permissions

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Live metering: distance, speed, and waiting-time detection. |
| `INTERNET` | Map tiles and (future) FX/remote tariff data. |
| `POST_NOTIFICATIONS` (optional) | Ride activity notification channel. |
| `AD_ID` | Explicitly **removed** (`tools:node="remove"`); analytics collection is disabled in the manifest. |

---

## How to build

### Prerequisites
- Android Studio (Ladybug or newer), JDK 17+
- A Google Maps API key

### Steps
1. Clone the repository and open it in Android Studio.
2. Provide your Maps key (replace `google_maps_key` in
   `app/src/main/res/values/strings.xml`, or wire the Secrets Gradle Plugin).
3. Sync Gradle — Room/Moshi code generation runs through **KSP** (not kapt).
4. Run on a device/emulator with Location Services enabled
   (`minSdk 24`, `targetSdk 35`).

Release builds are minified (R8 + resource shrinking) and signed via
environment variables (`KEY_ALIAS`, `KEY_PASSWORD`, `KEYSTORE_PASSWORD`) with a
local `release-keystore.jks`.

---

## Screenshots

Captures from a physical device (Aug 2025) live in the repo:

- ![Meter screen](<screnTAxiMeter/taxiflay scren/Screenshot_20250802_164906.png>)
- ![Ride in progress](<screnTAxiMeter/taxiflay scren/Screenshot_20250802_164923.png>)
- ![Dialogs](<screnTAxiMeter/taxiflay scren/Screenshot_20250802_164941.png>)
- ![Dark theme](<screnTAxiMeter/taxiflay scren/Screenshot_20250802_171232.png>)

Retake after UI changes (see `docs/RELEASE_CHECKLIST.md` §8).

---

## Tech stack

- Kotlin 1.9.0, Android Views (no Compose), single activity — AGP 8.4.1, KSP
- `minSdk 24`, `targetSdk 35`, `compileSdk 35`, JVM target 1.8
- AndroidX: AppCompat, ConstraintLayout, Lifecycle (ViewModel/LiveData) 2.6.1, Room 2.5.0
- Google: Maps SDK 18.1.0, Play Services Location 21.0.1, Material Components
- Declared but currently unused (see `docs/AUDIT.md`): Firebase Auth/Database/Firestore,
  Retrofit 2.11 + Moshi, ZXing, EasyPermissions

---

## Configuration

- **Google Maps key** — `google_maps_key` in `app/src/main/res/values/strings.xml`,
  referenced from `AndroidManifest.xml`. The committed key should be treated as
  compromised: restrict/rotate it in Google Cloud Console (`docs/SECURITY.md`).
- **Firebase** — `app/src/google-services.json` (project `taxi-dabd8`), consumed by
  the `com.google.gms.google-services` plugin. Analytics/ad-ID collection is
  disabled via manifest meta-data.
- **Room schema export** — `room.schemaLocation` points at `app/schemas/`
  (`app/build.gradle.kts`); commit new schema JSON with any DB version bump.

---

## Build commands

```bash
./gradlew assembleDebug          # debug APK
./gradlew bundleRelease          # signed release AAB (needs signing env vars)
./gradlew lintDebug              # Android Lint
```

---

## Test commands

```bash
./gradlew testDebugUnitTest          # JVM unit tests (none exist yet — docs/TESTING.md)
./gradlew connectedDebugAndroidTest  # instrumented tests (one template test)
```

---

## Environment variables

Required only for release signing (`app/build.gradle.kts` `signingConfigs`):

| Variable | Purpose |
|---|---|
| `KEY_ALIAS` | Alias inside `app/release-keystore.jks` |
| `KEY_PASSWORD` | Key password |
| `KEYSTORE_PASSWORD` | Keystore password |

CI additionally uses GitHub secrets `KEYSTORE_BASE64`, `PLAY_STORE_CREDENTIALS`,
and `PACKAGE_NAME` (`.github/workflows/playstore-release.yml`). Missing variables
fall back to empty strings and fail only at signing time.

---

## Known limitations

- An active ride does not survive screen rotation or process death, and there is no
  foreground service — metering degrades when the app is backgrounded.
- Waiting time is approximated (1 s added per GPS callback at a 1–2 s interval).
- The country picker exposes 20 of the 36 built-in tariff configs; vehicle types and
  the airport surcharge have no UI yet.
- `totalFareUsd` in the trip table uses a hardcoded 1.0 rate (placeholder).
- No translations ship yet (`values-ar/`, `values-fr/` are empty); UI is English only.
- Unit-test coverage is currently zero. Full list with evidence: `docs/AUDIT.md`.

---

## Release process

Pushing to `master` triggers `.github/workflows/playstore-release.yml`, which builds
`bundleRelease` and uploads straight to the Play **production** track — there is no
test/lint gate in CI, so complete `docs/RELEASE_CHECKLIST.md` before merging.
Note: upgrades from any old `com.example.taximeter` build install as a separate app;
that data is not migrated (applicationId changed to `com.myapp.taximeter`).

---

## License

Copyright © 2026. All rights reserved.
