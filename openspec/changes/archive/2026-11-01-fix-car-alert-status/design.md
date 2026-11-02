# Design

## Context

See `proposal.md` — Why. The relevant current state:

- `AlertHandler` (`:core`) computes `Warning` values (`LocalActivity`, `Outlying`,
  `NoData`, `NoLocation`) and broadcasts them through a `ConsumerContainer`.
  `AlertRepository.observeAlertEvents()` exposes them as a cold `Flow`, replaying
  the container's seed value (`NoData`) to the first collector.
- On the phone, `Main.dispatchDataEvent()` and `AppService` call
  `alertHandler.dataEventConsumer(event)` for every `DataEvent`, which is what
  actually feeds the alert computation. `:car`'s `BlitzortungMapScreen` mirrors the
  repository wiring but never calls this entry point, so `AlertHandler` only ever
  sees its initial state.
- `MainDataHandler.run()` broadcasts `StatusUpdate("<remainingSeconds>/<periodSeconds>")`
  once per second while realtime; the phone renders it in its status line. The car
  ignores `StatusUpdate`.
- `CarAlertListPresenter.rows()` returns the waiting label both when `hasData` is
  false and when the warning is `NoData`, so the missing alert wiring surfaces as a
  permanent "Waiting for lightning data".
- The car app exercises realtime mode only; there is no history or animation UI.

## Goals / Non-Goals

**Goals:**

- Make the car alert list reflect the real alert state by feeding the shared alert
  computation the same way the phone does.
- Show a live countdown to the next refresh, sourced from the existing
  `StatusUpdate` events.
- Keep the fix inside `:car`; reuse `AlertHandler` unchanged.

**Non-Goals:**

- Changing the alert computation, its ranges, or its timing.
- Adding a car refresh interval preference or any settings UI.
- Changing the phone status line or `MainDataHandler`.

## Decisions

### Decision: Expose a data-event entry point on `AlertRepository`

Add `AlertRepository.onDataEvent(event: DataEvent)` that delegates to
`alertHandler.dataEventConsumer(event)`, and call it from
`BlitzortungMapScreen`'s data collector for every event.

- **Why**: `:car` already depends on `AlertRepository` through `CarDependencies`,
  so this reuses the existing abstraction and matches what `Main`/`AppService` do
  on the phone without leaking the `AlertHandler` itself into the car module.
- **Alternative considered**: Add `AlertHandler` to `CarDependencies`. Rejected:
  it exposes a handler that everything else accesses through repositories, for a
  single forwarding call.
- **Alternative considered**: Register `AlertHandler` as a `MainDataHandler`
  consumer inside `:core`. Rejected: it would change the phone wiring and the
  shared data flow, which the spec says must stay unchanged.

### Decision: Derive the countdown from `StatusUpdate` in the screen, render it as a row

`BlitzortungMapScreen` handles `is StatusUpdate ->`, parses the seconds remaining
(the part before `/`), stores it on `CarAlertListState`, and calls `invalidate()`.
`CarAlertListPresenter` renders it as the first row using a new
`CarAlertStrings.nextRefreshFormat` (for example "Next update in %d s"). The row
is shown whenever a status has been reported, independent of the alert/error row.

- **Why**: `StatusUpdate` is the existing, already-tested source of "time until
  next refresh", and a presentational string lives with the other car strings in
  the presenter where it is unit-testable.
- **Alternative considered**: Show the raw `"remaining/period"` string like the
  phone. Rejected: it is a phone-specific status format; "next update in N s" reads
  clearly in the car without extra context.
- **Alternative considered**: Put the countdown in the template header. Rejected:
  the Car App Library `Header` has no subtitle in 1.7.0, and a title-only header
  would overload the app name.

### Decision: Keep the existing `hasData`/`warning` mapping and fix the wiring

Leave `CarAlertListPresenter`'s mapping of `LocalActivity`/`Outlying`/`NoLocation`/
`NoData` as-is; the waiting label staying for `NoData` is correct once the wiring
is fixed, because it then genuinely means "no strike data available for alerting".

- **Why**: The bug is the missing input, not the mapping. Reusing the mapping keeps
  the change minimal and the existing presenter tests valid.
- **Alternative considered**: Special-case `hasData == true` with `NoData` to a
  different label. Rejected: `NoData` already means "no data for alerting"; adding
  a second label would not be more truthful.

## Risks / Trade-offs

- **Template invalidation at 1 Hz** (`StatusUpdate` every second) → The countdown
  row changes each second, so the template is rebuilt at that cadence. This matches
  the data cadence and is the intended behavior of a live countdown; if a host
  throttles redraws, the countdown simply updates less often rather than breaking.
- **`StatusUpdate` format is parsed, not typed** → Parsing the leading integer is
  guarded with `toIntOrNull()`, and a null value simply omits the countdown, so a
  format change degrades to "no countdown" instead of a crash.
- **Alert computation now runs in the car** → It already runs in the shared graph
  (the car collects location and data); the added call is the same one the phone
  makes, so cpu/cache behavior is unchanged.

## Migration Plan

Code-only change in `:car` plus unit tests; no stored state, preference keys, or
data migrations. Rollback is a straight revert.
