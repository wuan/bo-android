# Proposal

## Why

The in-car map currently opens at osmdroid's default camera (centre `0,0`,
default zoom) and never frames the driver's position: the first `LocationUpdate`
only moves the own-location marker. A driver therefore starts on an empty ocean
view far from the strikes that matter, and has to pan and zoom manually before
the map is useful.

## What Changes

- Frame the car map on the first available location fix: centre the camera on the
  current position and apply a default zoom suited to nearby-strike viewing.
- Perform the initial framing once per map surface. Later location updates keep
  updating the own-location marker only, so the driver's pan and zoom are not
  overridden while driving.
- Keep the existing pan and zoom actions working; a manual pan or zoom after the
  initial framing is preserved.
- When no location is available (permission denied or no fix), keep the current
  fallback behaviour and leave the camera as-is.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `car-integration`: the "Car map shows live lightning strikes" requirement gains
  initial camera framing around the current position, so the "Map follows the
  user location" scenario covers starting the map zoomed to the driver's
  position rather than only marking it.

## Impact

- `:car` — `CarMapRenderer` and its `OsmdroidCarMapRenderer` implementation
  (`onLocationEvent` gains one-time initial framing), plus their unit tests.
- No change to `:app`, `:core`, the phone UI, preference keys, or the data layer.
- Both car platforms share `:car`, so Android Auto and Android Automotive OS both
  get the behaviour; the spec requires the two platforms stay consistent.
