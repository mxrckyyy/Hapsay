# ARCHITECTURE.md — Technical Architecture

> The source code is the source of truth. This document describes what
> **actually exists** in the repository and clearly marks what is planned.
> Companion documents: `AGENTS.md`, `SYSTEM_DESIGN.md`, `PROJECT_CONTEXT.md`,
> `UI_UX_GUIDELINES.md`.

## 1. Technology stack (exact, verified against project files)

| Tool / library | Version | Declared in |
|---|---|---|
| Gradle | 9.6.0 | `gradle/wrapper/gradle-wrapper.properties` |
| Gradle daemon JDK (toolchain) | 25 | `gradle/gradle-daemon-jvm.properties` |
| Android Gradle Plugin | 9.5.0 | `gradle/libs.versions.toml` |
| Kotlin | 2.2.10 | `gradle/libs.versions.toml` |
| Kotlin (agp mode) | **AGP built-in Kotlin** — no separate `org.jetbrains.kotlin.android` plugin | root `build.gradle.kts` |
| KSP | 2.3.12 | `gradle/libs.versions.toml` |
| Compose BOM | 2026.02.01 | `gradle/libs.versions.toml` |
| Kotlin serialization plugin | 2.2.10 (pinned to Kotlin) | `gradle/libs.versions.toml` |
| Navigation Compose | 2.10.2 | `gradle/libs.versions.toml` |
| Lifecycle (runtime-ktx / viewmodel-compose / runtime-compose) | 2.11.0 | `gradle/libs.versions.toml` |
| Room (runtime / ktx / compiler) | 2.8.5 | `gradle/libs.versions.toml` |
| kotlinx-coroutines-android | 1.11.0 | `gradle/libs.versions.toml` |
| Retrofit + kotlinx-serialization converter | 3.0.0 / 3.0.0 | `gradle/libs.versions.toml` |
| kotlinx-serialization-json | 1.11.0 | `gradle/libs.versions.toml` |
| Material icons extended | BOM-managed (no explicit version) | `gradle/libs.versions.toml` |
| Activity Compose / Core KTX | 1.13.0 / 1.19.1 | `gradle/libs.versions.toml` |
| compileSdk / targetSdk | 37 | `app/build.gradle.kts` |
| minSdk | 26 | `app/build.gradle.kts` |
| Java compatibility | 11 | `app/build.gradle.kts` |
| DI framework | **none** — manual `ServiceLocator` | `core/di/ServiceLocator.kt` |

**Known compatibility constraints (do not "fix" without a reason):**
- **KSP 2.3.12 with AGP 9 built-in Kotlin:** the Kotlin-paired KSP line
  (e.g. `2.2.10-2.0.2`) fails configuration because it registers generated
  sources through the `kotlin.sourceSets` DSL, which AGP 9 forbids. KSP 2.3.x
  (AGP 9 support added in KSP 2.3.1) is the required line.
- **kotlinx-serialization-json ≤ 1.11.0:** version 1.12.0 is compiled with
  Kotlin 2.4.0 metadata, which Kotlin 2.2.10 refuses (binary metadata
  incompatibility). Upgrading serialization requires (a) compiling against an
  older runtime or (b) a coordinated Kotlin upgrade — treat as deliberate work.
- Retrofit 3.0.0 brings OkHttp 5 transitively; no explicit OkHttp pin exists.
- Compose UI/Material artifact versions come exclusively from the BOM.

## 2. Package structure (actual)

Root of all app packages: `com.campushelper.app` (namespace and applicationId
both). Module: `:app` only.

