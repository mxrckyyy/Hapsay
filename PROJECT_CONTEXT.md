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
(KSP 2.3.12, no kapt; schema JSON exported to `app/schemas/`) · Coroutines +
StateFlow · Retrofit 3.0.0 + kotlinx-serialization (declared, unused so far) ·
manual `ServiceLocator` (no DI framework).

Exact versions and compatibility constraints: `ARCHITECTURE.md §1`.

## 3. Project structure (summary)

```
com.campushelper.app
├── MainActivity.kt               # entry point only
├── core/      database(v2 + MIGRATION_1_2) | di | network(EMPTY) | util(EMPTY)
├── data/      entity(NoteEntity) | dao(NoteDao) | repository(NotesRepository + Impl)
├── domain/    model(Note) | usecase(EMPTY)
├── presentation/  app root + navigation + components + home + placeholder features + notes ViewModel + weather(EMPTY)
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
| Notes data layer | **IMPLEMENTED** (Phase 3.1) | Schema v2 `NoteEntity`, full `NoteDao`, `NotesRepository`(+Impl), domain `Note`, migration 1→2, schema export; instrumented-tested |
| Notes UI (list/editor) | **PLANNED** | Placeholder screen remains; no ViewModel consumes the repository yet |
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

- Room `AppDatabase` **v2**, `exportSchema = true` (KSP arg
  `room.schemaLocation` → `app/schemas/com.campushelper.app.core.database.AppDatabase/2.json`,
  committed), DB name `campus-helper.db`.
- `NoteEntity` (IMPLEMENTED, Phase 3.1): columns `id`, `title`, `content`,
  `created_at`, `updated_at` (epoch millis). Saved text is stored verbatim —
  never trimmed or rewritten.
- `NoteDao` (IMPLEMENTED): `observeAll` / `observeById` / `search`
  (parameterized LIKE on title OR content) / `insert` / `update` /
  `deleteById`; deterministic order `updated_at DESC, id DESC`.
- `NotesRepository` + `NotesRepositoryImpl` (IMPLEMENTED): maps entity ↔
  domain `Note`, exactly one `System.currentTimeMillis()` read per write
  (`createdAt == updatedAt` on create; `createdAt` preserved and `updatedAt`
  re-stamped on update), trims search input (blank = all notes). Provided by
  `ServiceLocator.provideNotesRepository`.
- Migration 1→2 (`AppDatabase.MIGRATION_1_2`, registered via
  `addMigrations` in `ServiceLocator`): recreate-table pattern (API 26-safe)
  — the seed's `body` became `content`, `created_at` backfilled from
  `updated_at`. No destructive fallback.
- Limitations: no consumer yet (Notes UI PLANNED — a user cannot reach the
  database); no schema v1 JSON exists, so the 1→2 migration has no automated
  regression test and the runtime old-version upgrade is unverified.
- Planned entities: Expense, Event, Course. Protocol in `ARCHITECTURE.md §7`.

## 9. Current development phase

**Phase 3.1 — Notes data foundation** (data layer behind the unchanged Notes
placeholder screen). Completed phases:

| Phase | Content | Result |
|---|---|---|
| 0 — Setup | Package rename to `com.campushelper.app`, architecture skeleton, Gradle/KSP/Room wiring | Build verified; app launched on emulator-5556 |
| 1 — UI system + Home | Dark design system, 8 components, dashboard, honest empty states | Build + tests + emulator verification (screenshots, taps, scroll) |
| 2 — Documentation | This document set | Build/tests re-verified (see phase report) |
| 3.1 — Notes data foundation | `NoteEntity` v2, `NoteDao`, `NotesRepository`(+Impl), domain `Note`, `MIGRATION_1_2`, KSP schema export | Build + unit tests green; **17 instrumented tests green** on emulator-5558 (records 0 failures) |

## 10. Build and test commands

```bat
gradlew.bat assembleDebug          :: debug APK -> app\build\outputs\apk\debug\
gradlew.bat testDebugUnitTest      :: local unit tests
gradlew.bat assembleDebugAndroidTest   :: compiles instrumented tests
gradlew.bat connectedDebugAndroidTest  :: runs them on a connected emulator/device
```

Optional device verification (when an emulator is online, e.g. `emulator-5556`):

```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.campushelper.app/.MainActivity
```

Note: `emulator-5554` historically shows as `offline` (ghost process); use a
healthy `device`-state instance and report the device ID used. Phase 3.1 used
a headless `emulator-5558` (`-no-window -gpu swiftshader_indirect`); system
rendering may briefly lag after boot there — the app itself launches after a
few seconds.

## 11. Known limitations

1. **Dark-only theme** this phase; light theme planned.
2. **Notes data is not user-reachable yet** — the notes table (schema v2) and
   its repository are implemented and tested, but no Notes UI/ViewModel
   consumes them; the tab still shows the placeholder, so the table stays
   empty in real use. Schedule/events/expenses remain un-stored.
3. **No network layer usage** — Retrofit dependencies declared but no service/repository exists; the weather feature is intentionally placeholder.
4. **Placeholder screens** for Notes/Expenses/Calendar/Schedule (shared `PlaceholderScreen`).
5. **Minimal automated testing** — one template unit test; instrumented testing exists for the Notes DAO/repository only.
6. `material-icons-extended` inflates debug APK size (~18–19 MB); release R8 strips unused icons.
7. Empty package directories are untracked by any future Git init until populated.
8. Muted text color (~3.2:1) is limited to decorative/small text by guideline.
9. **Room v1→v2 migration has no automated test and is runtime-unverified** —
   no schema v1 JSON was ever exported (the seed phase ran with
   `exportSchema = false`), so `MigrationTestHelper` cannot recreate the v1
   state, and no pre-Phase-3.1 install was upgraded on a device. The
   migration SQL is matched byte-for-byte against the KSP-generated v2
   schema contract instead; from v2 onward every schema version is exported,
   so future migrations can be regression-tested properly.

## 12. Development roadmap

1. **Notes UI** — Notes list/editor screen + `NotesViewModel`, the first
   consumer of the Phase 3.1 data foundation
   (`ServiceLocator.provideNotesRepository`), styled with the design system.
2. **Expenses feature** — Expense entity/DAO/repository + summary/month totals.
3. **Calendar feature** — Event storage + monthly UI + reminders (WorkManager; not yet added).
4. **Schedule feature** — Course/weekly timetable storage + weekly grid UI.
5. **Dashboard aggregation** — switch Home sections to real data (schedule today, upcoming events, notes count, monthly expenses).
6. **Cebu weather** — Retrofit service in `core/network/` + repository + loading/error states replacing the "NOT AVAILABLE YET" card.
7. **Theming & polish** — light theme, final branding assets, saving/FAB conventions.
8. **Quality** — unit + instrumented tests per feature, lint baseline cleanup.
