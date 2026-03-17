# AGENTS.md — Quarterly Countdown (Wear OS)

Essential information for AI agents working on this codebase.

---

## Build Commands

```bash
# Generate Gradle wrapper (required once before first build — needs Gradle 8.7 installed locally)
gradle wrapper --gradle-version 8.7

# Compile + build debug APK
./gradlew assembleDebug

# Run JVM unit tests (fast, no device needed)
./gradlew test

# Run instrumented UI tests (requires Wear OS emulator API 30+ running)
./gradlew connectedAndroidTest

# Run all checks
./gradlew check
```

> **Note:** `gradlew` and `gradle/wrapper/gradle-wrapper.jar` are not committed. Run
> `gradle wrapper --gradle-version 8.7` once to generate them, or open the project in
> Android Studio which will offer to set up the wrapper automatically.

---

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
| Min SDK | 30 (Wear OS 3.0) |
| Target/Compile SDK | 34 |

All dependency versions live in `gradle/libs.versions.toml`.

---

## Package Structure

```
com.example.watchtimer
├── MainActivity.kt                    — Entry point; wires ViewModel → Compose
├── timer/
│   ├── TimerState.kt                  — Constants, TimerStatus enum, TimerUiState data class
│   └── TimerLogic.kt                  — Pure functions: computeUiState, tickerFlow, quarter math
├── viewmodel/
│   └── TimerViewModel.kt              — State machine, coroutine tick job, StateFlow
└── presentation/
    ├── TimerScreen.kt                  — Stateless root composable (Scaffold + tap)
    ├── QuarterProgressRing.kt          — Canvas-based ring with 4 arcs + blink animation
    └── theme/
        ├── Color.kt                    — High-contrast Wear OS palette
        ├── Type.kt                     — Custom typography (display1 = 44sp bold)
        └── Theme.kt                    — WatchTimerTheme composable
```

---

## Key Conventions

### State is always derived via `computeUiState`
Never construct `TimerUiState(...)` manually with hand-picked field values outside of tests.
Always call `computeUiState(remainingMs, status)` — it is the single source of truth for all
derived fields (`completedQuarters`, `currentQuarterProgress`, `displayMinutes`, `displaySeconds`).

### Composables are stateless
`TimerScreen` and `QuarterProgressRing` receive `TimerUiState` and an `onTap` lambda.
They own no state. Do not add `remember` state or `LaunchedEffect` inside them for timer logic.

### Domain layer has no Android imports
`timer/TimerLogic.kt` and `timer/TimerState.kt` must not import anything from `android.*`
or `androidx.*`. This keeps them fast to test on the JVM.

### ViewModel uses `viewModelScope` (not custom scopes)
The tick coroutine runs in `viewModelScope`. Tests override the dispatcher via
`Dispatchers.setMain(testDispatcher)` — do not add a constructor parameter for the dispatcher.

---

## Testing Notes

### JVM unit tests (`src/test/`)

**`TimerLogicTest`** — covers pure functions with standard JUnit assertions.
Uses Turbine (`app.cash.turbine`) for `tickerFlow` assertions.

**`TimerViewModelTest`** — covers the state machine.

Critical pattern for time-based tests:
```kotlin
private val testScheduler  = TestCoroutineScheduler()
private val testDispatcher = StandardTestDispatcher(testScheduler)

@Before fun setUp()    { Dispatchers.setMain(testDispatcher) }
@After  fun tearDown() { Dispatchers.resetMain() }

@Test
fun example() = runTest(testDispatcher) {
    val vm = TimerViewModel()
    vm.onTap()
    advanceTimeBy(181_000L)   // fast-forwards virtual time; triggers all delay()s
    assertEquals(TimerStatus.FINISHED, vm.uiState.value.status)
    vm.onCleared()            // cancel ViewModel coroutines to avoid leaks
}
```

Passing `testDispatcher` (and therefore `testScheduler`) to both `Dispatchers.setMain` and
`runTest` ensures that `viewModelScope` (which uses `Dispatchers.Main`) and the test share
the same virtual clock.

### Instrumented tests (`src/androidTest/`)

**`TimerScreenTest`** — inject `TimerUiState` directly; no ViewModel needed:
```kotlin
composeTestRule.setContent {
    WatchTimerTheme {
        TimerScreen(state = TimerUiState(), onTap = {})
    }
}
composeTestRule.onNodeWithText("03:00").assertIsDisplayed()
```

---

## Wear OS Emulator Setup

1. In Android Studio: **Device Manager → Create Device → Wear OS**
2. Select a round or square Wear OS watch profile
3. System image: **Wear OS 3 (API 30)** or newer
4. Start the emulator before running `connectedAndroidTest`

---

## Architecture Reference

See [`docs/architecture.md`](docs/architecture.md) for:
- Layer diagram (UI → ViewModel → Domain)
- State machine diagram
- Data-flow diagram
- Ring drawing details
- Battery efficiency notes
- Ambient mode upgrade path