```
app/src/main/java/com/campushelper/app/
├── MainActivity.kt                      // entry point ONLY (hosts Compose UI)
├── core/
│   ├── database/AppDatabase.kt          // Room @Database v3 + MIGRATION_1_2 + MIGRATION_2_3
│   ├── network/                         // EMPTY — planned weather service
│   ├── di/ServiceLocator.kt             // manual DI: database + NotesRepository + ExpensesRepository
│   └── util/                            // EMPTY — planned helpers
├── data/
│   ├── entity/
│   │   ├── NoteEntity.kt               // notes table row (id, title, content, created_at, updated_at)
│   │   └── ExpenseEntity.kt            // expenses table row (id, title, amount_minor, category, expense_date_epoch_day, note, created_at, updated_at)
│   ├── dao/
│   │   ├── NoteDao.kt                  // notes queries
│   │   └── ExpenseDao.kt               // expenses queries (+ category/date filters)
│   └── repository/
│       ├── NotesRepository.kt           // Notes data boundary (interface)
│       ├── NotesRepositoryImpl.kt       // Room-backed impl
│       ├── ExpensesRepository.kt        // Expenses data boundary (interface)
│       └── ExpensesRepositoryImpl.kt   // Room-backed impl
├── domain/
│   ├── model/
│   │   ├── Note.kt                     // notes domain model (pure Kotlin)
│   │   ├── Expense.kt                  // expenses domain model + amount parsing/formatting
│   │   └── ExpenseCategory.kt          // expense category enum (stable ids)
│   └── usecase/                         // EMPTY — planned use cases
├── presentation/
│   ├── CampusHelperApp.kt               // root Scaffold (single)
│   ├── navigation/
│   │   ├── Destinations.kt              // 5 @Serializable routes + bottom items
│   │   ├── CampusHelperNavHost.kt       // single NavHost graph
│   │   └── NavigationExtensions.kt      // navigateToTopLevel
│   ├── components/                      // 8 reusable Campus components
│   │   ├── CampusTopBar.kt
│   │   ├── CampusSectionHeader.kt
│   │   ├── CampusCard.kt
│   │   ├── CampusPrimaryButton.kt
│   │   ├── CampusSecondaryButton.kt
│   │   ├── CampusEmptyState.kt
│   │   ├── CampusBottomNavigation.kt
│   │   └── CampusIconContainer.kt
│   ├── home/                            // IMPLEMENTED dashboard
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── notes/
│   │   ├── NotesScreen.kt               // IMPLEMENTED (Phase 3.3): real list, search, states, FAB
│   │   ├── NotesViewModel.kt            // IMPLEMENTED (Phase 3.2): NotesUiState + search + CRUD intents
│   │   ├── NoteEditorScreen.kt          // IMPLEMENTED (Phase 3.4): create/edit editor, unsaved-changes guard
│   │   └── NoteEditorViewModel.kt       // IMPLEMENTED (Phase 3.4): draft state, validation, save, dirty flag
│   ├── expenses/ExpensesScreen.kt       // placeholder
│   ├── calendar/CalendarScreen.kt       // placeholder
│   ├── schedule/ScheduleScreen.kt       // placeholder
│   ├── weather/                         // EMPTY — planned weather section UI
│   └── components/PlaceholderScreen.kt  // shared "not implemented yet" screen
├── ui/theme/                            // design tokens + theme
│   ├── Color.kt   ├── Theme.kt
│   ├── Type.kt    └── Spacing.kt
└── res/ (values: strings, colors, themes; mipmap; xml backup rules)
```

Note on empty packages: `core/network`, `core/util`, `domain/usecase`, and
`presentation/weather` exist as directories on disk but contain zero files.
They are intentional seams for the planned phases (Git would not track them
until files appear — expected and not a defect). `data/repository` and
`domain/model` are populated (Notes + Expenses data foundations).

Generated artifacts tracked in the repo:
- `app/schemas/.../2.json` — Room schema export for v2 (Notes).
- `app/schemas/.../3.json` — Room schema export for v3 (adds `expenses`).
- `app/src/androidTest/assets/...` — copies of the schema JSONs used by
  `MigrationTestHelper` (generated by the `copyRoomSchemas` Gradle task).
Schema JSONs are committed exactly as generated; they document the contract
every future migration must satisfy and are never hand-edited. Note: no
`1.json` exists — export was off while the notes table was still the seed, so
the 1→2 migration cannot be regression-tested with `MigrationTestHelper`.

Non-source code sets: `app/src/test/` (`ExampleUnitTest.kt`) and
`app/src/androidTest/` (`ExampleInstrumentedTest.kt`, `NoteDaoTest.kt`,
`NotesRepositoryImplTest.kt`). The Notes DAO/repository tests run against an
in-memory Room database via `gradlew.bat connectedDebugAndroidTest`; the
template examples are kept.

