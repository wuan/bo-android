// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.1.1" apply false
    id("com.android.library") version "9.1.1" apply false
    id("com.android.legacy-kapt") version "9.1.1" apply false
    id("org.sonarqube") version "7.2.3.7755"
//    id("io.gitlab.arturbosch.detekt") version "2.0.0-alpha.2"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
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

subprojects {
//    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

// detekt {
//     buildUponDefaultConfig = true
//     allRules = false
//     config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
//     baseline = file("$rootDir/config/detekt/baseline.xml")
// }

    ktlint {
        android.set(true)
        ignoreFailures.set(true)
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
        }
    }
}

// Task to run all static analysis checks
tasks.register("staticAnalysis") {
    group = "verification"
    description = "Runs all static analysis tools (detekt, ktlint, lint)"
    dependsOn("detekt", "ktlintCheck", ":app:lint")
}
