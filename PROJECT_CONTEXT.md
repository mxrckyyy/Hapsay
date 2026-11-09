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
├── core/      database(v3 + MIGRATION_1_2 + MIGRATION_2_3) | di | network(EMPTY) | util(EMPTY)
├── data/      entity(Note, Expense) | dao(Note, Expense) | repository(Notes, Expenses)
├── domain/    model(Note, Expense, ExpenseCategory) | usecase(EMPTY)
├── presentation/  app root + navigation + components + home + notes (screens + ViewModels) + expenses (ViewModel) + weather(EMPTY)
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
| Notes ViewModel | **IMPLEMENTED** (Phase 3.2) | `NotesViewModel` + `NotesUiState` (debounced reactive search, CRUD intents, loading/error/empty states); 12 unit tests against a fake repository |
| Expenses ViewModel | **IMPLEMENTED** (Phase 4.2) | `ExpensesViewModel` + `ExpensesUiState` (period/category filters, today/month totals, category breakdown, loading/error/empty); 21 unit tests against a fake repository; date source injectable via `Clock` |
| Expenses UI (overview + editor) | **PLANNED** | Placeholder screen remains; no screen collects the ViewModel yet |
| Notes list screen | **IMPLEMENTED** (Phase 3.3) | Real list (LazyColumn keyed by id), debounced search, loading/error/empty/no-results states, green FAB; 5 Compose UI tests green on emulator-5556 |
| Note editor (create/edit) | **IMPLEMENTED** (Phase 3.4) | `NoteEditorScreen` + `NoteEditorViewModel`: create/edit, validation, unsaved-changes guard (BackHandler + dialog), save error handling; 11 unit tests + 5 Compose UI tests green |
| User-facing create/edit | **IMPLEMENTED** (Phase 3.4) | FAB opens editor; tap note opens editor; save persists via repository and returns to list |
| User-facing deletion | **PLANNED** | No deletion UI (Phase 3.5) |
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

- Room `AppDatabase` **v3**, `exportSchema = true` (KSP arg
  `room.schemaLocation` → `app/schemas/.../2.json` and `.../3.json`,
  committed), DB name `campus-helper.db`.
- `NoteEntity` (IMPLEMENTED, v2): columns `id`, `title`, `content`,
  `created_at`, `updated_at` (epoch millis).
- `ExpenseEntity` (IMPLEMENTED, v3): columns `id`, `title`, `amount_minor`
  (centavos, Long), `category` (stable id), `expense_date_epoch_day`
  (epoch days), `note`, `created_at`, `updated_at`.
- `NoteDao` (IMPLEMENTED): observeAll / observeById / search / insert / update /
  deleteById; order `updated_at DESC, id DESC`.
- `ExpenseDao` (IMPLEMENTED): observeAll / observeById / observeByCategory /
  observeByDateRange / observeFiltered / insert / update / deleteById; order
  `expense_date_epoch_day DESC, id DESC`.
- `NotesRepository` + Impl (IMPLEMENTED): verbatim text, one clock read per
  write, trims search input. Provided by `ServiceLocator.provideNotesRepository`.
- `ExpensesRepository` + Impl (IMPLEMENTED, Phase 4.1): verbatim text, one
  clock read per write, category/date filtering, category fallback to OTHER.
  Provided by `ServiceLocator.provideExpensesRepository`.
- Migration 1→2 (`MIGRATION_1_2`): recreate-table pattern (API 26-safe).
- Migration 2→3 (`MIGRATION_2_3`): additive, creates `expenses` table +
  two indexes; verified by `Migration_2_3_Test` (notes preserved, expenses
  accepts/returns rows, indexes present). No destructive fallback.
- Limitations: create, edit, and delete (Notes) are implemented (Phases 3.4/3.5).
  No schema v1 JSON exists, so the 1→2 migration has no automated regression
  test and the runtime old-version upgrade is unverified. The 2→3 migration
  IS verified by `Migration_2_3_Test`.
- Planned entities: Event, Course. Protocol in `ARCHITECTURE.md §7`.

