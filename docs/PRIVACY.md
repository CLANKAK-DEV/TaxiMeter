# TaxiMeter — Privacy (data inventory)

What the app actually collects, stores, and transmits, verified against source.
This is an engineering inventory, not a legal privacy policy.

## Data the app processes

| Data | Where it comes from | Where it lives | Leaves the device? |
|---|---|---|---|
| Precise location (lat/lng, speed) | Fused Location Provider (`MainActivity.handleNewLocation`) | Held in memory only during a ride; used to derive distance/speed/waiting time | Not by app code (see Google SDKs below) |
| Trip records: start/end time, distance, duration, waiting minutes, fare, currency, country, city, vehicle type | Computed at ride end (`MainViewModel.saveCompletedTrip`) | Room DB `taximeter.db`, `trips` table (`data/local/Entities.kt`) | Only when the user shares (receipt text or CSV via `ACTION_SEND`) |
| Driver tariff overrides | Tariff editor dialog | Room `fare_overrides` table | No |
| Preferences: country/city, keep-screen-on, (unused: language, theme, FX, notifications) | Settings UI | SharedPreferences `taximeter_prefs` (`SettingsRepository.kt`) | No |
| CSV export files | Export button | `Android/data/com.myapp.taximeter/files/exports/` | Only via the user's chosen share target |

## What is NOT collected

- **No per-point GPS trail is stored.** `TripEntity.polylineEncoded` exists in the
  schema but is always written as `null` (`MainViewModel.saveCompletedTrip`). Only
  aggregate distance and the selected country/city label persist.
- **No accounts, no login, no user identifiers** — `firebase-auth` is a declared but
  unused dependency; there is no sign-in code.
- **No analytics events.** `firebase-analytics` was removed from dependencies, and
  the manifest disables analytics collection, ad-ID collection, and ad
  personalization; the `AD_ID` permission is stripped (`AndroidManifest.xml`).
  Earlier released versions (≤ 1.8) bundled AdMob (`play-services-ads`) — removed in
  the current pass (see `CHANGELOG.md`).
- **No app-owned backend.** App code performs zero network requests (no Retrofit
  call sites, no URLs in `app/src/main/java/`).

## Third-party data flows (bundled SDKs)

- **Google Maps SDK** fetches map tiles — Google receives the device IP, API key,
  and map viewport while the map is on screen. This is inherent to showing the map.
- **Google Play services / Firebase runtime** initializes at startup
  (google-services plugin + `app/src/google-services.json`). With analytics disabled
  in the manifest, no analytics data should be sent, but Play services' own
  telemetry is outside the app's control. `firebase-database`/`firestore` are never
  called, so no app data is written to Firebase.
- **Cloud backup**: `android:allowBackup="true"` with no exclusion rules wired means
  Android may back up `taximeter.db` and prefs to the user's Google account
  (see `docs/SECURITY.md`, Storage). Trip history therefore *can* leave the device
  via the OS backup mechanism, encrypted to the user's account.

## Sharing is always user-initiated

The only code paths that emit trip data are `MainActivity.shareLastTrip()` (plain-text
receipt) and `MainActivity.exportTripHistoryCsv()` (CSV) — both open a system share
chooser; nothing is sent automatically.

## Gaps to close before a store "Data safety" declaration

1. Decide the backup posture (exclude `taximeter.db` or declare backup).
2. Remove unused Firebase SDKs so the declaration doesn't have to reason about them.
3. If FX/weather APIs are added later (Retrofit is staged for this,
   `app/build.gradle.kts` comment), this document must be updated first — those calls
   would be the first app-initiated network traffic.
