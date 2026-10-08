pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Enable build cache for faster incremental builds.
// Use the default location (~/.gradle/caches/build-cache-1) so CI can persist
// and restore it between runs via gradle/actions/setup-gradle.
buildCache {
    local {
        isEnabled = true
    }
}

rootProject.name = "bo-android"
include(":app")
