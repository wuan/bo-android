[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=wuan_bo-android&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=wuan_bo-android)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=wuan_bo-android&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=wuan_bo-android)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=wuan_bo-android&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=wuan_bo-android)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=wuan_bo-android&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=wuan_bo-android)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=wuan_bo-android&metric=coverage)](https://sonarcloud.io/summary/new_code?id=wuan_bo-android)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/wuan/bo-android/badge)](https://scorecard.dev/viewer/?uri=github.com/wuan/bo-android)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/wuan/bo-android)

# Blitzortung Lightning Monitor Android app

The Android App *Blitzortung Lightning Monitor* built from these sources visualizes lightning data provided by the blitzortung.org network.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/org.blitzortung.android.app/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=org.blitzortung.android.app)

Please have a look at [the documentation](https://blitzortung.tryb.de) for more information on usage.

## Development builds

The app defines three build variants:

| Variant | Minified (R8) | Debuggable | Signing | Use for |
| --- | --- | --- | --- | --- |
| `debug` | no | yes | debug key | daily development, debugging |
| `perf` | yes | no | debug key | performance-critical testing, profiling |
| `release` | yes | no | release key | publishing |

`debug` builds run without R8 optimization and are installed without ahead-of-time compilation, so heavy map rendering (especially zooming with global data) feels noticeably slower than production.

The `perf` variant exists to close that gap: it inherits the `release` build type (R8 minification, resource shrinking, optimization) but is signed with the debug key so it can be installed over the `debug` build. It is intentionally **not** debuggable, which is what allows it to match release performance.

External baseline profiles from library dependencies are excluded on `perf` to avoid `INSTALL_BASELINE_PROFILE_FAILED` on devices with a work profile. They only cover library code, so they do not affect the app's map rendering performance.

To use it:

* In Android Studio select `perf` under **Build > Select Build Variant** and Run, or
* From the command line:

```bash
./gradlew installPerf
```

Because `perf` and `debug` share the same application ID and signing key, they overwrite each other. The `release` variant uses a separate key and must be uninstalled first.

## Available translations

 * Czech
 * Dutch
 * English
 * French
 * German
 * Hungarian
 * Italian
 * Polish
 * Slovak
 * Russian
 * Spanish

## Translation offers are welcome

We are still looking forward to volunteers preparing a translation for the following languages:

 * yours?

Translations to other languages are welcome as well. Please contact us via `blitzortung at tryb.de`

## User privacy

This app can use device location in order to show the users location on the map and for generating warning view and alerts for lightning activity nearby.

The lightning warning feature **can also be used without device location permissions** as the location can also be set manually (via preferences but also via long pressing the desired position on the map). You will still be able to use the warning feature without allowing location permissions.

In order to have a more efficient data transfer with high resolution lightning data the app only fetches the last 10 minutes of activity in an area of 5° x 5° rounded to five degrees.

Although this data is derived from the devices location or the map area currently visible it can not be used to track users.
Here is why:

  1. **Granularity**: A 5x5 degree area is extremely large.
     * **Latitude**: 5 degrees covers approximately 555 km (345 miles).
     * **Longitude**: At the equator, 5 degrees is also ~555 km. At 50° latitude (e.g., Central Europe/Canada), it is still ~355 km wide.
     * This resolution is sufficient to identify a broad region (e.g., "Central Europe" or "US West Coast") but is completely insufficient to pinpoint a city, let alone a specific person or address.
  2. **Anonymization**: The calculation `(coordinate / scale).toInt()` effectively quantizes the location. All users within that massive 5x5 degree square report the exact same grid index (e.g., x=2, y=10). It is impossible to distinguish between two users within that same ~300,000 km² area based solely on these coordinates. On top of that, there is no data provided which would allow to link subsequent requests coming from the same app.

**Conclusion**: The coordinates generated by `LocalData.update()` are too coarse for individual tracking. They serve only to download lightning data for the correct general region of the world.
