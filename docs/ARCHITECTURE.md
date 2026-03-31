# Architecture

## Goal

Keep Android framework code thin and move the tuner's behavior into deterministic, unit-testable Kotlin modules.

## Runtime Flow

1. `MainActivity` requests microphone permission and creates `AudioRecord`.
2. Raw PCM frames are passed to `PitchDetector`.
3. `FrequencySmoother` applies a rolling median to reduce jitter.
4. `NoteMapper` resolves the closest note and target frequency.
5. `TuningFeedbackEvaluator` converts cents deviation into UI feedback bands.
6. `MainActivity` renders the resulting state into the dial, note label, and status text.

## Core Modules

### `PitchDetector`

- Pure Kotlin implementation of a YIN-style pitch estimator
- Works on `ShortArray` PCM frames
- Rejects quiet or undersized buffers early
- Covered with synthetic-signal regression tests

### `FrequencySmoother`

- Maintains a bounded rolling window
- Uses the median rather than the mean
- Keeps transient spikes from destabilizing the UI

### `NoteMapper`

- Converts arbitrary frequencies into nearest-note names
- Computes cents deviation relative to the target note
- Encodes music-theory behavior in one place instead of inside the activity

### `TuningFeedbackEvaluator`

- Maps cents thresholds into accuracy bands and dial directions
- Centralizes feedback thresholds so they can be tuned and regression-tested

## Why This Split Matters

- The important behavior is now testable without emulators or Android framework mocks.
- The tuning rules are easier to reason about in code review.
- Future refactors can replace the activity with a `ViewModel` or presenter without rewriting DSP logic.

## Next Refactors

- Move audio session management out of `MainActivity`
- Add a small UI state model instead of directly mutating views
- Expand instrumentation coverage beyond the current permission and start/stop flows
