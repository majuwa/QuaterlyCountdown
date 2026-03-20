# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run JVM unit tests (fast, no device needed)
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.watchtimer.viewmodel.TimerViewModelTest"

# Run all checks
./gradlew check

# Run instrumented UI tests (requires Wear OS emulator API 30+ running)
./gradlew connectedAndroidTest
```

> **Note:** `gradlew` is committed. If it is ever missing, regenerate it with
> `gradle wrapper --gradle-version 8.7` or let Android Studio set it up automatically.

## Tech Stack

| Concern | Version |
|---|---|
| AGP | 8.4.2 |
| Kotlin | 1.9.24 |
| Compose compiler extension | 1.5.14 |
| Compose BOM | 2024.05.00 |
| Wear Compose | 1.3.1 |
| Lifecycle | 2.8.2 |
| Coroutines | 1.8.1 |
| Min/Target/Compile SDK | 36 |

All dependency versions live in `gradle/libs.versions.toml`.

## Architecture

The app is a Wear OS 3-minute countdown timer. It follows MVVM with a foreground service owning the tick loop.

```
UI (Compose) → TimerViewModel → TimerService (foreground)
                    ↑                    |
                    └── StateFlow ───────┘
```

**`TimerService`** is the heart of the app. It owns:
- The coroutine tick loop (`tickerFlow` collected in a `CoroutineScope`)
- A `PARTIAL_WAKE_LOCK` so the CPU stays awake when the screen is off
- A `MutableStateFlow<TimerUiState>` exposed as a companion-object `StateFlow`
- Vibration on quarter transitions and on finish
- A foreground notification while running

The service is started/stopped via explicit Intent actions (`ACTION_START`, `ACTION_PAUSE`, `ACTION_RESET`); it is never bound.

**`TimerViewModel`** (`AndroidViewModel`) is a thin dispatcher: it reads the companion `StateFlow` and sends Intent actions to the service. It does not own a coroutine tick.

**Domain layer** (`timer/TimerLogic.kt`, `timer/TimerState.kt`) contains only pure functions with no Android imports — `computeUiState`, `tickerFlow`, `completedQuarters`, `currentQuarterProgress`. All UI data is derived via `computeUiState(remainingMs, status)`.

**UI layer** (`presentation/`) is fully stateless. `TimerScreen` and `QuarterProgressRing` receive `TimerUiState` and an `onTap` lambda; they own no state.

### Package structure

```
de.majuwa.watchtimer
├── MainActivity.kt                    — Entry point; wires ViewModel → Compose
├── service/
│   └── TimerService.kt                — Foreground service; tick loop, WakeLock, vibration
├── timer/
│   ├── TimerState.kt                  — Constants, TimerStatus enum, TimerUiState data class
│   └── TimerLogic.kt                  — Pure functions: computeUiState, tickerFlow, quarter math
├── viewmodel/
│   └── TimerViewModel.kt              — Thin dispatcher; reads StateFlow, sends Intent actions
└── presentation/
    ├── TimerScreen.kt                  — Stateless root composable (Scaffold + tap/long-press)
    ├── QuarterProgressRing.kt          — Canvas-based ring with 4 arcs + blink animation
    └── theme/
        ├── Color.kt                    — High-contrast Wear OS palette
        ├── Type.kt                     — Custom typography (display1 = 44sp bold)
        └── Theme.kt                    — WatchTimerTheme composable
```

### Packages & namespace

Source code lives under `de.majuwa.watchtimer`. The Gradle `namespace`/`applicationId` remains `com.example.watchtimer` (controls R class and app identity). Test file package declarations use `com.example.watchtimer.*` but import from `de.majuwa.watchtimer.*`.

## Key Conventions

- **`computeUiState` is the single source of truth.** Never construct `TimerUiState(...)` manually with hand-picked fields outside of tests — always call `computeUiState(remainingMs, status)`.
- **Domain layer must stay Android-free.** `timer/TimerLogic.kt` and `timer/TimerState.kt` must not import `android.*` or `androidx.*`.
- **Composables are stateless.** No `remember` state or `LaunchedEffect` for timer logic inside `TimerScreen` or `QuarterProgressRing`.
- **`TimerViewModel` uses `AndroidViewModel`.** It needs `Application` context to call `startService`. Do not revert to plain `ViewModel`.

## Testing

### JVM unit tests (`src/test/`)

`TimerLogicTest` — pure-function tests, no mocks needed. Uses Turbine (`app.cash.turbine`) for `tickerFlow` assertions.

`TimerViewModelTest` — tests dispatch logic by manipulating `TimerService._uiState` directly and verifying Intent actions via MockK:

```kotlin
@Before fun setUp() {
    Dispatchers.setMain(testDispatcher)
    TimerService.resetState()   // always start from IDLE
}

@After fun tearDown() {
    Dispatchers.resetMain()
    TimerService.resetState()
}

@Test
fun `onTap from RUNNING sends ACTION_PAUSE`() = runTest(testDispatcher) {
    TimerService._uiState.value = computeUiState(TOTAL_DURATION_MS, TimerStatus.RUNNING)
    val vm = TimerViewModel(mockApplication)
    vm.onTap()
    verify { mockApplication.startService(match { it.action == TimerService.ACTION_PAUSE }) }
}
```

`TimerService._uiState` and `TimerService.resetState()` are `internal` — accessible from tests in the same module without Robolectric.

### Instrumented tests (`src/androidTest/`)

`TimerScreenTest` — injects `TimerUiState` directly into composables; no ViewModel or service needed:

```kotlin
composeTestRule.setContent {
    WatchTimerTheme {
        TimerScreen(state = TimerUiState(), onTap = {})
    }
}
composeTestRule.onNodeWithText("03:00").assertIsDisplayed()
```

Requires a Wear OS emulator (API 30+): Android Studio → Device Manager → Create Device → Wear OS, system image Wear OS 3 (API 30) or newer.

## After Every Code Change

After adding a feature or making any code change:

1. **Run all tests** — `./gradlew test` must pass before considering the change done.
2. **Update `README.md`** — if the change affects user-facing behaviour, features, gestures, or visual design.
3. **Update `CLAUDE.md`** — if the change affects architecture, key conventions, package structure, tech stack, or testing patterns.

## Additional Reference

- `docs/architecture.md` — layer diagrams, state machine, ring drawing details, battery efficiency notes, and ambient mode upgrade path. Note: the ViewModel section there describes an older design (tick loop in ViewModel); the service-based architecture above is current.
