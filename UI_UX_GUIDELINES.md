# UI_UX_GUIDELINES.md — Campus Helper Design System

> Describes the design system **as implemented** in `ui/theme/` and
> `presentation/components/` (verified against source). Every new screen must
> follow this document. Companion documents: `AGENTS.md`, `ARCHITECTURE.md`,
> `PROJECT_CONTEXT.md`, `SYSTEM_DESIGN.md`.

## 1. Design direction

**Modern minimalist, dark-first student productivity app.**

- Quiet dark green-tinted neutrals for structure; ONE green accent family
  (`#22C55E` / `#16A34A`) for identity and meaning.
- Flat design: surfaces imply hierarchy through contrast — no shadows, glows,
  or gradients. (`CampusCard` and buttons ship with zero elevation.)
- Restrained corner rounding (shape scale below), restrained typography
  (nothing larger than 26sp), restrained spacing (fixed scale only).
- Advantage comes from information design, not decoration.

Phase status: **dark theme only** (Phase 1 decision — dynamic color is off;
do not "restore" it; see `ui/theme/Theme.kt`).

## 2. Color palette (source: `ui/theme/Color.kt`, `ui/theme/Theme.kt`)

| Token (Compose val) | Hex | Role |
|---|---|---|
| `DarkBackground` | `#0B0F0E` | App/window background (`background`) |
| `DarkSurface` | `#121816` | Primary surface: cards, bottom bar (`surface`, `surfaceContainer`/`Low`) |
| `DarkSurfaceSecondary` | `#19211D` | Secondary surface tier (`surfaceVariant`, `surfaceContainerHigh`) |
| `DarkSurfaceElevated` | `#202923` | Elevated tier: icon containers, indicators (`surfaceContainerHighest`) |
| `GreenPrimary` | `#22C55E` | Primary accent: selected states, primary buttons, quick-access icons (`primary`) |
| `GreenSecondary` | `#16A34A` | Secondary accent tier (`secondary`, `tertiary`) |
| `GreenSoft` | `#86EFAC` | Text-on-green-container (`onPrimaryContainer`) |
| `GreenDeep` | `#052E16` | Text/dark accent on filled green (`onPrimary`) — ~5.9:1 |
| `GreenContainer` | `#1B4332` | Green-tinted containers, bottom nav indicator |
| `TextPrimary` | `#F3F7F4` | Headings and body text (`onSurface`, `onBackground`) |
| `TextSecondary` | `#A1ADA5` | Supporting/descriptive text (`onSurfaceVariant`) |
| `TextMuted` | `#6F7C73` | Minor metadata only — limited to non-essential text (contrast ≈3.2:1) |
| `DarkDivider` | `#28332C` | Outlines/borders (`outline`, `outlineVariant`) |
| `ErrorRed` | `#EF4444` | Error (`error`) |
| `WarningAmber` | `#F59E0B` | Warning — **defined but not yet used in any UI**; do not invent uses casually |

**Usage rules**
- Screens reference `MaterialTheme.colorScheme.*` — never raw hex (raw values
  are defined once in `Color.kt` and plumbed into the scheme in `Theme.kt`).
- Green = interactive/selected/primary meaning. Backgrounds of *inactive*
  content stay neutral. Do not fill large areas green.
- `TextPrimary`/`TextSecondary` for reading; `TextMuted` only for tiny,
  non-essential metadata.

## 3. Typography (source: `ui/theme/Type.kt` — `CampusTypography`)

System sans-serif family. Nine slots, defined once:

| Slot | Size/Weight | Use for |
|---|---|---|
| `headlineMedium` | 26sp SemiBold / 32lh | Dashboard heading, screen titles |
| `titleLarge` | 20sp SemiBold / 28lh | Larger content headings (reserved - defined, not used in the current UI yet) |
| `titleMedium` | 16sp SemiBold / 24lh | Section headings, empty-state titles |
| `bodyLarge` | 16sp Normal / 24lh | Primary reading text |
| `bodyMedium` | 15sp Normal / 22lh | Supporting descriptions, date line |
| `bodySmall` | 13sp Normal / 18lh | Secondary descriptions |
| `labelLarge` | 15sp Medium / 20lh | Button text |
| `labelMedium` | 12sp Medium / 16lh | Quick-access labels, general labels |
| `labelSmall` | 11sp Medium / 16lh | Small captions / status labels |

Reference the *slot*, never the size: `MaterialTheme.typography.titleMedium`.
No custom font resources are bundled.

## 4. Spacing (source: `ui/theme/Spacing.kt`)

| Token | dp | Typical use |
|---|---|---|
| `extraSmall` | 4 | Icon→micro-label gaps |
| `small` | 8 | List internals, quick-access tile inner padding, header→date gap |
| `smallMedium` | 12 | Content padding inside cards, tile label gaps, column gaps |
| `medium` | 16 | Standard component gap; **screen horizontal padding** (`screenPadding`) |
| `mediumLarge` | 20 | Pre-CTA spacing, container sizes (2× = 40dp icon container) |
| `large` | 24 | Dashboard section separation |
| `extraLarge` | 32 | Bottom breathing room before the nav bar |

Rule: if a layout needs a value outside this scale, extend `Spacing.kt`
deliberately (documented decision) — never inline arbitrary dp.

## 5. Shapes (source: `ui/theme/Theme.kt` — `CampusShapes`)

| Material slot | Radius | Applied to |
|---|---|---|
| `extraSmall` | 4dp | Reserved for tiny accents |
| `small` | 8dp | Small components (`CampusIconContainer`) |
| `medium` | 12dp | **Standard cards and all buttons** |
| `large` | 16dp | Reserved for larger dashboard sections |

