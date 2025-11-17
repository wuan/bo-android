// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.1" apply false
    id("com.android.library") version "8.13.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.sonarqube") version "6.2.0.5505"
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

sonar {
    properties {
        property("sonar.projectKey", "wuan_bo-android")
        property("sonar.organization", "wuan")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.androidLint.reportPaths", "app/build/reports/lint-results-debug.xml")
    }
}