### Package responsibilities

| Package | Responsibility | Depends on |
|---|---|---|
| `core` | Cross-cutting infrastructure: database creation, manual DI, (later) network client, helpers | — |
| `data` | Storage contracts: entities, DAOs; (later) repositories that implement data access | `core` |
| `domain` | Business objects and operations; pure Kotlin, no Android UI dependencies | (later) `data` interfaces |
| `presentation` | Compose UI, ViewModels, navigation; renders only state | `ui/theme`, components, (later) `domain` |
| `ui.theme` | Design tokens (color/typography/spacing/shapes) and theme entry | — |

## 3. MVVM implementation

**Reference implementation: `presentation/home/`** — copy its shape for every
new feature. The Notes feature (`presentation/notes/NotesViewModel.kt`,
Phase 3.2) extends the same scheme with repository-backed state.

- **Screen** (`HomeScreen.kt`): collects its ViewModel's
  `StateFlow<HomeUiState>` with `collectAsStateWithLifecycle()`, renders
  sections, and forwards user intents upward via constructor callbacks
  (`onOpenNotes`, `onOpenExpenses`, `onOpenCalendar`, `onOpenSchedule`). No
  ViewModel/DAO/repository/network imports beyond those needed for state
  collection — and no data classes from `data.*` are ever visible.
- **Screen** (`NotesScreen.kt`, Phase 3.3): the first repository-backed
  screen. Collects `NotesUiState` lifecycle-aware, renders
  loading/error/empty/no-results/success states (UI_UX §8.5), owns a debounced
  search field and a `LazyColumn` of notes keyed by id, and hosts a FAB that
  opens the editor (Phase 3.4).
- **Screen** (`NoteEditorScreen.kt`, Phase 3.4): create/edit editor. TopAppBar
  with back + save (enabled only when valid and not saving), borderless title
  and multiline content fields, subtle last-updated metadata, unsaved-changes
  guard (BackHandler + dialog), and honest save error handling. Collects
  `NoteEditorUiState` lifecycle-aware.
- **ViewModel** (`HomeViewModel.kt`; first repo-backed case:
  `NotesViewModel.kt`): owns the private
  `MutableStateFlow<UiState>` and exposes it through
  `val uiState: StateFlow<UiState> = _uiState.asStateFlow()`. Holds derived
  state logic (e.g. the date formatting in Home). Repository observation and
  write calls run in `viewModelScope` (`NotesViewModel` does; Home has no
  repository yet); user actions arrive via screen callbacks/handlers.
- **UI state:** immutable data classes (`HomeUiState`, `NotesUiState`), always
  values, never mutable references, including explicit flags for what has no
  data yet (`hasScheduleToday`, `hasSavedEvents` — both false while no sources
  exist). `NotesUiState` demonstrates the derived-flag pattern: `isEmpty` /
  `hasSearchNoResults` are computed properties, not stored fields.
- **Repository interaction:** `NotesViewModel` (Phase 3.2) and
  `NoteEditorViewModel` (Phase 3.4) are repository-backed ViewModels for Notes;
  `ExpensesRepository` (Phase 4.1) is registered in `ServiceLocator` and ready
  for its ViewModels (Phase 4.2). Rule for every feature: the repository arrives
  via constructor + `viewModel { }` factory — never `ServiceLocator` calls
  inside composables.
- **Lifecycle-aware collection:** `collectAsStateWithLifecycle()` keeps
  updates only while the screen is visible (androidx.lifecycle 2.11.0
  runtime-compose artifact).

## 4. Dependency flow (permitted direction)

