# TaxiMeter — Accessibility Status

Honest snapshot. "Documented" means asserted by design tokens/docs; nothing here has
been verified with TalkBack, Accessibility Scanner, or automated contrast tooling.

## Touch targets — good, by construction

- Token-enforced sizes in `app/src/main/res/values/dimens.xml`: `touch_target` 48dp,
  `button_height_primary` 64dp, `fab_size` 48dp, `list_item_min_height` 64dp.
- `activity_main.xml` uses them: primary ride buttons at
  `@dimen/button_height_primary` (lines ~380–411), FABs with
  `app:fabCustomSize="@dimen/fab_size"`, bottom-sheet buttons and the country button
  at `@dimen/touch_target`.

## Labels / screen reader support — partial

- 17 dedicated `cd_*` content-description strings exist
  (`res/values/strings.xml`); `activity_main.xml` applies 14 content descriptions
  (FABs, pause/resume, reset, fare amount, country flag, switch, summary actions).
- The pause button's description is updated dynamically on state change
  (`MainActivity.handlePauseResume` sets `cd_pause`/`cd_resume` text).
- Decorative views are excluded (`android:importantForAccessibility="no"` in
  `activity_main.xml`, `dialog_currency.xml`, `dialog_earnings.xml`, and the chart
  bars in `MainActivity.buildEarningsBars`).
- **Gaps:**
  - The 7-day earnings chart has **no text alternative** — bars are hidden from
    accessibility and only weekday labels remain; a TalkBack user gets no values.
  - No `accessibilityLiveRegion` anywhere (verified by grep) — the ticking fare,
    timer, and status changes are never announced.
  - The country search field (`dialog_country_picker.xml`) and tariff editor inputs
    rely on hints/labels; no `labelFor` associations were found.

## Contrast & color — documented, not measured

- `docs/DESIGN_SYSTEM.md` documents WCAG-AA-or-better ratios for every token pair
  (e.g. `on_surface` 16.1:1 light / 14.9:1 dark) and mandates that color is never
  the only state carrier (status dot always paired with a text label — confirmed in
  `MainActivity.updateStatusDisplay`).
- These ratios have **not** been independently re-measured in this audit; treat them
  as design intent verified only by inspection of `values/colors.xml` /
  `values-night/colors.xml` token values.

## Dark theme — complete

- Full night token set exists: `values-night/colors.xml`, `values-night/styles.xml`,
  `values-night/themes.xml`, plus a dark map style applied at runtime
  (`MainActivity.onMapReady` → `raw/map_style_dark.json`).

## Text scaling — untested

- All text sizes use `sp` (`values/dimens.xml` type scale, layout `textSize`
  attributes), so system font scaling applies.
- **Unverified risks:** the 56sp monospace fare readout and the fixed-height HUD/
  bottom-sheet rows have not been checked at 1.3–2.0x font scale; clipping is
  plausible. Several labels are 10–13sp, small at default scale.

## RTL / localization — declared, not delivered

- `android:supportsRtl="true"` is set (`AndroidManifest.xml`), and layouts use
  start/end-relative Material components.
- But `res/values-ar/` and `res/values-fr/` are **empty directories** — every one of
  the 127 strings ships in English only. `SettingsRepository.languageCode` and
  `UserProfileEntity.preferredLanguageCode` are dead — no locale switching exists.
- `FareEngine.kt` hardcodes English breakdown labels ("Base fare", "Night
  surcharge", …) inside domain output, so localization requires a domain change.
- RTL mirroring has not been visually tested; concatenated formats like
  `"%.2f km · %s · %s"` (`TripHistoryAdapter.kt`) may render awkwardly in RTL.

## Priority fixes

1. Announce ride-state and fare milestones (`accessibilityLiveRegion="polite"` on
   status; consider announcing fare only on stop to avoid chatter).
2. Give the earnings chart a content description summarizing the 7 values.
3. Test at 200% font scale and fix clipping in the HUD and bottom sheet.
4. Ship `values-ar`/`values-fr` translations (dirs already exist) and move
   `FareEngine` labels to string resources; then do a real RTL pass.
5. Run Accessibility Scanner + TalkBack over all six dialogs and record results here.
