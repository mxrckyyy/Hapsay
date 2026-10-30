# AGENTS.md — Instructions for AI Coding Agents

**Project:** Campus Helper (native Android, Kotlin, Jetpack Compose)
**Package:** `com.campushelper.app`

This file is the permanent rulebook for OpenCode and any other AI agent working
in this repository. The existing source code is the **source of truth**; the
companion documents explain it:

- `SYSTEM_DESIGN.md` — what the application is and how it is organized conceptually.
- `ARCHITECTURE.md` — how the code is structured technically.
- `PROJECT_CONTEXT.md` — current status, what exists, what is planned.
- `UI_UX_GUIDELINES.md` — the visual and interaction system every screen must follow.

---

## 1. Project rules

1. **Inspect before changing.** Read the relevant source files and the four
   companion documents before writing any code. Never assume how a file looks.
2. **Preserve working features.** If something compiles, builds, and behaves
   correctly, do not rewrite it "while you are in the area".
3. **Follow the existing package structure** documented in `ARCHITECTURE.md`.
   Do not invent new top-level packages, do not relocate `MainActivity.kt`,
   and do not move the whole project into a different namespace.
4. **Reuse before creating.** Check `presentation/components/`
   (`CampusTopBar`, `CampusSectionHeader`, `CampusCard`, `CampusPrimaryButton`,
   `CampusSecondaryButton`, `CampusEmptyState`, `CampusBottomNavigation`,
   `CampusIconContainer`) and the theme tokens before writing new UI code.
   Only introduce a new component when no existing one fits.
5. **Keep responsibilities separated.** Screens render state; ViewModels own
   state and logic; repositories own data access. Composables never touch
   DAOs, repositories, or the network directly.
6. **Avoid unnecessary dependencies.** Every new dependency must be justified.
   The version catalog (`gradle/libs.versions.toml`) is the only place versions
   are declared.
7. **Do not upgrade tooling without a requirement.** Gradle 9.6.0, AGP 9.5.0
   (built-in Kotlin), Kotlin 2.2.10, KSP 2.3.12, and the Compose BOM
   2026.02.01 are pinned and verified to work together (known compatibility
   constraints are listed in `ARCHITECTURE.md`). Do not bump versions "just to
   be current".
8. **Never replace working implementations with rewrites.** Small, surgical
   changes win. If a refactor is truly required, explain why first.
9. **Never fabricate application data.** If no data source exists yet, render
   an honest empty/setup state (see `UI_UX_GUIDELINES.md`). Never invent
   notes, expenses, events, class times, weather conditions, or productivity
   metrics.
10. **Never claim a feature is tested unless it was actually tested.** Report
    real build output and real verification steps. "It should work" is not a
    test result.
11. **Phase discipline.** Only implement what the current phase asks for.
    UI/design phases do not implement database features; database phases do
    not redesign UI.

## 2. Kotlin and Compose rules

- Follow standard Kotlin naming conventions (camelCase for functions and
  properties, PascalCase for classes and composables, SCREAMING_SNAKE_CASE for
  constants).
- Use descriptive names. `uiState`, `onOpenSchedule`, `HomeUiState` — not
  abbreviations or single letters.
- Keep composables focused: one screen section per composable, extract
  repeated pieces, keep them reasonably small.
- Business logic lives in ViewModels (or future domain `usecase/` classes) —
  never in composables.
- ViewModels expose immutable screen state as `StateFlow` (see
  `presentation/home/HomeViewModel.kt` for the reference pattern).
- Collect state in screens with `collectAsStateWithLifecycle()`.
- Use `MaterialTheme.colorScheme`, `MaterialTheme.typography`,
  `MaterialTheme.shapes`, and the tokens in `ui/theme` — no hardcoded colors,
  sp, dp, or corner radii in screens. The only sanctioned dp values are through
  `ui/theme/Spacing.kt` and shape tokens.
- Avoid unnecessary recomposition (prefer lambda taps, stable state,
  deferred reads in `graphicsLayer` blocks).
- Add a `@Preview` for reusable components where practical. Screens rely on
  ViewModels; give them manual state so previews stay possible.
- Comment code for someone learning: comments explain *why* and *how to
  extend*, not what is obviously visible.

## 3. Development workflow

### Before implementation

1. Inspect the relevant files.
2. Read the linked companion documents.
3. Identify affected packages and dependencies.
4. Explain the planned changes (files to touch, expected behavior) before
   editing.

### After implementation

1. Build: `gradlew.bat assembleDebug`
2. Test: `gradlew.bat testDebugUnitTest`
3. Read the build output — fix errors caused by your changes; report warnings
   separately from errors.
4. When an emulator is online, install and launch
   (`adb install -r app\build\outputs\apk\debug\app-debug.apk`, then start
   `com.campushelper.app/.MainActivity`), verify navigation and rendering.
   Report the emulator ID used.
5. Report: changed files, actual build/test results, actual manual
   verification (or "not verified on device").

## 4. Restrictions (hard rules)

- **No Hilt or any other DI framework.** The manual `core/di/ServiceLocator`
  is the injection story for this project. Extend it; do not bypass it.
- **No duplicate app scaffolding.** Exactly one `Scaffold`
  (`presentation/CampusHelperApp.kt`), one `NavHost`
  (`presentation/navigation/CampusHelperNavHost.kt`), one bottom bar
  (`CampusBottomNavigation`). Do not add a second one anywhere.
- **No fake weather.** The Cebu weather card explicitly states that the
  integration is not implemented. Do not display temperatures, icons of
  "sunny weather", or forecasts until a real data source exists.
- **No fake productivity metrics.** Numbers on the dashboard must be computed
  from real data or not shown.
- **No premature database expansion.** The seeded `NoteEntity`/`NoteDao`
  exist to verify the Room + KSP pipeline. Real entities/DAOs belong to their
  feature phases.
- **Do not delete tests.** The example tests are placeholders but they are
  the only automated coverage — extend rather than remove.
- **Do not touch the Gradle wrapper, the daemon JVM toolchain file
  (`gradle/gradle-daemon-jvm.properties`), or build settings** unless a build
  failure requires it.

## 5. Quick reference

| What | Where |
|---|---|
| Theme tokens | `app/src/main/java/com/campushelper/app/ui/theme/` |
| Reusable components | `app/src/main/java/com/campushelper/app/presentation/components/` |
| Navigation graph | `presentation/navigation/` |
| Manual DI | `core/di/ServiceLocator.kt` |
| Room database | `core/database/AppDatabase.kt`, `data/entity/`, `data/dao/` |
| Build outputs | `app/build/outputs/apk/debug/` |
| Status of every feature | `PROJECT_CONTEXT.md` |
