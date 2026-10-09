import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("jacoco")
}

val enableCoverage = providers.gradleProperty("enableCoverage").getOrElse("false").toBoolean()

android {
    compileSdk = 37

    defaultConfig {
        applicationId = "org.blitzortung.android.app"
        minSdk = 23
        targetSdk = 37
        versionCode = 360
        versionName = "2.5.5"
        multiDexEnabled = false
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            enableUnitTestCoverage = enableCoverage
            enableAndroidTestCoverage = enableCoverage
        }
        create("perf") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += listOf("release")
            // The bundled external baseline profiles make installation fail on
            // devices with a work profile (INSTALL_BASELINE_PROFILE_FAILED).
            // They only cover library code, not the app's map rendering path.
            baselineProfile {
                ignoreFromAllExternalDependencies = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
            all {
                it.jvmArgs("-noverify")
            }
        }
    }

    testCoverage {
        jacocoVersion = "0.8.12"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    namespace = "org.blitzortung.android.app"
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.media)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.osmdroid.android)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.material)

    // Dagger2
    implementation(libs.dagger)
    implementation(libs.dagger.android)
    implementation(libs.dagger.android.support)
    implementation(libs.androidx.test.ext.junit.ktx)
    ksp(libs.dagger.android.processor)
    ksp(libs.dagger.compiler)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.ext.junit.ktx)

    // Kotlin Coroutines Testing
    testImplementation(libs.kotlinx.coroutines.test)

    // Turbine - Flow Testing
    testImplementation(libs.turbine)

    // AndroidX Arch Core Testing (LiveData/ViewModel testing)
    testImplementation(libs.androidx.arch.core.testing)

    // AndroidX Test Rules
    testImplementation(libs.androidx.test.rules)

    // Fragment Testing
    debugImplementation(libs.androidx.fragment.testing)

    // Instrumented Testing
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Compose Testing (if needed in future)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<Test> {
    jvmArgs("-Xmx4g", "-XX:MaxMetaspaceSize=1g")
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 2)
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates the JaCoCo coverage report from the debug unit test run."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    // Add files that should not be listed in the report (e.g. generated Files from dagger)
    val fileFilter = listOf("**/*Dagger.*")

    val kotlinDebugTree =
        fileTree("${layout.buildDirectory.get()}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
            exclude(fileFilter)
        }

    val javaDebugTree =
        fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") {
            exclude(fileFilter)
        }

    val mainSrc = "$projectDir/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(kotlinDebugTree, javaDebugTree))

    // Make sure the path is correct (if not run the unit tests and try find the .exec file that is generated after the unit tests are finished should be similar to that one)
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
    )
}

sonar {
    properties {
        property("sonar.junit.reportPaths", "build/test-results/testDebugUnitTest/")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
    }
}
