# Proposal

## Why

The car alert list is effectively inert: it never receives strike data, so it
stays on "Waiting for lightning data" even after data has loaded, and it shows no
indication of when the next refresh happens. The car experience already promises
an alert summary beside the map, but nothing feeds the alert computation on
either car platform.

## What Changes

- Forward car strike-data events into the shared alert computation, so the car
  alert list reflects the real alert state (closest strike, sector counts, "no
  strikes in range", "location unknown") instead of a permanent empty state.
- Show a live countdown to the next data refresh in the car alert list, derived
  from the existing `StatusUpdate` events the data handler already emits once per
  second in realtime mode.
- Keep the empty state only for the case it describes: strike data that has not
  arrived yet.
- No change to the phone experience, the data layer, or the alert computation
  itself.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `car-integration`: the "Car alert list summarizes proximity alerts" requirement
  gains the expectation that the list is actually driven by received strike data
  (not a standing empty state) and that it reports the time to the next refresh.

## Impact

- `:car` — `AlertRepository` gains a data-event entry point, `BlitzortungMapScreen`
  forwards data events and handles `StatusUpdate`, and `CarAlertListPresenter`
  renders the countdown; unit tests updated.
- `:core` — no behavior change; `AlertHandler.dataEventConsumer` is reused as-is.
- `:app`, the phone UI, preference keys, and the data layer are untouched.
- Both car platforms share `:car`, so Android Auto and Android Automotive OS are
  fixed together.
