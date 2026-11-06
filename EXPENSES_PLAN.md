# EXPENSES_PLAN.md — Hapsay Expenses Module (Phase 4.0)

> **Status:** Planning only. No production code, schema, migration, or dependency
> has been changed. This document is the primary reference for Phases 4.1–4.7.
>
> Companion documents: `AGENTS.md`, `ARCHITECTURE.md`, `SYSTEM_DESIGN.md`,
> `PROJECT_CONTEXT.md`, `UI_UX_GUIDELINES.md`.
>
> The Notes module (Phases 3.1–3.6) is the architectural reference for this
> module: same package layout, same MVVM + repository + manual `ServiceLocator`,
> same testing conventions, same design tokens.

---

## 1. Module overview

Expenses lets a student record, review, and understand personal spending in
Philippine Pesos. It is the second data-backed feature after Notes and the
first one with derived/aggregated values (monthly totals, category breakdown).

Scope discipline (per `AGENTS.md`): only what this plan lists. No bank
integration, no receipts, no budgets, no multi-currency, no cloud sync.

## 2. Goals and non-goals

### Goals
- Record an expense (amount, title, category, date, optional note).
- Review expense history, newest first.
- Edit and delete an expense with confirmation.
- See honest summaries (today / this month / count / per-category) computed
  from real stored records.
- Filter by date range and category.
- Stay consistent with the existing Hapsay dark theme and components.

### Non-goals (out of scope for the initial Expenses module)
- Budgets, spending limits, or alerts.
- Recurring expenses.
- Receipts/photos/attachments.
- Bank or card import.
- Multiple currencies or currency conversion.
- User-created categories.
- Charts beyond a minimal text-first breakdown.
- Export/import.
- Bulk actions or swipe-to-delete.

## 3. Initial feature scope

### 3.1 Record expenses
Fields:
- `amount` (pesos, user input; stored as centavos — see §6).
- `title` (required, non-blank after trim check; stored verbatim).
- `category` (one of the predefined `ExpenseCategory` values).
- `expenseDate` (date only, no time — see §7).
- `note` (optional, may be empty; stored verbatim).

Validation:
- Amount must parse to a positive value (> 0 centavos). Reject zero and
  negatives. Reject non-numeric / malformed input with a field-level message.
- Title must not be blank (whitespace-only counts as blank).
- Category must be one of the enum values (UI prevents invalid selection).
- Date must be a valid `LocalDate` (UI prevents invalid selection).
- Note may be empty.

Invalid input is shown inline on the relevant field (red helper text under the
field), consistent with UI_UX §8.5 (errors are honest, never fake success).
The Save action stays disabled while any required field is invalid.

### 3.2 Expense categories
A Kotlin `enum class ExpenseCategory` with a stable string `id`, display name,
and an icon (`ImageVector`). User-created categories are out of scope.

Proposed categories (final list subject to user approval):

| id | Display name | Icon (Material) |
|---|---|---|
| `food` | Food | `Icons.Filled.Restaurant` |
| `transport` | Transportation | `Icons.Filled.DirectionsBus` |
| `school` | School | `Icons.Filled.School` |
| `personal` | Personal | `Icons.Filled.Person` |
| `shopping` | Shopping | `Icons.Filled.ShoppingBag` |
| `bills` | Bills | `Icons.Filled.Receipt` |
| `entertainment` | Entertainment | `Icons.Filled.Movie` |
| `health` | Health | `Icons.Filled.Favorite` |
| `other` | Other | `Icons.Filled.MoreHoriz` |

Storage: persist the enum's `id` (String) in `ExpenseEntity.category`, and map
to/from the enum in `ExpensesRepositoryImpl` (unknown id degrades to `other`).
Icons are resolved in the presentation layer from the enum, so the data layer
stays free of Compose types where practical (the enum may reference
`ImageVector` via a presentation-side extension if preferred — decision for
approval).

### 3.3 Expense history
- Title, amount, category (icon + label), date.
- Ordered by `expenseDateEpochDay DESC, id DESC` (newest date first; id breaks
  ties deterministically).
- `LazyColumn` with stable keys by id, mirroring Notes.
- Tap a row → edit screen.

