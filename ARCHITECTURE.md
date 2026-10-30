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
│   ├── database/AppDatabase.kt          // Room @Database v1 (seed: Note)
│   ├── network/                         // EMPTY — planned weather service
│   ├── di/ServiceLocator.kt             // manual DI singleton holder
│   └── util/                            // EMPTY — planned helpers
├── data/
│   ├── entity/NoteEntity.kt             // seed entity (verifies Room+KSP)
│   ├── dao/NoteDao.kt                   // seed DAO (observer + CRUD skeleton)
│   └── repository/                      // EMPTY — planned repositories
├── domain/
│   ├── model/                           // EMPTY — planned domain models
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
│   ├── notes/NotesScreen.kt             // placeholder
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

Note on empty packages: `core/network`, `core/util`, `data/repository`,
`domain/model`, `domain/usecase`, and `presentation/weather` exist as
directories on disk but contain zero files. They are intentional seams for
the planned phases (Git would not track them until files appear — that is
expected and not a defect).

Non-source code sets: `app/src/test/` (`ExampleUnitTest.kt`, compiles) and
`app/src/androidTest/` (`ExampleInstrumentedTest.kt`, compiled via
`assembleDebugAndroidTest`). Both were template examples and are kept.

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
new feature.

- **Screen** (`HomeScreen.kt`): collects its ViewModel's
  `StateFlow<HomeUiState>` with `collectAsStateWithLifecycle()`, renders
  sections, and forwards user intents upward via constructor callbacks
  (`onOpenNotes`, `onOpenExpenses`, `onOpenCalendar`, `onOpenSchedule`). No
  ViewModel/DAO/repository/network imports beyond those needed for state
  collection — and no data classes from `data.*` are ever visible.
- **ViewModel** (`HomeViewModel.kt`): owns the private
  `MutableStateFlow<UiState>` and exposes it through
  `val uiState: StateFlow<UiState> = _uiState.asStateFlow()`. Holds derived
  state logic (e.g. the date formatting in Home). Future repository calls run
  in `viewModelScope`; future user actions arrive via an `onAction(...)` hook.
- **UI state:** immutable data classes (`HomeUiState`), always values, never
  mutable references, including explicit flags for what has no data yet
  (`hasScheduleToday`, `hasSavedEvents` — both false while no sources exist).
- **Repository interaction:** today: none (no repositories exist). Planned:
  ViewModel ← (use case)/repository → DAO. When repositories appear,
  ViewModels obtain them through `ServiceLocator`, ideally injected via the
  ViewModel constructor + `viewModel { }` factory — not by ServiceLocator
  calls inside composables.
- **Lifecycle-aware collection:** `collectAsStateWithLifecycle()` keeps
  updates only while the screen is visible (androidx.lifecycle 2.11.0
  runtime-compose artifact).

## 4. Dependency flow (permitted direction)

```
MainActivity ──► CampusHelperApp (Scaffold+bottom bar) ──► CampusHelperNavHost ──► feature screens
                                              │
                                              ▼ allowed at composition roots only
                                     ServiceLocator (core/di) ──► AppDatabase (core/database)

Feature screen ──► its ViewModel ──► (planned) repository/use case ──► DAO/API
Feature screen ──► ui/theme tokens + components        (never data.*)
data.*        ──► core.database only
domain.*      ──► nothing Android-specific (planned pure Kotlin)
```

Forbidden:
- composable → DAO/repository/`ServiceLocator` (must go through ViewModel),
- `data.*` → `presentation.*` (data must never know UI),
- `ui.theme` → anything outside `androidx.compose` (tokens are leaf code),
- ViewModel → `presentation` screens (state flows down, events up).

## 5. Dependency injection

`core/di/ServiceLocator.kt` — a manual, lazy singleton registry:

```kotlin
ServiceLocator.provideAppDatabase(context)   // double-checked locking, reused instance
```

Rules:
- It is the single place shared singletons are created. Add providers here
  (future: Retrofit service, data sources), never `Room.databaseBuilder(...)`
  in screens or random classes.
- Nothing constructs `AppDatabase` anywhere else. (Today no caller uses it —
  repositories will, in their phase.)
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

## 7. Database architecture

- `AppDatabase` (`core/database/AppDatabase.kt`): Room 2.8.5, `version = 1`,
  `exportSchema = false` (schema export turns on when the model is final),
  currently registers the seed `NoteEntity` and exposes `noteDao()`.
- Seed toolchain evidence: entities/DAOs currently exist to prove Room + KSP
  (2.3.12) code generation works under AGP 9 built-in Kotlin; they are *not*
  yet consumed by any UI. Real entities (Expense, Event, Course) and their
  DAOs come with each feature phase; when the schema becomes real,
  `room.schemaLocation` KSP argument is expected with `exportSchema = true`.
- Naming/storage: database name `"campus-helper.db"` (app-local storage).
- Expansion protocol: add entity → register in `@Database(entities=[...])`
  → add DAO accessor → bump `version` → write migration (or agree on
  destructive fallback policy explicitly in an issue — none chosen yet),
  → build (`kspDebugKotlin` must pass).
- Data types note: timestamps are stored as epoch-millis `Long`
  (`NoteEntity.updatedAt`) — consistent choice for future entities.

## 8. Coding conventions (practical)

**Kotlin**
- State of a screen: `HomeUiState`; ViewModel: `HomeViewModel`; screen:
  `HomeScreen.kt`; component: `CampusXxx.kt`; route: `XxxRoute`.
- `private val _uiState` (mutable) + public `uiState` (read-only) naming.
- Prefer immutable data classes + `data object`s; explicit types on public
  API, inferred inside scopes.
- Coroutines: `viewModelScope`, `Flow` for streams, `suspend` for one-shot;
  no `GlobalScope`, no arbitrary thread `runBlocking` in UI code.

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
