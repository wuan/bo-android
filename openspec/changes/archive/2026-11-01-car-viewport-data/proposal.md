# Proposal

## Why

The car map is pinned to a single data request: because nothing feeds the map
viewport to the data layer, `MainDataHandler` never changes the grid size and
never derives a local data area, so the car always asks for the *global* grid
with 25 km squares — no matter where the driver pans or how far they zoom. The
phone already adapts (auto grid size by zoom, global/local by viewport); the car
should behave the same.

## What Changes

- Feed the car map's viewport to the data layer, so the grid size and the
  global/local data area follow the map exactly as they do on the phone:
  global data with ≥25 km squares at low zoom, local data with <25 km squares
  when zoomed in.
- Fix the car to the phone's automatic mode: region pinned to **Global** and
  grid size forced to **auto**. The phone's `REGION` and `GRID_SIZE` preferences
  are ignored by the car, so any classical region (Europe, Asia, …) is
  unreachable.
- Do not draw the local data-area outline on the car map (the phone keeps it).
- Defer the car's first data fetch until the first viewport notification, so a
  throwaway global request is not issued before the map is laid out.
- Expose a narrow viewport-update entry point on `StrikeDataRepository` instead
  of leaking `MainDataHandler` into `:car`.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities

- `car-integration`: the car map derives its grid size and global/local data
  automatically from the map viewport, with no region or grid-size
  configuration and no local data-area outline. The existing "Car app reuses
  the existing data layer" requirement is unchanged — this adds the missing
  viewport wiring on top of it.

## Impact

- `:car`
  - A `MapViewport` adapter over the plain osmdroid `MapView`.
  - `OsmdroidCarMapRenderer`: push the viewport on every camera change
    (pan, pinch/action zoom, first-fix framing, restored camera) and on layout
    changes.
  - `BlitzortungMapScreen`: gate the initial `updateData()` on the first
    viewport notification; disable the data-area outline.
- `:core`
  - `StrikeDataRepository`: a viewport-update method that forces region Global
    and auto grid size.
  - `StrikeListOverlay`: an opt-out for the data-area outline (default keeps the
    phone behaviour).
- No changes to `LocalData`, `JsonRpcData`, the RPC methods, preference keys, or
  stored state. The phone experience is unchanged.