### 3.4 Expense summary
Computed from real stored records only (no invented figures):
- Total spent this month (sum of `amountMinor` for expenses whose date falls
  in the current year-month).
- Total spent today.
- Number of recorded expenses (all-time count, or for the active filter —
  decide in Phase 4.2; recommend: count of the currently filtered list).
- Category breakdown: per-category sum + count, sorted by amount desc.

Empty state: all totals show `₱0.00` and count `0`, with an honest empty
message ("No expenses recorded yet"). No fake metrics.

### 3.5 Filtering
Recommended for v1:
- Date range: All / Today / This week / This month.
- Category: All categories / single category.

Deferred to a later phase:
- Custom date range picker.
- Search by title (cheap to add; recommended to include in 4.3 as a simple
  field, but optional).

Keep the filter UI to one row of chips + one dropdown to avoid clutter.

## 4. Currency and amount storage design

**Decision: store money as integer minor units (centavos) in a `Long`.**

- `amountMinor: Long` in `ExpenseEntity` (column `amount_minor`).
- Display conversion: pesos = `amountMinor / 100`, cents = `amountMinor % 100`,
  formatted with two decimals and the `₱` symbol.
- Example: ₱150.50 → `15050` (centavos); ₱1,250.00 → `125000`.
- Parsing input: use `java.math.BigDecimal` (stdlib, no dependency) to parse
  the user's string and convert to centavos via `movePointRight(2)` with
  `RoundingMode.HALF_UP`, then `longValueExact()`. Reject on
  `ArithmeticException`/`NumberFormatException`.
- Never use `Float`/`Double` as the authoritative representation.

Display helper: a small presentation-layer extension
`fun Long.toPesoString(): String` (e.g. using `NumberFormat` with a
`Locale("en", "PH")` or a manual `₱%d.%02d` formatter). Recommend manual
formatting to guarantee the `₱` glyph and two decimals regardless of device
locale. Decision for approval.

## 5. Date and time design

**Decision: store the expense date as `expenseDateEpochDay: Long` (days since
Unix epoch), derived from `java.time.LocalDate.toEpochDay()`.**

Rationale:
- Date-only, no time component → no timezone shift risk (a `LocalDate` is a
  calendar date, not an instant).
- Room stores a plain `INTEGER` (no converter needed).
- Filtering by day/week/month is computed in Kotlin from the epoch day using
  `LocalDate.ofEpochDay(...)`.

Display: `LocalDate.ofEpochDay(epochDay)` formatted with a `DateTimeFormatter`
(e.g. "MMM d, yyyy", matching Notes).

Sorting: `ORDER BY expense_date_epoch_day DESC, id DESC`.

This differs from Notes' epoch-millis timestamps, which is justified: Notes
records *when* a note was edited (an instant), while an expense records *which
day* money was spent (a calendar date). The two representations serve
different purposes.

## 6. Data architecture

### Domain layer (`domain/model/`)
- `Expense` (immutable data class): `id: Long`, `title: String`,
  `amountMinor: Long`, `category: ExpenseCategory`, `expenseDateEpochDay: Long`,
  `note: String`, `createdAt: Long`, `updatedAt: Long`.
- `ExpenseCategory` (enum): `id: String`, `displayName: String`,
  `icon: ImageVector` (or an icon-name resolved in presentation — decision).

### Data layer (`data/`)
- `ExpenseEntity` (`@Entity(tableName = "expenses")`): `id` (PK autoGenerate),
  `title`, `amount_minor`, `category` (String id), `expense_date_epoch_day`,
  `note`, `created_at`, `updated_at`.
- `ExpenseDao`: `observeAll`, `observeById`, `insert`, `update`, `deleteById`.
  (Filtering is done in the ViewModel for v1; DAO stays minimal.)
- `ExpensesRepository` (interface) + `ExpensesRepositoryImpl` (internal):
  `expenses: Flow<List<Expense>>`, `observeExpense(id)`, `createExpense(...)`,
  `updateExpense(...)`, `deleteExpense(id)`. Private mappers; verbatim text
  storage; one clock read per write (`createdAt == updatedAt` on create;
  `createdAt` preserved on update).

