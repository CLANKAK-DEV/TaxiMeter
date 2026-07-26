# TaxiMeter Design System — "Checker"

A high-contrast, driver-first design system built around the app's existing
brand: **classic taxi yellow on near-black**. Every visual decision optimizes
for the two situations a working driver actually faces — direct sunlight on a
dashboard-mounted phone, and long night shifts — while meeting WCAG AA (and
AAA for primary text) in both themes.

All values live in:

- `app/src/main/res/values/colors.xml` (light) and `values-night/colors.xml` (dark)
- `values/dimens.xml` (spacing, touch targets, radii, type scale)
- `values/styles.xml` (text appearances, component styles)
- `values/themes.xml` + `values-night/themes.xml` (Material 3 theme mapping)

---

## 1. Color

Semantic tokens only — components never hardcode hex. Each token resolves
automatically per theme via the resource system.

### Brand

| Token | Light | Dark | Use |
|---|---|---|---|
| `colorPrimary` | `#FFD700` | `#FFD54A` | Brand yellow: accents, summary CTA, destination FAB, dialog headers |
| `colorPrimaryDark` | `#F5B800` | `#E6B800` | Focus/box strokes on yellow surfaces |
| `on_brand` | `#1F1B00` | `#241A00` | Text/icons on yellow (≈15:1) |
| `brand_container` / `on_brand_container` | `#FFF3C4` / `#241A00` | `#4D3E00` / `#FFEFB8` | Tonal chips |
| `colorAccent` | `#0B57D0` | `#8AB4F8` | Informational blue: links, dialog buttons, pickup FAB, route line |

### Neutrals & surfaces

| Token | Light | Dark |
|---|---|---|
| `app_background` | `#F4F5F7` | `#121212` |
| `surface` (cards, sheets, dialogs) | `#FFFFFF` | `#1D1F23` |
| `surface_elevated` | `#FFFFFF` | `#25282D` |
| `on_surface` / `text_primary` | `#191C1E` (16.1:1) | `#F2F3F5` (14.9:1) |
| `on_surface_variant` / `text_secondary` | `#545B62` (6.9:1) | `#A9B0B8` (7.0:1) |
| `text_hint` | `#8A9199` | `#7B828A` |
| `outline` / `divider_color` | `#E1E4E8` | `#34383E` |
| `scrim` | `#99000000` (60%) | `#B3000000` (70%) |

Dark surfaces are **elevated greys, not pure black** (avoids OLED smear and
crushed contrast); pure `#121212` is reserved for the window background.

### Fare readout

| Token | Light | Dark | Rationale |
|---|---|---|---|
| `fare_display` | `#101214` | `#FFD54A` | In daylight, near-black on white is the highest-contrast option (~17:1). At night, the yellow readout evokes a physical meter and pops on dark grey (~11:1) without white glare. |

### Status & actions

| Token | Light | Dark | Meaning |
|---|---|---|---|
| `status_available` | `#1E8E3E` | `#57C97C` | Free / ready |
| `status_active` | `#0B57D0` | `#8AB4F8` | Informational |
| `status_paused` | `#E8710A` | `#FFB74D` | Attention |
| `button_start` / `colorSuccess` | `#1E8E3E` | `#2E9E52` | Start ride |
| `button_stop` / `colorError` | `#D93025` | `#E5473C` / `#FF6B5E` | Stop ride / destructive |
| `colorWarning` | `#E8710A` | `#FFB74D` | Paused state dot |

Color is never the only carrier of state: the status dot is always paired with
a text label ("Available" / "On Trip" / "Paused"), and start/stop is carried by
both button color **and** label.

---

## 2. Typography

System Roboto for UI; **monospace for every live numeral** so digits keep a
fixed width and never jitter as the meter ticks (tabular-figures rule).

| Style | Size / weight | Face | Use |
|---|---|---|---|
| `TextAppearance.TaxiMeter.FareDisplay` | 56sp bold, −2% tracking | monospace | Fare readout |
| `TextAppearance.TaxiMeter.Metric` | 18sp bold | monospace | HUD time / km / speed, history totals |
| `TextAppearance.TaxiMeter.MetricLabel` | 10sp bold, +12% tracking, ALL CAPS | Roboto | Labels above metrics |
| `TextAppearance.TaxiMeter.Title` | 20sp bold | Roboto | Dialog & card titles |
| `TextAppearance.TaxiMeter.Body` | 14sp regular | Roboto | Body copy, helper text |
| `TextAppearance.TaxiMeter.Caption` | 12sp regular | Roboto | Secondary metadata |

Buttons: 14–16sp bold, ALL CAPS, +5% tracking — deliberate for split-second
recognition of START RIDE / STOP RIDE while driving.

---

## 3. Spacing & shape

