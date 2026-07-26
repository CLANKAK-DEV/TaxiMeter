# TaxiMeter — Testing

## Current state: coverage is effectively zero

- **Unit tests: none.** The only unit test file the project ever had
  (`app/src/test/java/com/example/taximeter/ExampleUnitTest.kt`, the `2 + 2 = 4`
  template) was **deleted** in the current uncommitted pass (visible in `git status`).
  There is no `app/src/test/` directory anymore.
- **Instrumented tests: one template.**
  `app/src/androidTest/java/com/example/taximeter/ExampleInstrumentedTest.kt` —
  a single `useAppContext()` test asserting
  `appContext.packageName == "com.myapp.taximeter"`. It exercises no app code.
  (Note it still lives under the old `com/example/taximeter` path.)
- No CI test gate: `.github/workflows/playstore-release.yml` runs only
  `./gradlew clean bundleRelease` before deploying to the Play production track.
- Test dependencies are wired (`junit 4.13.2`, `androidx.test.ext:junit 1.2.1`,
  `espresso-core 3.6.1` in `app/build.gradle.kts` / `gradle/libs.versions.toml`),
  so adding tests requires no build changes.

## How to run

```bash
# JVM unit tests (once they exist)
./gradlew testDebugUnitTest

# Instrumented tests (device/emulator required)
./gradlew connectedDebugAndroidTest

# Android Lint
./gradlew lintDebug
```

## Why the current gap is risky

The app computes money. `FareEngine.calculateFare()` contains ordered surcharge and
multiplier math, a pre-clamp minimum-fare adjustment (a regression already fixed once,
per the comment at `FareEngine.kt` lines 93–96), and currency-decimal rounding — all
unverified. `EarningsCalculator` does calendar bucketing that is easy to break across
DST/timezone boundaries. Both are pure Kotlin with zero Android dependencies, i.e.
the cheapest possible code to test — and they are untested.

## Prioritized test plan

### P0 — pure domain (JVM unit tests, no emulator)

1. **`domain/FareEngine.kt`** — the top target:
   - component sums: base/distance/time/waiting for a known config;
   - night, weekend, and combined surcharges (percent of pre-surcharge subtotal);
   - airport flat surcharge with vehicle multiplier;
   - traffic and surge ordering (surge applies after traffic top-up);
   - minimum-fare adjustment reported when raw subtotal is below `minimumFare`,
     and zero when above (regression test for the pre-clamp fix);
   - rounding for 0-decimal (JPY) vs 2-decimal currencies;
   - negative inputs clamped by `max(x, 0.0)`;
   - `resolveEffectiveConfig`: null override fields fall back to base.
2. **`domain/EarningsCalculator.kt`** — second target:
   - trips on today / 6 days ago / 7 days ago land in the right buckets;
   - `last7DayEarnings` has 7 entries ordered oldest→today;
   - fixed `TimeZone` injection (the API already accepts one);
   - `currencySymbol` null when currencies are mixed, set when uniform.
3. **`domain/TripCsvBuilder.kt`** — header constant matches row field order; RFC 4180
   quote doubling; null city → `""`; Locale.US decimal points regardless of default
   locale; timezone-injected timestamp formatting.

### P1 — data layer

4. `FareRepository.buildKey` casing/nullable-city behavior (JVM, DAO faked) and
   override round-trip via an in-memory Room database (instrumented).
5. `TripDao` queries against in-memory Room: ordering, `clearAll`, aggregate sums.

### P2 — behavior

6. `MainViewModel` state transitions with fake repositories (needs
   `InstantTaskExecutorRule` + a coroutine test dispatcher).
7. Espresso smoke test: start ride → stop below 0.05 km → nothing saved; verify the
   discard-confirmation dialogs appear while metering.

### Blockers to broader testing

`MainActivity.kt` (978 lines) owns GPS, ride state, and dialogs, making most ride
logic untestable without an emulator. The decomposition in `docs/ROADMAP.md` P0 is a
prerequisite for testing ride-state transitions and waiting-time accrual off-device.
