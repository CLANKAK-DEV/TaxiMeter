# TaxiMeter — Security Notes

Honest inventory based on the working tree. No penetration testing was performed;
"not observed" means not found in the code read, not proven absent.

## Secrets & credentials

- **Google Maps API key committed in plaintext** at
  `app/src/main/res/values/strings.xml` line 3 (`google_maps_key`). It ships in the
  APK either way, but living unrestricted in a Git repo it can be lifted and abused
  for billing. Action: restrict the key in Google Cloud Console to the Android app
  signature + package, and rotate it; longer term move to the Secrets Gradle Plugin
  (the README already suggests this).
- **`app/src/google-services.json` is committed** (Firebase project `taxi-dabd8`).
  It contains Firebase API keys and two client entries — a stale
  `com.example.taximeter` and the current `com.myapp.taximeter`. Committing this file
  is common practice, but the stale client should be removed from the Firebase
  project, and the keys should have API restrictions applied. Note the file sits at
  the nonstandard `app/src/` location (not `app/google-services.json`).
- **Release keystore is NOT in the repo** (verified: no `app/release-keystore.jks`).
  Signing reads `KEY_ALIAS` / `KEY_PASSWORD` / `KEYSTORE_PASSWORD` from environment
  variables (`app/build.gradle.kts` `signingConfigs`); CI decodes the keystore from
  the `KEYSTORE_BASE64` GitHub secret (`.github/workflows/playstore-release.yml`).
  Caveat: the config falls back to `""` for missing env vars, so a misconfigured
  release build fails late (at signing) rather than early.

## Storage

- **Room database `taximeter.db` is unencrypted** (standard for local app data).
  It stores trip records: start/end timestamps, distance, fare, country/city
  (`data/local/Entities.kt`). No per-point GPS coordinates are stored
  (`polylineEncoded` is always null).
- **SharedPreferences `taximeter_prefs`** stores only preferences (country, city,
  toggles) — no credentials exist in the app (no login).
- **CSV exports** are written to app-specific external storage
  (`Android/data/com.myapp.taximeter/files/exports/`,
  `MainActivity.exportTripHistoryCsv`). No storage permission is needed, but on
  API 24–29 devices (minSdk is 24) other apps holding `READ_EXTERNAL_STORAGE` can
  read app-specific external directories. The export contains the full trip ledger.
- **Cloud backup is wide open.** `AndroidManifest.xml` sets
  `android:allowBackup="true"` and does **not** reference
  `res/xml/backup_rules.xml` or `res/xml/data_extraction_rules.xml` — both files are
  commented-out templates. Result: the trip database and prefs are eligible for
  Google cloud backup with default rules. Decide deliberately: either wire exclusion
  rules or accept (and document) backup of trip data.

## Network

- **App code makes no network calls.** No Retrofit usage, no URLs in
  `app/src/main/java/` (verified by grep). `INTERNET` permission exists for the
  Google Maps SDK (tiles) and the bundled Play Services/Firebase libraries.
- **Unused network-capable SDKs enlarge the attack surface**: `firebase-auth`,
  `firebase-database`, `firebase-firestore-ktx` are compiled in with zero call sites
  (`app/build.gradle.kts`). Firebase still initializes at startup via the
  google-services plugin. Removing them shrinks both APK and exposure.
- Firebase Analytics collection, ad-ID collection, and ad personalization are
  explicitly disabled by manifest meta-data; the `AD_ID` permission is removed via
  `tools:node="remove"` (`AndroidManifest.xml`).

## Permissions

Declared in `AndroidManifest.xml`: `INTERNET`, `ACCESS_FINE_LOCATION`,
`ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS` (marked not required; never actually
requested at runtime — dead declaration until notifications ship). Location is
requested at runtime with rationale-free system dialog
(`MainActivity.requestPermissions`). No background-location permission — consistent
with the (current) foreground-only metering.

## Components, WebView, deep links

- Single exported component: `MainActivity` with only the MAIN/LAUNCHER intent
  filter. No other activities, services, receivers, or providers declared.
- No WebView anywhere in the source. No custom URL schemes or App Links.
- ZXing (barcode) libraries are bundled but never invoked.

## Supply chain / release pipeline

- **CI deploys to the Play *production* track on every push to `master`** with no
  test, lint, or manual-approval gate (`.github/workflows/playstore-release.yml`,
  `track: production`, `status: completed`). One bad merge ships to users.
- **A signed release artifact is committed**: `app/release/app-release.aab` (tracked
  and currently modified). Binary artifacts in Git are a hygiene and provenance
  problem; remove and gitignore `*.aab`/`*.apk`.
- R8 minification + resource shrinking are on for release; `app/proguard-rules.pro`
  is empty (comments only). Models are `@Keep`-annotated (`data/model/FareModels.kt`).
- Dependencies pin fixed versions; several are dated (Room 2.5.0,
  play-services-maps 18.1.0, zxing 3.4.1). No dependency-audit tooling is configured.
