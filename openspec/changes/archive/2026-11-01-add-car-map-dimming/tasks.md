# Tasks

## 1. Car-only settings store

- [x] 1.1 Add a `MapDimMode` value (`FOLLOW_CAR`, `ON`, `OFF`) and a `CarSettings` accessor backed by a dedicated `car_settings` `SharedPreferences` file with key `map_dim_mode`; verify with unit tests that an unset preference reads back `FOLLOW_CAR`, each mode round-trips as a string, and the values are not written to `:core`'s default preferences
- [x] 1.2 Expose changes to the stored mode (a listener or `Flow`) from `CarSettings`; verify with a unit test that writing a mode notifies a registered observer with the new value

## 2. Renderer dimming

- [x] 2.1 Add a dimming overlay that draws a translucent black scrim over the map background; replace `setDarkMode` on `CarMapRenderer` with a dimming call that sets the scrim alpha (0 when clear, the fixed value when dimmed); verify with renderer unit tests that the scrim is transparent when not dimmed, visible when dimmed, and that the tile overlay no longer applies `INVERT_COLORS`
- [x] 2.2 Place the dimming overlay beneath the strike and grid overlays so only the map background is dimmed; verify with a renderer test that the dim overlay precedes the strike overlay in `mapView.overlays`

## 3. Map screen dimming and settings entry

- [x] 3.1 Compute the effective dim state on the map screen as `ON`, or `FOLLOW_CAR` with `carContext.isDarkMode`, and apply it in `onGetTemplate()`; verify with screen unit tests covering Follow car dark/light, On, and Off against the renderer
- [x] 3.2 Collect `CarSettings` changes on the map screen and `invalidate()` so a selection made in settings is reflected; verify with a screen unit test that an observed change triggers a template refresh
- [x] 3.3 Add an icon-only settings action to the map header end action that pushes the settings screen; verify with a screen unit test that the header exposes the settings action

## 4. Settings screen

- [x] 4.1 Add `BlitzortungSettingsScreen` as a `ListTemplate` listing the map-dimming setting with its current value, opening a choice screen of the three modes; write the selected mode to `CarSettings` and pop back; add the needed car strings and a selected/check drawable; verify with screen unit tests that the list shows the dimming row, the choice screen offers all three modes, and selecting one stores it

## 5. Integration verification

- [x] 5.1 Run `./gradlew testDebugUnitTest` and verify all new and existing unit tests pass
- [x] 5.2 On the Android Automotive OS emulator verify the map dims in Follow car under a dark theme and clears under a light theme, that On/Off override it, that the setting survives an app restart, and that changing it in settings updates the visible map
- [x] 5.3 On Android Auto verify the same dimming, the reachable settings screen, and that the phone app's own settings are unaffected

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result and confirm the `car-integration` spec reflects the dimming and settings-screen requirements.
