## Purpose

Lets users save their application preferences to a portable JSON file, restore
them on another device or after a reinstall, and restore all preferences to
factory defaults, without relying on opaque platform auto-backup.

## ADDED Requirements

### Requirement: Export preferences to a backup file

The system SHALL let the user export the current application preferences to a
JSON file at a location chosen through the system document picker. The export
MUST write only the content described by the backup format and exclusions
requirements below.

#### Scenario: Successful export
- **WHEN** the user selects the export action and confirms a destination file
- **THEN** the system writes a JSON backup file at the chosen location
- **AND** the current preferences are left unchanged

#### Scenario: Export cancelled
- **WHEN** the user selects the export action and dismisses the document picker
  without choosing a destination
- **THEN** no file is written
- **AND** the current preferences are left unchanged

### Requirement: Backup file format

A backup file SHALL be a JSON object containing a format identifier, a format
version, the application version code that produced it, and a map of preference
keys to values. Preference values MUST preserve their native types (boolean,
integer, long, float, or string) so that an exported value is restored as the
same type.

#### Scenario: Format metadata is present
- **WHEN** a backup file is produced
- **THEN** it contains a format identifier, a format version, and the producing
  application version code

#### Scenario: Value types round-trip
- **WHEN** a backup is exported and then imported on a device with no
  conflicting values
- **THEN** each restored preference has the same type and value it had at export

### Requirement: Sensitive and device-specific values are excluded

The system MUST NOT include the account username, the account password, or the
device-specific map storage path in an exported backup file, even when those
values are present in the preferences.

#### Scenario: Credentials omitted
- **WHEN** the user exports preferences while a username and password are set
- **THEN** the resulting backup file contains neither the username nor the
  password value

#### Scenario: Device-specific map path omitted
- **WHEN** the user exports preferences and the map library has stored its
  storage path in the preferences
- **THEN** the resulting backup file does not contain that storage path value

### Requirement: Import preferences from a backup file

The system SHALL let the user select a backup file through the system document
picker, validate it, and, after an explicit confirmation, replace the
application preferences with the values from the file. An import MUST apply all
eligible values in the file as a single replacement, and preferences not present
in the file MUST NOT retain their prior values.

#### Scenario: Successful import
- **WHEN** the user selects a valid backup file and confirms the import
- **THEN** the application preferences are replaced by the values in the file
- **AND** the system informs the user that a restart is required

#### Scenario: Import cancelled at confirmation
- **WHEN** the user selects a valid backup file but does not confirm the import
- **THEN** the application preferences are left unchanged

#### Scenario: Import does not restore excluded values
- **WHEN** the user imports a backup file
- **THEN** excluded values such as the account password are not restored from the
  file, and any current value of an excluded preference is cleared by the
  replacement

### Requirement: Import warns about unknown keys and summarizes missing keys before applying

Before applying an import, the system SHALL not change any preference until the
user confirms. When the file contains keys the application does not recognize
("unknown keys"), the system MUST surface a prominent warning that names them.
Recognized preference keys that are absent from the file ("missing keys") MUST be
listed in a secondary, less prominent part of the summary and explained as being
reset to their default values. Unknown keys are ignored; missing keys are reset
to defaults.

#### Scenario: Unknown keys trigger a prominent warning
- **WHEN** the user selects a valid backup file containing one or more keys the
  application does not recognize
- **THEN** the system prominently warns about the unknown keys, naming them,
  before applying anything

#### Scenario: Missing keys are summarized secondarily
- **WHEN** the user selects a valid backup file that omits one or more recognized
  preference keys
- **THEN** the system lists those missing keys in a secondary summary line and
  explains that they will be reset to their default values
- **AND** the missing keys do not by themselves produce the prominent unknown-key
  warning

#### Scenario: Summary is shown before any change
- **WHEN** the import summary is displayed
- **THEN** no preference has been changed yet

#### Scenario: No differences to report
- **WHEN** the user selects a valid backup file whose keys exactly match the
  recognized preference keys
- **THEN** the system proceeds to the normal import confirmation without an
  unknown-key warning or missing-key summary

### Requirement: Invalid backup files are rejected without changing preferences

If the selected file cannot be parsed as JSON, is not a backup file produced by
this application, uses an unsupported format version, or contains a value of an
unexpected type, the system MUST report the problem and MUST leave the current
preferences unchanged. Unrecognized preference keys that are otherwise
well-formed MUST be reported in the pre-apply reconciliation summary and then
ignored without failing the import.

#### Scenario: Malformed JSON
- **WHEN** the user selects a file that is not valid JSON
- **THEN** the system reports that the file cannot be read
- **AND** the current preferences are left unchanged

#### Scenario: Foreign or unsupported file
- **WHEN** the user selects a well-formed JSON file whose format identifier or
  format version is not recognized
- **THEN** the system reports that the file is not a supported backup
- **AND** the current preferences are left unchanged

#### Scenario: Unexpected value type
- **WHEN** the user selects a backup file in which a known preference key has a
  value of an unexpected type
- **THEN** the system reports the problem
- **AND** the current preferences are left unchanged

#### Scenario: Unknown keys are ignored
- **WHEN** the user confirms an import of a valid backup file that contains
  preference keys the application does not recognize
- **THEN** the import succeeds for the recognized keys
- **AND** the unrecognized keys are ignored

### Requirement: Reset preferences to defaults

The system SHALL let the user restore all application preferences to the
defaults defined by the application's preference definitions. A reset MUST
require explicit confirmation and MUST clear existing values, including
credentials.

#### Scenario: Successful reset
- **WHEN** the user selects the reset action and confirms it
- **THEN** all preferences are restored to their defined default values
- **AND** the system informs the user that a restart is required

#### Scenario: Reset cancelled
- **WHEN** the user selects the reset action but does not confirm it
- **THEN** the current preferences are left unchanged

### Requirement: Restart after state-changing operations

After a successful import or reset, the system SHALL prompt the user to restart
the application and SHALL update running application state so that it reflects
the new preferences rather than values captured before the operation.

#### Scenario: Restart prompt after import
- **WHEN** an import completes successfully
- **THEN** the system asks the user to restart the application

#### Scenario: Restart prompt after reset
- **WHEN** a reset completes successfully
- **THEN** the system asks the user to restart the application
