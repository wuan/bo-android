# Design

## Context

See `proposal.md` — Why. The relevant current state:

- `BlitzortungMapScreen.buildAlertListTemplate()` builds a `ListTemplate` with a
  `Header` whose title is `R.string.car_app_name` ("Blitzortung"), and a single
  `ItemList` of rows. The rows come from `CarAlertListPresenter.rows()`, which
  prepends a countdown row (`R.string.car_status_next_refresh`, "Next update in
  %d s") before the alert rows. That is why the name and the countdown are two
  separate, spaced lines.
- `CarAlertListState` carries `refreshSecondsRemaining: Int?`, parsed from
  `StatusUpdate("<remaining>/<period>")` in `CarAlertListState.reduce()`; the
  period is discarded.
- The Car App Library 1.7.0 `Header` has a title and (start/end) actions but no
  subtitle, and in a map-based template (`MapWithContentTemplate`) header action
  **labels are stripped** — only icons/standard actions show. So a text countdown
  cannot be a header action; the only way to put the name and countdown on one
  line is a single title string.
- The display name also lives in `automotive/app_name` ("Blitzortung"), used as
  the AAOS launcher/activity label.

## Goals / Non-Goals

**Goals:**

- Put the app name and the next-refresh countdown on a single header line.
- Remove the separate countdown row so the alert content follows immediately.
- Show a compact countdown derived from the actual remaining seconds and period.
- Rename the car display name to "Blitzortung Lightning".

**Non-Goals:**

- Control the host's inter-element spacing (the header/list divider is
  host-owned); we only reduce the number of elements.
- Change the alert rows themselves, the data layer, or the reference refresh
  scheduling.
- Change the phone app label.

## Decisions

### Decision: Compose name + countdown into the `Header` title

Set the content template header title to `"<app name> · <countdown>"` when a
refresh status is known, and to the app name alone otherwise.

- **Why**: In a map-based template there is no header subtitle and text header
  actions are stripped, so a single title string is the only way to render both
  on one line. It also removes the countdown row, which is the element that
  created the empty line before the alerts.
- **Alternative considered**: Keep the countdown as the first list row next to a
  header title. Rejected: rows keep their host spacing, so the gap between the
  status and the alerts remains.
- **Alternative considered**: Header end action with a text label. Rejected: the
  label is stripped in map-based templates, so nothing would render.

### Decision: Compact `%1$d / %2$ds` format, period from the payload

Format the countdown as `<remaining> / <period>s` using both numbers
from `StatusUpdate`; keep the period in `CarAlertListState` rather than hardcoding
60, because the period is the user's `QUERY_PERIOD` preference.

- **Why**: The requirement is a compact single line, and the period is
  configurable, so displaying a guessed constant would be wrong.
- **Alternative considered**: Show only the remaining seconds. Rejected: the
  requested shape includes the period ("/ 60s").
- **Alternative considered**: Colon format like the phone (`40/60`). Rejected:
  the requested text is `40 / 60s`.
- **Follow-up from implementation**: A longer "update in 40 / 60s" was tried
  first but the narrow Android Automotive content card ellipsized it, so the
  prefix was dropped to keep the full countdown visible.

### Decision: Join name and countdown with a middle dot separator

Render `"<app name> · <countdown>"` (for example
"Blitzortung Lightning · 40 / 60s").

- **Why**: Without a separator the two phrases run together
  ("Blitzortung Lightning update in 40 / 60s"), which reads as one sentence.
- **Alternative considered**: Plain concatenation. Kept as a trivial fallback:
  the separator is a single literal in the composition, so it can be dropped
  without touching logic.

### Decision: Move the countdown out of the presenter and state rows

`CarAlertListPresenter` no longer emits a countdown row and `CarAlertStrings`
drops `nextRefreshFormat`; `CarAlertListState` keeps the remaining seconds and
gains the period (or a small countdown value object). The screen composes the
header title from the state.

- **Why**: The countdown is now a header concern, not a list row; keeping it out
  of the rows makes the "alerts only" list explicit and the change local.
- **Alternative considered**: Leave the row in the presenter and just hide it.
  Rejected: dead state and a second source of truth.

### Decision: Rename the car display name in `:car` and `:automotive`

Set `car/car_app_name` and `automotive/app_name` to "Blitzortung Lightning"; the
phone label (":app", "Blitzortung Lightning Monitor") is unchanged.

- **Why**: The header and the AAOS launcher label are the two places the car user
  sees the name; the phone name is intentionally different and out of scope.

## Risks / Trade-offs

- **Title truncation in the narrow card** → The composed title is longer than the
  previous name and the AAOS content card is narrow. Confirmed on the emulator
  that "update in <remaining> / <period>s" ellipsizes, so the "update in" prefix
  was dropped; the header now reads "Blitzortung Lightning · 40 / 60s" and fits.
- **Per-second header invalidation** → The title changes every second while
  realtime, so the template is rebuilt at that cadence; this already happened for
  the countdown row, so the cost is unchanged.
- **Requirement name stays "Car alert list reports the time to the next refresh"**
  even though the countdown now lives in the header → Keeping the existing
  requirement name avoids a rename while the requirement still owns the refresh
  countdown behavior; the description and scenarios state the header location.

## Migration Plan

Code-only change in `:car` (plus one `:automotive` string) with unit tests; no
stored state, preference keys, or data migrations. Rollback is a straight revert.
