# r8-optimization Specification

## Purpose
The release build applies R8 shrinking, obfuscation and optimisation — and
optimised resource shrinking — across the whole program, retaining code only
where a documented reflection, layout-inflation or entry-point dependency
requires it.

## Requirements

### Requirement: Release builds optimize, obfuscate and shrink the whole program

Every release artifact offered to users SHALL have R8 shrinking, obfuscation and
optimisation applied to dependency code as well as app code, rather than
retaining whole third-party namespaces. The R8 statistics embedded in the
shipped bundle MUST show each rate has risen well above the recorded 16%
baseline.

#### Scenario: Dependency code is subject to R8

- **WHEN** a release bundle is built
- **THEN** its embedded R8 statistics show optimisation, obfuscation and shrinking rates at or above the configured target of 75%, and no third-party package is retained in full

#### Scenario: Improvement is measured against the baseline

- **WHEN** the new bundle's R8 statistics are compared with the pre-change values (measured at the current HEAD as optimisation 15.88%, obfuscation 15.97%, shrinking 15.92%; the published-bundle baseline was 16.05/16.14/16.08%)
- **THEN** each of the three rates has increased

### Requirement: Keep rules are narrow and justified

Each R8 keep rule SHALL be limited to a specific class, member or entry point
that is genuinely reached by reflection, layout inflation or a framework entry
point. A keep rule MUST NOT retain an entire third-party package or library with
a catch-all member specification.

#### Scenario: No catch-all third-party keeps

- **WHEN** the merged keep rules for a release build are inspected
- **THEN** no rule matches an entire third-party namespace (for example a whole `androidx` or map-library package) with a catch-all member selector

#### Scenario: Every remaining keep rule is accounted for

- **WHEN** the R8 keep-radius report is generated for a release build
- **THEN** each keep rule with a non-trivial radius maps to a documented reflection, inflation or entry-point dependency, and no single rule dominates the retained set

### Requirement: Reflection- and inflation-dependent code keeps working after obfuscation

Functionality that depends on names R8 would otherwise change — layout-inflated
views, fragments, preferences, background workers, widgets, services and
receivers, the dependency-injection graph, and reflective access to map-library
internals — SHALL continue to work in a minified, obfuscated release build.

#### Scenario: Map library reflective access

- **WHEN** the release build renders the map
- **THEN** map rotation/animation state access and scale-bar centring behave as in the non-minified build

#### Scenario: Inflated and framework-instantiated components

- **WHEN** the release build inflates custom views, preferences and fragments, and the framework instantiates declared services, receivers, workers and widgets
- **THEN** they are created successfully and no `ClassNotFoundException`, `NoSuchMethodException` or `NoSuchFieldException` occurs

#### Scenario: Dependency-injection graph

- **WHEN** the release build launches the app, the background service and the Android Auto entry point
- **THEN** the Dagger graph is injected successfully

### Requirement: Optimised resource shrinking is enabled for release builds

Release builds SHALL enable R8 optimised resource shrinking so that resources
that are unreachable from live code are removed while resources referenced only
dynamically are preserved.

#### Scenario: Bundle reporting

- **WHEN** a release bundle is built
- **THEN** its embedded R8 metadata reports optimised resource shrinking as enabled

#### Scenario: Dynamically referenced resources survive

- **WHEN** the app runs a release build and uses resources referenced only indirectly (for example through resource identifiers resolved from code or preference defaults)
- **THEN** those resources are present and the UI renders them correctly

### Requirement: Release optimisation is verified before shipping

The build SHALL provide a repeatable, local check that reads the R8 statistics
from the produced bundle and compares them with the recorded baseline, and a
minified on-device build SHALL pass the regression checklist before a release is
published.

#### Scenario: Local rate check

- **WHEN** a release bundle is built locally
- **THEN** the three R8 rates can be read from the bundle's metadata without waiting for a store report, and are compared against the baseline

#### Scenario: Minified device smoke test

- **WHEN** the minified release-like build is installed on a device
- **THEN** the map, proximity alerts, background monitoring, widget, preferences backup and Automotive entry point all function without crashes

### Requirement: Phone and automotive artifacts share one rule set

The phone and automotive release artifacts SHALL be configured from a single
shared set of R8 rules so that a change to the rule set applies to both.

#### Scenario: Shared configuration

- **WHEN** the R8 rule set is changed
- **THEN** both the phone and automotive release builds use the change, and the two rule files are not independent copies
