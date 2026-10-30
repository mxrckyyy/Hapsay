# PROJECT_CONTEXT.md — Project Knowledge Base

> Status snapshot for future development sessions. Written 2026-10-30 after
> Phase 1 (UI design system + Home dashboard) and Phase 2 (documentation).
> Companion documents: `AGENTS.md`, `SYSTEM_DESIGN.md`, `ARCHITECTURE.md`,
> `UI_UX_GUIDELINES.md`.

## 1. Identity

| | |
|---|---|
| **Project name** | Campus Helper |
| **Package / applicationId / namespace** | `com.campushelper.app` |
| **Module** | `:app` (single module) |
| **Platform** | Native Android |
| **Purpose** | Student productivity: dashboard, notes, expenses, calendar of academic events, weekly class schedule, Cebu weather |
| **Target users** | University students (Cebu, Philippines — weather feature assumes Cebu) |

**Main problems solved:** scattered student workflows — one place for
today's classes, notes, expense visibility, academic deadlines, and local
weather, without juggling separate apps or paper notes.

## 2. Technology stack

Native Android · Kotlin · Jetpack Compose + Material 3 · MVVM + light Clean
Architecture (single module) · Navigation Compose (type-safe) · Room 2.8.5
(KSP 2.3.12, no kapt) · Coroutines + StateFlow · Retrofit 3.0.0 +
kotlinx-serialization (declared, unused so far) · manual `ServiceLocator`
(no DI framework).

Exact versions and compatibility constraints: `ARCHITECTURE.md §1`.

## 3. Project structure (summary)

```
com.campushelper.app
├── MainActivity.kt               # entry point only
├── core/      database | di | network(EMPTY) | util(EMPTY)
├── data/      entity(NoteEntity) | dao(NoteDao) | repository(EMPTY)
├── domain/    model(EMPTY) | usecase(EMPTY)
├── presentation/  app root + navigation + components + home + 4 placeholder features + weather(EMPTY)
└── ui/theme/  Color | Theme | Type | Spacing
```

Detailed tree with per-file notes: `ARCHITECTURE.md §2`.

## 4. Feature status

| Feature | Status | Notes |
|---|---|---|
| Dark design system (tokens, theme) | **IMPLEMENTED** | Phase 1; dark-only for now |
| Reusable Campus components (8) | **IMPLEMENTED** | `presentation/components/` |
| Type-safe navigation, 5 destinations | **IMPLEMENTED** | Home/Notes/Expenses/Calendar/Schedule |
| Bottom navigation | **IMPLEMENTED** | `CampusBottomNavigation`, single bar |
| Home dashboard | **IMPLEMENTED** | Header, real date, quick access, 4 honest result sections |
| Schedule data (courses) | **PLANNED** | No Course storage yet; Home shows empty state |
| Calendar / events data | **PLANNED** | No Event storage yet; Home shows empty state |
| Notes CRUD | **PLANNED** | Seed `NoteEntity`/`NoteDao` exist for toolchain verification only |
| Expenses tracking | **PLANNED** | Nothing beyond placeholder screen |
| Reminders/notifications for events | **PLANNED** | No WorkManager/notification code |
| Cebu weather integration | **NOT AVAILABLE** | No API/network code; card explicitly says "Not available yet" |
| Productivity metrics on Home | **NOT AVAILABLE** | No data sources; honest setup state only — no fabricated numbers |
| Light theme | **PLANNED** | Phase 1 is dark-only by decision |
| User profile/settings | **PLANNED** | No destination exists; header intentionally has no profile/settings action yet |

## 5. Design direction

Modern minimalist **dark theme** with restrained green brand identity
(`#22C55E`). Flat surfaces (no shadows/gradients), 12dp cards, token-driven
spacing/typography. Full specification: `UI_UX_GUIDELINES.md`.

## 6. Navigation destinations