## 9. Current development phase

**Phase 4.2 — Expenses ViewModel** (complete; awaiting review before 4.3).
Completed phases:

| Phase | Content | Result |
|---|---|---|
| 0 — Setup | Package rename to `com.campushelper.app`, architecture skeleton, Gradle/KSP/Room wiring | Build verified; app launched on emulator-5556 |
| 1 — UI system + Home | Dark design system, 8 components, dashboard, honest empty states | Build + tests + emulator verification (screenshots, taps, scroll) |
| 2 — Documentation | This document set | Build/tests re-verified (see phase report) |
| 3.1 — Notes data foundation | `NoteEntity` v2, `NoteDao`, `NotesRepository`(+Impl), domain `Note`, `MIGRATION_1_2`, KSP schema export | Build + unit tests green; **17 instrumented tests green** on emulator-5558 (records 0 failures) |
| 3.2 — Notes ViewModel | `NotesViewModel` + `NotesUiState` (debounced reactive search, CRUD intents, loading/error/empty states), fake-repository unit tests | Build green; **13 unit tests green** (1 template + 12 NotesViewModel) |
| 3.3 — Notes screen | Real list, debounced search, loading/error/empty/no-results states, green FAB; Compose UI tests | Build green; **22 instrumented tests green** on emulator-5556 (5 new NotesScreen) |
| 3.4 — Note editor | `NoteEditorScreen` + `NoteEditorViewModel` (create/edit, validation, unsaved-changes guard, save error handling); navigation route `NoteEditorRoute(noteId)` | Build green; **24 unit tests green** (1 + 11 + 12) and **27 instrumented tests green** on emulator-5556 (5 new NoteEditorScreen) |
| 3.5 — Delete notes | Delete action + confirmation dialog in `NoteEditorScreen`/`NoteEditorViewModel`; permanent deletion via existing repository | Build green; **31 unit tests green** (1 + 12 + 18) and **33 instrumented tests green** on emulator-5556 (6 new delete tests) |
| 3.6 — Notes final audit | Data integrity, ViewModel, UI, navigation, test, and documentation audit; minor cleanup fixes | Audit complete; no critical/high issues; 24 unit + 33 instrumented tests green |
| 4.1 — Expenses data foundation | `Expense`/`ExpenseCategory` domain, `ExpenseEntity`, `ExpenseDao`, `ExpensesRepository`(+Impl), migration 2→3, ServiceLocator provider, schema export, domain/DAO/repository/migration tests | Build green; **8 unit tests green** (parse + category) and **25 instrumented tests green** (10 DAO + 6 repo + 1 migration) on emulator-5556 |
| 4.2 — Expenses ViewModel | `ExpensesViewModel` + `ExpensesUiState` (period/category filters, today/month totals, category breakdown, loading/error/empty); `ExpensePeriodFilter`, `CategoryTotal`; injectable `Clock`; 21 unit tests | Build green; **59 unit tests green** (1 + 3 + 4 + 18 + 12 + 21) |

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
2. **User-facing deletion still unimplemented** — notes can now be created and
   edited end to end, but there is no delete UI (Phase 3.5). Schedule/events/
   expenses remain un-stored.
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

1. **Notes deletion** — delete UI (Phase 3.5) wiring into the existing
   `NotesViewModel.deleteNote` API.
2. **Expenses feature** — data foundation (4.1) and ViewModel (4.2) implemented; overview/editor UI pending (4.3+).
3. **Calendar feature** — Event storage + monthly UI + reminders (WorkManager; not yet added).
4. **Schedule feature** — Course/weekly timetable storage + weekly grid UI.
5. **Dashboard aggregation** — switch Home sections to real data (schedule today, upcoming events, notes count, monthly expenses).
6. **Cebu weather** — Retrofit service in `core/network/` + repository + loading/error states replacing the "NOT AVAILABLE YET" card.
7. **Theming & polish** — light theme, final branding assets, saving/FAB conventions.
8. **Quality** — unit + instrumented tests per feature, lint baseline cleanup.
