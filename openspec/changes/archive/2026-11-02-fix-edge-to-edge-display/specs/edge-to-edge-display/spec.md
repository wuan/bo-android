# Spec Delta

## Purpose

Ensure the app's content is laid out correctly relative to the system bars and display cutout across all supported Android versions, so edge-to-edge display is consistent and no critical UI is obscured.

## ADDED Requirements

### Requirement: Edge-to-edge is enabled across supported Android versions
The app SHALL draw its content behind the system bars on Android 15 and later, and SHALL opt into the same layout on earlier supported versions with backward-compatible transparent system bars and legible system bar icons.

#### Scenario: Android 15 or later
- **WHEN** the app runs on a device running Android 15 or later
- **THEN** content is drawn behind the status bar and navigation bar without an opaque system bar background

#### Scenario: Earlier supported versions
- **WHEN** the app runs on a supported device below Android 15
- **THEN** content is drawn behind transparent system bars, matching the Android 15 layout

### Requirement: Every screen applies window insets exactly once
Each screen SHALL apply system bar and display cutout insets at a single place so that content is neither obscured nor double-padded.

#### Scenario: No duplicated insets
- **WHEN** a screen is displayed
- **THEN** its content is offset from the system bars exactly once, with no additional gap at any edge

### Requirement: The main map fills the window edge-to-edge
The map SHALL extend behind the status bar and navigation bar so that no blank band is visible at the top or bottom of the screen.

#### Scenario: Map reaches the screen edges
- **WHEN** the main screen is displayed
- **THEN** map imagery is visible behind the status bar and behind the navigation bar

### Requirement: Critical controls remain fully visible and usable
Status text, warning text, and the top, bottom, and side-anchored controls SHALL remain fully within the usable area across navigation modes and with a display cutout.

#### Scenario: Gesture navigation
- **WHEN** the device uses gesture navigation
- **THEN** the top, bottom, and side controls do not overlap the system bars or system gesture areas

#### Scenario: Three-button navigation
- **WHEN** the device uses three-button navigation
- **THEN** the bottom controls remain fully visible above the navigation bar

#### Scenario: Display cutout in landscape
- **WHEN** a display cutout is present and the device is in landscape
- **THEN** controls anchored to the affected edge remain fully visible and clear of the cutout

### Requirement: Map-canvas overlays clear the system bars
Overlays drawn inside the map canvas, including the scale bar and copyright text, SHALL be positioned clear of the navigation bar.

#### Scenario: Scale bar and copyright remain visible
- **WHEN** the map is displayed
- **THEN** the scale bar and copyright text are drawn fully within the visible map area above the navigation bar

### Requirement: System bar icons remain legible
System bar icons SHALL maintain sufficient contrast against the content drawn behind them.

#### Scenario: Dark content behind the bars
- **WHEN** the content drawn behind the system bars is dark
- **THEN** the system bar icons are rendered in a contrasting color