| Destination | Route | Screen |
|---|---|---|
| Home | `HomeRoute` | `presentation/home/HomeScreen.kt` (implemented) |
| Notes | `NotesRoute` | `presentation/notes/NotesScreen.kt` (placeholder) |
| Expenses | `ExpensesRoute` | `presentation/expenses/ExpensesScreen.kt` (placeholder) |
| Calendar | `CalendarRoute` | `presentation/calendar/CalendarScreen.kt` (placeholder) |
| Schedule | `ScheduleRoute` | `presentation/schedule/ScheduleScreen.kt` (placeholder) |

Header actions navigate cross-destination: Home empty-state buttons open
Schedule and Calendar; quick-access opens all four feature destinations.

## 7. Reusable components

`CampusTopBar` · `CampusSectionHeader` · `CampusCard` · `CampusPrimaryButton`
· `CampusSecondaryButton` · `CampusEmptyState` · `CampusBottomNavigation` ·
`CampusIconContainer` — usage rules in `UI_UX_GUIDELINES.md §Components`.

## 8. Database status

- Room `AppDatabase` v1, `exportSchema = false`, DB name `campus-helper.db`.
- Seed `NoteEntity` (id, title, body, updatedAt epoch millis) + `NoteDao`
  (observe/insert/update/deleteById) — exist solely to verify the Room + KSP
  pipeline; **not consumed by any UI yet**.
- Planned entities: Expense, Event, Course (+ eventual schema export +
  migrations). Protocol in `ARCHITECTURE.md §7`.

## 9. Current development phase

**Phase 2 — Project documentation and engineering standards** (this file).
Completed phases:

| Phase | Content | Result |
|---|---|---|
| 0 — Setup | Package rename to `com.campushelper.app`, architecture skeleton, Gradle/KSP/Room wiring | Build verified; app launched on emulator-5556 |
| 1 — UI system + Home | Dark design system, 8 components, dashboard, honest empty states | Build + tests + emulator verification (screenshots, taps, scroll) |
| **2 — Documentation** | **This document set** | Build/tests re-verified (see phase report) |

## 10. Build and test commands

```bat
gradlew.bat assembleDebug          :: debug APK -> app\build\outputs\apk\debug\
gradlew.bat testDebugUnitTest      :: local unit tests
gradlew.bat assembleDebugAndroidTest   :: compiles instrumented tests
```

Optional device verification (when an emulator is online, e.g. `emulator-5556`):

```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.campushelper.app/.MainActivity
```

Note: `emulator-5554` historically shows as `offline` (ghost process); use a
healthy `device`-state instance and report the device ID used.

## 11. Known limitations

1. **Dark-only theme** this phase; light theme planned.
2. **No persisted user data yet** — notes/schedule/events/expenses are not stored; the DB pipeline is seeded but unused.
3. **No network layer usage** — Retrofit dependencies declared but no service/repository exists; the weather feature is intentionally placeholder.
4. **Placeholder screens** for Notes/Expenses/Calendar/Schedule (shared `PlaceholderScreen`).
5. **Minimal automated testing** — one template unit test; no instrumented UI tests.
6. `material-icons-extended` inflates debug APK size (~18–19 MB); release R8 strips unused icons.
7. Empty package directories are untracked by any future Git init until populated.
8. Muted text color (~3.2:1) is limited to decorative/small text by guideline.

## 12. Development roadmap

1. **Notes feature** — finalize Note model → repository (`data/repository/`) → domain model → Notes UI (list/editor) consuming the design system.
2. **Expenses feature** — Expense entity/DAO/repository + summary/month totals.
3. **Calendar feature** — Event storage + monthly UI + reminders (WorkManager; not yet added).
4. **Schedule feature** — Course/weekly timetable storage + weekly grid UI.
5. **Dashboard aggregation** — switch Home sections to real data (schedule today, upcoming events, notes count, monthly expenses).
6. **Cebu weather** — Retrofit service in `core/network/` + repository + loading/error states replacing the "NOT AVAILABLE YET" card.
7. **Theming & polish** — light theme, final branding assets, saving/FAB conventions.
8. **Quality** — unit + instrumented tests per feature, lint baseline cleanup.