### Database
- `AppDatabase` version **3** (from v2), `exportSchema = true`.
- New table `expenses`. No change to `notes` or `NoteEntity`.
- Migration `MIGRATION_2_3`: `CREATE TABLE expenses (...)` only (new table, no
  data migration). Register via `ServiceLocator.addMigrations(..., MIGRATION_2_3)`.
- Exported schema: `app/schemas/.../3.json` (generated by KSP, committed).

### Dependency management
- `ServiceLocator.provideExpensesRepository(context)` (double-checked locking),
  backed by `provideAppDatabase(context).expenseDao()`. No Hilt/Koin/Dagger.

## 7. Presentation architecture

### ViewModels
- `ExpensesViewModel`: owns `ExpensesUiState` (isLoading, expenses (filtered),
  allExpenses, dateFilter, categoryFilter, searchQuery, summary, errorResId).
  Observes `repository.expenses`, applies filters in memory, computes summary.
  No repository writes.
- `ExpenseEditorViewModel`: owns `ExpenseEditorUiState` (title, amountInput,
  category, expenseDate, note, isLoading, isSaving, isDeleting, errorResId,
  initial* for dirty check, createdAt/updatedAt, noteNotFound). Handles
  load (edit mode), validation, save (create/update), delete, unsaved changes.

### Screens
- `ExpensesScreen`: summary section + category breakdown + filter row +
  `LazyColumn` of expenses; empty/loading/error states; FAB → create editor.
- `ExpenseEditorScreen`: amount, title, category selector, date picker, note,
  save, delete (edit mode), unsaved-changes guard.

### States
Loading, empty, populated, error, saving, deleting, validation errors — all
represented, mirroring Notes' honest-state policy (UI_UX §8.5).

## 8. UI/UX layout plan

Follow existing tokens; no new design system. Reuse `CampusCard`,
`CampusEmptyState`, `CampusSecondaryButton`, `CampusIconContainer`, `TopAppBar`.

### Expenses screen
1. Header: title "Expenses", subtitle "Track your spending." (mirrors Notes).
2. Summary card (`CampusCard`): month total (prominent, `headlineMedium`-ish
   via `titleLarge`), today total, count. All from real data; `₱0.00` when empty.
3. Category breakdown: a simple list of rows (icon, name, amount) sorted by
   amount desc. **No chart in v1** (text-first; a thin progress bar is an
   optional, low-value polish deferred to 4.6). Decision for approval.
4. Filter row: chips for All / Today / This week / This month; category
   dropdown (All categories + each).
5. "All Expenses" section title + `LazyColumn` of expense rows (title,
   category icon+label, amount, date), keyed by id. Tap → edit.
6. FAB (green, +) → create editor.
7. Empty state: `CampusEmptyState` "No expenses yet" / "Record your first
   expense to start tracking your spending."
8. Loading: centered `CircularProgressIndicator`; error: honest message + retry.

### Expense editor
- `TopAppBar`: back, title ("New Expense"/"Edit Expense"), save (enabled when
  valid), delete (edit mode only, error-tinted).
- Amount field (numeric, leading `₱`), title field, category selector (chips
  or dropdown), date (opens `DatePicker` dialog), note (multiline).
- Unsaved-changes guard (BackHandler + dialog) identical to Notes.

### Delete confirmation
Same pattern as Notes: "Delete expense?" / "This expense will be permanently
deleted. This action cannot be undone." / Cancel / Delete.

## 9. Navigation proposal

- `ExpensesRoute` already exists (bottom nav, 3rd item).
- Add `@Serializable data class ExpenseEditorRoute(val expenseId: Long? = null)`.
- `CampusHelperNavHost`: `composable<ExpensesRoute> { ExpensesScreen(...) }`
  and `composable<ExpenseEditorRoute> { ... }` using `toRoute<ExpenseEditorRoute>()`.
- Back-stack: editor pushes onto the existing back stack; save/delete/back pops
  to the expenses list, which refreshes via its Flow.
- One NavHost, one Scaffold, one bottom bar preserved. Bottom bar stays visible
  on the editor (consistent with Notes).

## 10. Validation rules

- Amount: must parse to > 0 centavos; reject 0, negatives, malformed.
- Title: non-blank (after the repository's verbatim storage — validation is
  in the ViewModel; the repository does not trim saved text).
- Category: must be a valid enum value (UI constrains selection).
- Date: valid `LocalDate` (UI constrains selection).
- Note: optional, may be empty/whitespace.