Not every element may be "extra rounded". Cards use `medium`; use `large`
only where a section is meant to read as a big grouped panel.

## 6. Components (`presentation/components/`)

All are the single source of their patterns. Check here FIRST (per
`AGENTS.md`) before creating anything new.

| Component | Signature (essentials) | Reuse when | Behavior notes |
|---|---|---|---|
| `CampusTopBar` | `(title, subtitle, logo)` | Every feature screen header | Flat header row: 40dp logo container + greeting block. **No action slots** — add one only when a real destination exists |
| `CampusSectionHeader` | `(title, actionLabel?, onActionClick?)` | Section titles on dashboard | Action renders only when both label AND handler exist; enforces 48dp touch height |
| `CampusCard` | `(contentPadding, content)` | Every grouped content block | Flat Card: `surfaceContainerLow`, 12dp shape, 0 elevation, uniform padding |
| `CampusPrimaryButton` | `(text, onClick, enabled)` | THE one main action per view | Solid green/`onPrimary` text, 12dp, ≥48dp, no shadow |
| `CampusSecondaryButton` | `(text, onClick, enabled)` | Supporting actions (empty-state "Open Calendar") | Outlined, `onSurface` text, divider border |
| `CampusEmptyState` | `(icon, title, subtitle, actionLabel?, onActionClick?)` | Sections with no data yet | Muted decorative icon, centered text, optional real action; always fills its parent width so the centered composition is guaranteed inside any card |
| `CampusBottomNavigation` | `(navController)` | The app bottom bar (only) | 5 `topLevelRoutes`, flat `surfaceContainer`, green selection, `secondaryContainer` indicator pill |
| `CampusIconContainer` | `(icon, size=40dp, containerColor, iconTint, contentDescription)` | Any single-icon badge | 8dp-rounded elevated box; default: elevated surface + green icon |

`PlaceholderScreen` (`presentation/components/PlaceholderScreen.kt`) is the
shared not-implemented screen used by Notes/Expenses/Calendar/Schedule tabs.

## 7. Layout rules

1. **Horizontal rhythm:** screens pad horizontally with `Spacing.medium`
   (16dp) — one value across all screens; no mixed side paddings.
2. **Vertical rhythm:** dashboard sections separate with `Spacing.large`
   (24dp); heading→content gap is `Spacing.smallMedium` (12dp).
3. **Visual hierarchy through color/typography only** — avoid borders and
   shadows; a `Divider` color exists but borders are discouraged unless a
   control needs an outline (e.g. `CampusSecondaryButton`).
4. **Bottom navigation overlap:** content must end above the bar — screens
   rely on the parent Scaffold's `innerPadding` (already applied in
   `CampusHelperApp.kt`); add `Spacing.extraLarge` end-spacer inside
   scrollable dashboards for breathing room.
5. **Scrollability:** any screen that may exceed one viewport uses
   `verticalScroll(rememberScrollState())` — never a second Scaffold.
6. **Touch targets ≥ 48dp:** rely on M3 minimum-interactive targeting
   (`NavigationBarItem`, buttons) and on custom controls use full-cell
   clickable containers (quick-access) or `heightIn(min = 48.dp)`.
7. **System insets:** edge-to-edge is enabled; Scaffold insets are consumed
   at the root — screens never pad for status bars themselves.
8. **Clean density:** one idea per card. If a card grows beyond ~2 content
   rows + empty state, it is a new section, not more card content.

## 8. Interaction rules

1. **Every visible control acts** — real navigation or real state change. No
   decorative buttons; empty states only show actions wired to actual
   destinations.
2. **Empty states teach + aim:** title (what's missing) + supporting line
   (why/what when ready) + one secondary action pointing to the destination
   where the data will be created.
3. **Predictable navigation:** bottom-bar discipline (`saveState`/`restoreState`,
   no duplicate destinations on repeat taps) — implemented in
   `NavigationExtensions.navigateToTopLevel`; reuse it, do not write custom
   navigation code per screen.
4. **Feedback:** ripples are standard (`clip` rounded containers so ripples
   stay inside shape); selection states (bottom bar, quick access) use the
   green accent on `secondaryContainer`/elevated surfaces.
5. **State coverage for data-driven UI (when data exists):** every section
   must define its **loading**, **error**, **empty**, and **success**
   presentation beforehand — empty/loading/error states use
   `CampusEmptyState` with accurate text; errors render with the `error`
   color token (`ErrorRed`), never as fake success.
6. **Motion:** one subtle entrance (≤250 ms fade + 12dp rise) on dashboards
   is allowed (`HomeScreen` implements the pattern); no looping/background
   animations; defer fancy transitions to a dedicated phase.
7. **Accessibility:** visible text keeps ≥ 4.5:1 contrast (per palette
   assignments); standalone icons get meaningful `contentDescription`, icons
   beside labels pass `null`; touch targets ≥ 48dp; tab order equals visual
   order (default Column/Row semantics).

## 9. Consistency checklist for every new screen

- [ ] Uses `CampusXxx` components where applicable — no ad-hoc cards/buttons
- [ ] All colors from `MaterialTheme.colorScheme` (no hex in screen code)
- [ ] All typography via `MaterialTheme.typography.<slot>`
- [ ] All spacing/shape values from `Spacing.kt` / `shapes`
- [ ] State collected with `collectAsStateWithLifecycle()` from a ViewModel `StateFlow`
- [ ] Real behavior behind every control; honest states for missing data
- [ ] ≥48dp targets; scrollable when long; respects root Scaffold insets
- [ ] Follows the dark minimalist language — nothing that looks like a second design system
