# Architecture — Quarterly Countdown (Wear OS)

## Overview

The app follows **MVVM** with a thin domain layer of pure functions. The goal is maximum testability: business logic lives in functions with no Android dependencies; the ViewModel is the only stateful component; Composables are fully stateless.

```
┌─────────────────────────────────────────┐
│  UI Layer (Jetpack Compose for Wear OS) │
│  TimerScreen · QuarterProgressRing      │
│  Receives: TimerUiState                 │
│  Emits:    onTap()                      │
└────────────────┬────────────────────────┘
                 │ collectAsStateWithLifecycle()
                 ▼
┌─────────────────────────────────────────┐
│  ViewModel Layer                        │
│  TimerViewModel                         │
│  Owns: MutableStateFlow<TimerUiState>   │
│  Owns: coroutine tick Job               │
│  Drives the IDLE/RUNNING/PAUSED/        │
│         FINISHED state machine          │
└────────────────┬────────────────────────┘
                 │ delegates to pure functions
                 ▼
┌─────────────────────────────────────────┐
│  Domain Layer (no Android imports)      │
│  TimerLogic.kt · TimerState.kt          │
│  computeUiState() · tickerFlow()        │
│  completedQuarters() · progress()       │
└─────────────────────────────────────────┘
```

---

## State Machine

```
         ┌───── tap ─────┐
         │               ▼
       IDLE ──tap──► RUNNING ──tap──► PAUSED
         ▲               │               │
         │            time=0             └──tap──► RUNNING
         │               ▼
         └───── tap ── FINISHED
```

The ViewModel's `onTap()` dispatches based on `uiState.value.status`:

| Current status | Result of `onTap()` |
|---|---|
| IDLE | → RUNNING (starts ticker) |
| RUNNING | → PAUSED (cancels ticker, freezes `remainingMs`) |
| PAUSED | → RUNNING (restarts ticker from frozen `remainingMs`) |
| FINISHED | → IDLE (resets `remainingMs = 180 000`) |

---

## Data Flow

```
tickerFlow(100ms)
     │  Flow<Unit>
     ▼
TimerViewModel.startTimer()  (viewModelScope.launch)
     │  decrements remainingMs by 100ms per tick
     │  calls computeUiState(remainingMs, status)
     ▼
MutableStateFlow<TimerUiState>
     │  StateFlow (cold snapshot)
     ▼
collectAsStateWithLifecycle()  (in MainActivity, stops on ON_STOP)
     │  State<TimerUiState>
     ▼
TimerScreen(state, onTap)  →  QuarterProgressRing + TimerDisplay
```

---

## Key Files

| File | Responsibility |
|---|---|
| `timer/TimerState.kt` | Constants, `TimerStatus` enum, `TimerUiState` data class |
| `timer/TimerLogic.kt` | Pure functions: `computeUiState`, `tickerFlow`, quarter math |
| `viewmodel/TimerViewModel.kt` | State machine, coroutine tick job, `StateFlow` |
| `presentation/TimerScreen.kt` | Stateless root composable; Wear `Scaffold` + tap handler |
| `presentation/QuarterProgressRing.kt` | `Canvas` ring with 4 arcs + `InfiniteTransition` blink |
| `presentation/theme/` | Wear Material colors, typography, theme wrapper |
| `MainActivity.kt` | Wires ViewModel → Composable via `collectAsStateWithLifecycle` |

---

## Timer Tick Design

`tickerFlow` is a plain Kotlin `Flow` built with `flow { while(true) { delay(ms); emit(Unit) } }`.

**Why this approach:**
- `delay` is a suspending function that participates in the test coroutine scheduler, enabling deterministic time-travel in unit tests (`advanceTimeBy`).
- The flow is collected inside `viewModelScope`, so it is automatically cancelled when the Activity is destroyed.
- No `WakeLock`, no background threads — the OS manages the coroutine thread pool.

Each tick subtracts exactly `TICK_INTERVAL_MS = 100 ms` from `remainingMs`. This is intentional: using wall-clock deltas would require `System.currentTimeMillis()` which is untestable. The worst-case drift per 180-second run is negligible.

---

## Visual Ring

The ring is drawn on a `Canvas` in `QuarterProgressRing.kt`.

```
        Q1 (12→3 o'clock, 270°–356°)
   Q4 ←                         → Q2
(9→12, 180°–266°)         (3→6, 0°–86°)
              Q3 (6→9, 90°–176°)
```

- Each arc spans **86°** with a **4° gap** on each side boundary.
- Arc stroke = 6.5% of the screen's shorter dimension (scales across watch sizes).
- Blink: `InfiniteTransition.animateFloat(1f → 0.25f, 600 ms, Reverse)` — only active when `status == RUNNING`.

### Color scheme

| State | Color | Hex |
|---|---|---|
| Completed quarter | Cyan | `#00E5FF` |
| Active quarter | Green | `#76FF03` |
| Future quarter | Dark grey | `#424242` |
| Background | Black | `#000000` |

---

## Wear OS Specifics

### Round vs. Square Screens
Wear Compose's `Scaffold` + `TimeText` + `Vignette` handle both form factors automatically. The `Canvas` ring is drawn relative to the composable's bounds and scales correctly on circular or square watches.

### Battery Efficiency
- `collectAsStateWithLifecycle()` stops the Flow collection during `ON_STOP`. When the watch screen turns off and the Activity is stopped, the tick coroutine effectively pauses until the screen returns to the foreground.
- 100 ms tick interval = 10 resumptions/second — negligible CPU overhead.
- No explicit `WakeLock` is held.

### Ambient Mode (future enhancement)
For an always-on display, implement `AmbientLifecycleObserver` (from `androidx.wear:wear`):
1. On `onEnterAmbient` → call `viewModel.pause()`, render with reduced colours.
2. On `onExitAmbient` → resume if it was running.

---

## Testing Strategy

### JVM Unit Tests (`src/test/`)

Pure-function tests run on the JVM — fast, no emulator needed.

- **`TimerLogicTest`**: Covers `computeUiState`, `completedQuarters`, `currentQuarterProgress`, and `tickerFlow` (via Turbine).
- **`TimerViewModelTest`**: Covers all state-machine transitions and time-based completion. Uses `Dispatchers.setMain(StandardTestDispatcher(testScheduler))` so `viewModelScope` and `runTest` share the same virtual clock. `advanceTimeBy(ms)` fast-forwards delays without real time passing.

### Instrumented Tests (`src/androidTest/`)

Compose UI tests run on a Wear OS emulator (API 30+).

- **`TimerScreenTest`**: Injects `TimerUiState` directly into composables — no ViewModel, no coroutines. Tests display text, tap callbacks, and ring rendering across all four timer states.