## 11. Error handling strategy

- Field-level inline messages for validation (amount, title).
- Load failure: `expense_error_load_failed` shown in empty/error state, with
  automatic retry (mirrors Notes' 3 s retry).
- Save failure: keep editor open, preserve draft, show `expense_error_save_failed`.
- Delete failure: keep editor open, preserve draft, show `expense_error_delete_failed`.
- Never expose stack traces or raw DB errors. Honest empty states.

## 12. Testing strategy

### DAO tests (instrumented, in-memory Room)
Insert, observeAll, observeById, update, deleteById, ordering
(`expense_date_epoch_day DESC, id DESC`), category filter, date-range filter
(if DAO filters are added later).

### Repository tests (instrumented, in-memory Room)
Amount storage conversion (round-trip ₱150.50 ↔ 15050), create, update
(createdAt preserved, updatedAt stamped), delete, filtering, summary
calculations (month total, today total, count, per-category).

### ViewModel tests (unit, fake repository)
Loading, empty, populated, filtering, validation, successful save, failed
save, successful deletion, failed deletion, duplicate-action prevention,
unsaved-changes detection, missing-expense handling.

### Compose UI tests (instrumented)
Screen rendering (empty/populated/error), summary display, list, add, edit,
delete confirmation, validation feedback, filter interactions.

Fake repositories mirror the Notes pattern (reactive re-emission on writes).

## 13. Phased development roadmap

### Phase 4.1 — Expenses Data Foundation
- Objective: domain models, Room entity, DAO, repository, migration v2→v3,
  ServiceLocator provider, schema export, tests.
- Create: `domain/model/Expense.kt`, `domain/model/ExpenseCategory.kt`,
  `data/entity/ExpenseEntity.kt`, `data/dao/ExpenseDao.kt`,
  `data/repository/ExpensesRepository.kt`, `...Impl.kt`, tests.
- Modify: `AppDatabase.kt` (v3, add entity, `MIGRATION_2_3`), `ServiceLocator.kt`
  (provider + migration registration), `build.gradle.kts`? No (no new deps).
- Tests: DAO + repository instrumented tests.
- Completion: build green; migration registered; schema 3.json committed.
- Exclusions: no ViewModels, screens, or navigation.

### Phase 4.2 — Expenses ViewModel
- Objective: state management, filtering, summary calculations, editor VM,
  unit tests with fakes.
- Create: `ExpensesViewModel.kt`, `ExpenseEditorViewModel.kt`,
  `ExpensesUiState`, `ExpenseEditorUiState`, unit tests.
- Modify: none beyond new files.
- Tests: unit tests (loading/empty/populated/filter/validation/save/delete).
- Completion: build + unit tests green.
- Exclusions: no UI.

### Phase 4.3 — Expenses Overview Screen
- Objective: history list, summary, category breakdown, empty/loading/error
  states, FAB.
- Create: `ExpensesScreen.kt`.
- Modify: `CampusHelperNavHost.kt` (wire `ExpensesRoute`).
- Tests: Compose UI tests (empty, populated, summary, filters).
- Completion: build + instrumented tests green; visual verification.
- Exclusions: no editor yet.

### Phase 4.4 — Expense Editor
- Objective: create/edit expense, validation, unsaved-changes guard.
- Create: `ExpenseEditorScreen.kt`, `ExpenseEditorRoute`.
- Modify: `Destinations.kt`, `CampusHelperNavHost.kt`, `ExpensesScreen.kt`
  (FAB + row tap → editor).
- Tests: Compose UI tests.
- Completion: build + tests green; visual verification.
- Exclusions: no deletion yet.

### Phase 4.5 — Delete and Confirmation
- Objective: safe deletion with confirmation dialog and failure handling.
- Modify: `ExpenseEditorScreen.kt`, `ExpenseEditorViewModel.kt`.
- Tests: unit + Compose UI tests for delete.
- Completion: build + tests green.
- Exclusions: no filtering/summary polish beyond what exists.

### Phase 4.6 — Filtering and Summary Polish
- Objective: date/category filters, category breakdown refinement, final UI
  improvements (only if not already in 4.3).
- Modify: `ExpensesScreen.kt`, `ExpensesViewModel.kt` (if needed).
- Tests: filter/summary tests.
- Completion: build + tests green.

### Phase 4.7 — Expenses Final Audit
- Objective: data integrity, tests, navigation, migration verification,
  documentation audit (mirrors Phase 3.6).
- No new features. Fix only permitted small issues.

## 14. File structure proposal

```
app/src/main/java/com/campushelper/app/
├── domain/model/
│   ├── Expense.kt
│   └── ExpenseCategory.kt
├── data/
│   ├── entity/ExpenseEntity.kt
│   ├── dao/ExpenseDao.kt
│   └── repository/
│       ├── ExpensesRepository.kt
│       └── ExpensesRepositoryImpl.kt
├── presentation/expenses/
│   ├── ExpensesScreen.kt
│   ├── ExpensesViewModel.kt
│   ├── ExpenseEditorScreen.kt
│   └── ExpenseEditorViewModel.kt
app/src/test/.../presentation/expenses/   (unit tests)
app/src/androidTest/.../                 (DAO + Compose tests)
app/schemas/.../3.json                   (generated, committed)
```

## 15. Risks and design decisions

1. **Currency as integer centavos** — avoids float; requires careful parsing
   (`BigDecimal`) and formatting. Risk: parsing edge cases (e.g. "1.999" →
   rounding). Mitigation: `HALF_UP` rounding + `longValueExact()`, reject on
   overflow. *Decision for approval.*
2. **Date as epoch day (Long)** — avoids timezone shifts; differs from Notes'
   epoch-millis timestamps. Justified (calendar date vs instant). *Decision for
   approval.*
3. **Category as enum with stable string id** — simple and exhaustive; unknown
   DB values degrade to `other`. *Decision for approval.*
4. **Filtering/summary in ViewModel (in-memory)** — simpler for v1; fine for
   hundreds of rows. If scale grows, push filters into DAO later.
5. **No charts in v1** — text-first breakdown; avoids decorative complexity.
   *Decision for approval.*
6. **No user-created categories in v1** — keeps the model stable; revisit
   later.
7. **Migration v2→v3 is additive (new table only)** — low risk; no data
   migration; existing Notes records untouched.
8. **Icon references in the domain enum** — the enum may reference
   `ImageVector`, coupling domain to Compose. Alternative: store an icon name
   and resolve in presentation. *Decision for approval* (recommend resolving
   icons in presentation to keep domain pure).

## 16. Future improvements outside initial scope

- Budgets and spending limits with alerts.
- Recurring expenses.
- Receipt photo attachments.
- Bank/card import or CSV export.
- Multiple currencies.
- User-created categories.
- Richer charts (trends over time).
- Home dashboard aggregation (monthly total, recent expenses) — belongs to the
  dashboard aggregation phase, not Expenses alone.

## 17. Completion criteria (per phase)

Each phase completes only when:
- `gradlew.bat assembleDebug` succeeds.
- `gradlew.bat testDebugUnitTest` succeeds (with the phase's tests).
- `gradlew.bat connectedDebugAndroidTest` succeeds if an emulator is available.
- Documentation is updated (`ARCHITECTURE.md`, `SYSTEM_DESIGN.md`,
  `PROJECT_CONTEXT.md`).
- No scope creep into later phases.

## 18. Decisions requiring user approval

1. Currency storage: integer centavos (`Long`) vs. `BigDecimal`-as-text.
2. Date storage: epoch day (`Long`) vs. epoch millis at local midnight.
3. Category representation: enum with stable string id (recommended) vs.
   sealed class.
4. Icon resolution: enum references `ImageVector` vs. presentation resolves by id.
5. Initial category list (9 categories) and their icons.
6. v1 filtering set (All/Today/Week/Month + category) and deferral of custom
   date range + title search.
7. No charts in v1 (text-first category breakdown).
8. No user-created categories in v1.
9. Filtering/summary computed in ViewModel vs. DAO for v1.

---

## 19. Confirmation

This document proposes architecture only. No Kotlin source files, Room schemas,
migrations, Gradle dependencies, or production code have been created or
modified by Phase 4.0. The Notes feature and all other existing behavior remain
untouched.
EXPENSES_PLAN.md is the primary reference for Phases 4.1–4.7.