**4dp rhythm** (`dimens.xml`):

```
space_xs 4 · space_sm 8 · space_md 12 · space_lg 16 · space_xl 20 · space_2xl 24 · space_3xl 32
```

**Radii:** `radius_sm` 8 (info banners) · `radius_md` 16 (HUD card) ·
`radius_lg` 24 (dialogs, summary card) · `radius_sheet` 28 (bottom sheet top) ·
`radius_pill` 32 (action buttons).

**Elevation scale:** HUD card 6dp → FABs 4dp → bottom sheet 16dp → overlay
cards 16dp. No ad-hoc shadow values.

---

## 4. Touch targets

- Minimum interactive size: **48dp** (`touch_target`); primary ride controls
  are **64dp** tall (`button_height_primary`) — sized for gloves, vibration,
  and peripheral-vision tapping.
- FABs use `fabCustomSize=48dp` (the previous "mini" 40dp FABs were below the
  Material minimum).
- List rows ≥ 64dp (`list_item_min_height`); text fields ≥ 48dp.
- ≥ 8dp gap between adjacent targets.

---

## 5. Components

| Component | Style | Notes |
|---|---|---|
| Primary action | `Widget.TaxiMeter.Button` | Filled pill, 64dp, bold caps; green (start) ↔ red (stop) swap in code |
| Secondary action | `Widget.TaxiMeter.Button.Outlined` | 2dp stroke, icon-capable (pause/resume, reset) |
| Tertiary action | `Widget.TaxiMeter.Button.Text` | Low-emphasis (Tariff, Close, Clear all) |
| HUD strip | `simple_card_background` | 16dp-radius surface card with hairline outline, floats over map |
| Bottom sheet | `bottom_card_background` | 28dp top radius, 16dp elevation |
| Dialogs | `ThemeOverlay.TaxiMeter.Dialog` / `.AlertDialog` | Surface-colored; dialog action buttons use **blue** (`colorAccent`), because brand-yellow text fails contrast on light surfaces |
| Status dot | `status_dot` + tint | 10dp oval, always paired with a text label |
| Map hint | `hint_background` | 80%-black pill with white text — readable over any map imagery |
| Rate chip | `rounded_border_yellow` | Tonal yellow chip; text uses `chip_yellow_text` (`#7A5B00` light / `#FFE082` dark) for AA contrast |
| Earnings bars | `bar_earnings` | View-based 7-day bar chart (no chart library): brand-yellow bars (`colorPrimary`, rounded 4dp tops), weekday captions in `text_secondary`; zero-value days render a dimmed 3dp stub so the axis stays readable. Dimens: `earnings_chart_height` 96dp, `earnings_bar_width` 20dp, `earnings_bar_min_height` 3dp |
| Keep-screen-on switch | `SwitchMaterial` | Themed thumb/track (brand yellow when on); label always visible next to the control, state persisted |

Icons: Material vector icons only (no emoji as icons), 24dp grid, tinted via
theme attributes or explicit token tints. Country **flags** remain emoji — they
are content, not UI glyphs.

---

## 6. Dark theme & maps

- The entire palette flips through `values-night/colors.xml`; layouts and
  drawables reference tokens, so no view is theme-hardcoded.
- `Theme.TaxiMeter` (night) flips `windowLightStatusBar` to false; system bars
  follow `status_bar_background` / `surface`.
- Google Maps loads `res/raw/map_style_dark.json` in night mode so the largest
  surface on screen (the map) doesn't blast white light at a night driver.

---

## 7. UX rationale (driver-first)

1. **One glance, one number.** The fare is the single largest element (56sp);
   everything else is subordinate. Monospace prevents digit-width jumping.
2. **State must survive sunlight.** Status is triple-encoded: dot color, label
   text, and the start/stop button's own color + label.
3. **Fat targets, forgiving taps.** 64dp primary controls; destructive Reset is
   outlined in red and spatially separated from Start/Stop.
4. **Night shifts are the norm, not an edge case.** Dedicated dark tokens with
   independently verified contrast (never inverted light values), plus a dark
   map style.
5. **No dead ends.** Every dialog has an explicit close/cancel; destructive
   actions (clear history) require confirmation; tariff overrides are always
   reversible ("Reset to default").
6. **Trust through receipts.** Trip summary + share turns the meter into a
   verifiable receipt for the passenger; the earnings dashboard and CSV
   export give the driver daily/weekly/monthly totals and a portable record
   of every trip.
7. **An active fare is money — protect it.** While the meter runs, Reset and
   the system Back gesture both require confirmation before discarding the
   unsaved fare (stopping a ride normally never prompts), and an optional
   keep-screen-on toggle holds the display awake only during metering so a
   sleeping screen never hides a running meter.