```
MainActivity ──► CampusHelperApp (Scaffold+bottom bar) ──► CampusHelperNavHost ──► feature screens
                                              │
                                              ▼ allowed at composition roots only
                                     ServiceLocator (core/di) ──► AppDatabase (core/database)

Feature screen ──► its ViewModel ──► repository/use case ──► DAO/API
                                                        (Notes: repository + ViewModel + list + editor IMPLEMENTED; deletion IMPLEMENTED)
                                                        (Expenses: data layer IMPLEMENTED (Phase 4.1); ViewModels/screens PLANNED)
Feature screen ──► ui/theme tokens + components        (never data.*)
data.*        ──► core.database only
domain.*      ──► nothing Android-specific (domain/model/Note is pure Kotlin)
```

Forbidden:
- composable → DAO/repository/`ServiceLocator` (must go through ViewModel),
- `data.*` → `presentation.*` (data must never know UI),
- `ui.theme` → anything outside `androidx.compose` (tokens are leaf code),
- ViewModel → `presentation` screens (state flows down, events up).

## 5. Dependency injection

`core/di/ServiceLocator.kt` — a manual, lazy singleton registry:

```kotlin
ServiceLocator.provideAppDatabase(context)       // double-checked locking, reused instance
ServiceLocator.provideNotesRepository(context)   // NotesRepositoryImpl wrapping the DB's NoteDao
ServiceLocator.provideExpensesRepository(context) // ExpensesRepositoryImpl wrapping the DB's ExpenseDao
```

Rules:
- It is the single place shared singletons are created. Add providers here
  (future: Retrofit service, data sources), never `Room.databaseBuilder(...)`
  in screens or random classes.
- Nothing constructs `AppDatabase` anywhere else - consumers grab DAOs via the
  repository providers.
- The Room builder registers every schema migration via
  `addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)`; destructive
  fallback is never configured (see `AGENTS.md`).
- No Hilt/Dagger/Koin — see `AGENTS.md` restriction #4.

## 6. Navigation architecture

- Type-safe routes: `@Serializable data object` per destination
  (`presentation/navigation/Destinations.kt`), enabled by the Kotlin
  serialization plugin (pinned to the Kotlin version) and Navigation 2.10.2.
- Single graph: `CampusHelperNavHost` with `startDestination = HomeRoute`;
  `composable<Route> { ... }` blocks wire each destination, injecting screen
  callbacks (`navigateToTopLevel(NotesRoute)` etc.).
- Top-level navigation semantics: `NavigationExtensions.navigateToTopLevel`
  — `popUpTo(graph.findStartDestination().id) { saveState = true }`,
  `launchSingleTop = true`, `restoreState = true`.
- Selection state: bottom bar uses
  `currentBackStackEntryAsState()` + `NavDestination.hasRoute(route::class)`
  (imported from `androidx.navigation.NavDestination.Companion` — an
  easily-forgotten import).
- One Scaffold + one bottom bar, lives in `CampusHelperApp.kt`. `MainActivity`
  only applies theme + hosts `CampusHelperApp`.
- The root Scaffold also owns a single `SnackbarHostState` (Phase 3.3) shared
  with screens that need transient feedback (Notes FAB). Screens receive it as
  a parameter — no second Scaffold, no per-screen host.
- `NoteEditorRoute(noteId: Long? = null)` (Phase 3.4) is a typed destination
  with an optional note id (null = create mode). The NavHost resolves it via
  `toRoute<NoteEditorRoute>()` and pops back to the existing list on save/back.

## 7. Database architecture

- `AppDatabase` (`core/database/AppDatabase.kt`): Room 2.8.5, **`version = 3`**,
  `exportSchema = true`, registers `NoteEntity` and `ExpenseEntity` and exposes
  `noteDao()` / `expenseDao()`.
- Storage: database name `"campus-helper.db"` (app-local storage).
- **Schema export:** `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`
  in `app/build.gradle.kts` writes
  `app/schemas/com.campushelper.app.core.database.AppDatabase/2.json` and
  `.../3.json` on every KSP run. Both JSONs are committed and are the
  reviewable source of truth for what Room expects at each version; never
  hand-edit them. Note: **no schema `1.json` exists** — export was off while
  the notes table was still the seed, so the 1→2 migration cannot be
  regression-tested with `MigrationTestHelper` (honest limitation; every
  future version bump must leave its JSON committed).
