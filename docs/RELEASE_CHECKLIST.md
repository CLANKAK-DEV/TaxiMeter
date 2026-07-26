# TaxiMeter — Release Checklist

Concrete steps for shipping this app. Current release: `versionName 1.9`,
`versionCode 18` (`app/build.gradle.kts`). CI: `.github/workflows/playstore-release.yml`
builds and uploads to the Play **production** track on every push to `master` —
treat a push to `master` as a release action.

## 1. Version bump

- [ ] In `app/build.gradle.kts`: increment `versionCode` (Play rejects reused codes)
      and set `versionName`.
- [ ] Add a dated section to `CHANGELOG.md`; move `[Unreleased]` content into it.

## 2. Code health gates (run locally — CI does not run them)

- [ ] `./gradlew testDebugUnitTest` — note: as of this pass there are **no unit
      tests** (`docs/TESTING.md`); at minimum land FareEngine tests before relying
      on this gate.
- [ ] `./gradlew lintDebug` and triage new warnings.
- [ ] `./gradlew connectedDebugAndroidTest` on one physical device.
- [ ] Verify no debug logging leaks secrets (current `Log.w` calls in
      `MainActivity.kt` are benign).

## 3. Data & schema

- [ ] If any `@Entity`/`@Database` changed: bump the Room version, write a real
      migration, and commit the new JSON under
      `app/schemas/com.myapp.taximeter.data.local.AppDatabase/`.
      **Warning:** `TaxiMeterApp.kt` uses `fallbackToDestructiveMigration()` — a
      version bump without a migration silently wipes every user's trip history.
- [ ] Confirm the exported schema file in `app/schemas/` matches the shipped version.

## 4. Release build & R8 smoke test

- [ ] Build locally with signing env vars set: `KEY_ALIAS`, `KEY_PASSWORD`,
      `KEYSTORE_PASSWORD`, and `app/release-keystore.jks` present
      (`app/build.gradle.kts` falls back to empty strings — missing vars fail late).
- [ ] `./gradlew bundleRelease`, install the minified build
      (`bundletool` or `assembleRelease`) and smoke-test on-device:
      start/pause/stop a ride, tariff override save + reset, history + CSV export,
      earnings dialog, country switch. `app/proguard-rules.pro` is empty, so R8
      regressions surface only at runtime (models are `@Keep`-annotated in
      `data/model/FareModels.kt`, but verify Room/Flow paths).
- [ ] Test both themes (dark map style applies) and airplane-mode behavior
      (map tiles gone; meter must still run).

## 5. Upgrade-path check

- [ ] Install the previous Play build, then update-install the new AAB: settings and
      trips must survive.
- [ ] Remember: builds from the old `com.example.taximeter` era are a **different
      applicationId** — they update nothing; the new app installs beside them and
      old data is not carried over. Do not promise data migration from those builds.

## 6. Secrets & artifact hygiene

- [ ] Maps key (`res/values/strings.xml`) is restricted to the release signing cert
      in Google Cloud Console (see `docs/SECURITY.md` — currently committed
      unrestricted).
- [ ] `app/release-keystore.jks` is NOT committed (verify `git status`).
- [ ] Do not commit build outputs. `app/release/app-release.aab` is currently
      tracked — remove it from the index and add `*.aab` / `*.apk` to `.gitignore`
      as part of the next housekeeping commit.
- [ ] GitHub secrets present and current: `KEYSTORE_BASE64`, `KEY_ALIAS`,
      `KEY_PASSWORD`, `KEYSTORE_PASSWORD` (if wired), `PLAY_STORE_CREDENTIALS`,
      `PACKAGE_NAME` (= `com.myapp.taximeter`).

## 7. Ship

- [ ] Merge to `master` (note: local default branch is `main`; only `master`
      triggers the deploy workflow) — CI runs `./gradlew clean bundleRelease` and
      uploads to the **production** track, `status: completed` (no staged rollout).
      Until the workflow gains a test gate and a percentage rollout, this step IS
      the release — double-check everything above first.
- [ ] Watch the Actions run; confirm the new version appears in Play Console.
- [ ] Tag the commit (history uses `V3`/`V4`-style commits; prefer annotated tags
      like `v1.9` going forward).

## 8. Store listing consistency

- [ ] Screenshots up to date (`screnTAxiMeter/` holds Aug 2025 captures; retake
      after UI changes).
- [ ] Play "Data safety" answers match `docs/PRIVACY.md` (offline-first, no
      analytics, location processed in-memory only, backup posture).
- [ ] Release notes drawn from `CHANGELOG.md`.
