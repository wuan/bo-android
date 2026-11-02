# Proposal

## Why

The car status block wastes the little vertical space the content card has: the
app name and the refresh countdown are separate lines, an empty gap sits between
the countdown and the alerts, and the countdown reads verbosely ("Next update in
51 s"). On a small car card this pushes the alert content down behind a
mostly-empty status block, and the app name does not match the product name.

## What Changes

- Show the app name and the next-refresh countdown together on one line in the
  car map header, in a compact form (`40 / 60s`).
- Remove the countdown as a separate alert-list row, so the alert content sits
  directly below the header with no empty status line in between.
- Rename the car display name to **Blitzortung Lightning** in the map header and
  in the Android Automotive OS app/launcher label.
- No change to the phone experience, the data layer, or the alert computation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `car-integration`: the "Car alert list reports the time to the next refresh"
  requirement changes from a countdown row in the alert list to a compact
  countdown shown in the map header on the same line as the app name.

## Impact

- `:car` — `BlitzortungMapScreen` composes a combined header title;
  `CarAlertListState`/`CarAlertListPresenter` drop the countdown row and carry
  the remaining seconds plus the period; car strings and unit tests updated.
- `:automotive` — the app label string is updated.
- `:app`, `:core`, the phone UI, preference keys, and the data layer are
  untouched.