- **Migration 1 → 2 (`AppDatabase.MIGRATION_1_2`)** — the notes table was
  finalized: the seed's `body` column became `content` and `created_at` was
  added. SQLite's `RENAME COLUMN` needs SQLite 3.25 (≈ Android 9) and the
  project supports API 26, so the portable recreate-table pattern is used:
  create `notes_new`, copy rows
  (`SELECT id, title, body, updated_at, updated_at FROM notes` — `created_at`
  is backfilled from the row's own `updated_at`, the best derivable value),
  drop `notes`, rename `notes_new` → `notes`. The temp-table DDL is
  byte-for-byte the KSP-generated v2 `createSql` (only the table name
  differs) so the post-migration table always passes Room's schema
  validation. Registered on the builder in `ServiceLocator` via
  `addMigrations(...)`; `fallbackToDestructiveMigration()` is never used.
- **Migration 2 → 3 (`AppDatabase.MIGRATION_2_3`)** — adds the `expenses`
  table (additive; the `notes` table and all its rows are untouched). DDL is
  byte-for-byte the KSP-generated v3 `createSql`; two indexes are created
  (`index_expenses_expense_date_epoch_day`, `index_expenses_category`). Verified
  by `Migration_2_3_Test` using `MigrationTestHelper` (notes preserved, expenses
  table accepts/returns rows, indexes present).
- Consumers: `NoteDao` is called only by `NotesRepositoryImpl`;
  `ExpenseDao` is called only by `ExpensesRepositoryImpl`. No UI consumes the
  expenses repository yet (Expenses ViewModels/screens are PLANNED).
- Expansion protocol: add entity → register in `@Database(entities=[...])`
  → add DAO accessor → bump `version` → write an explicit migration and
  register it in `ServiceLocator` → build (`kspDebugKotlin` must pass; the
  new schema JSON must appear under `app/schemas/` and be committed, and the
  `copyRoomSchemas` Gradle task bundles it for `MigrationTestHelper`).
  Destructive fallback stays forbidden (`AGENTS.md`).
- Data types note: timestamps are stored as epoch-millis `Long`
  (`NoteEntity.createdAt`/`updatedAt`, `ExpenseEntity.createdAt`/`updatedAt`);
  expense dates are stored as epoch days (`ExpenseEntity.expenseDateEpochDay`);
  money is stored as centavos (`ExpenseEntity.amountMinor`). Consistent
  choices for future entities.

## 8. Coding conventions (practical)

**Kotlin**
- State of a screen: `HomeUiState`; ViewModel: `HomeViewModel`; screen:
  `HomeScreen.kt`; component: `CampusXxx.kt`; route: `XxxRoute`.
- `private val _uiState` (mutable) + public `uiState` (read-only) naming.
- Prefer immutable data classes + `data object`s; explicit types on public
  API, inferred inside scopes.
- Coroutines: `viewModelScope`, `Flow` for streams, `suspend` for one-shot;
  no `GlobalScope`, no arbitrary thread `runBlocking` in UI code.
- ViewModel unit tests: inject a fake of the repository interface (no Room),
  override `Dispatchers.Main` with a `StandardTestDispatcher`, and drive
  logical time with `kotlinx.coroutines.test` (`runTest`, `advanceTimeBy` —
  this is how the 300 ms search debounce is covered). See
  `presentation/notes/NotesViewModelTest.kt`.

**Compose**
- `modifier: Modifier = Modifier` is the **first optional parameter** of
  composables that fill space; component-specific input first where natural
  (screens put callbacks first, then modifier, then `viewModel` default).
- Preview pattern: private section composables (`QuickAccessItem`) +
  shared `PlaceholderScreen`; components are previewable stateless functions.
- Accessibility: icons inside labeled controls get `contentDescription = null`
  semantics; icons that stand alone get a real description; interactive
  targets ≥ 48dp (M3 defaults enforce it; `heightIn(min = 48.dp)` is used on
  custom buttons).
- Comments explain architectural intent, extension points, and state caveats
  (see existing files for tone and density).

**Gradle**
- All versions in `gradle/libs.versions.toml`; modules reference aliases
  only (`libs.plugins.ksp` etc.).
- Never set versions inside module build files.
