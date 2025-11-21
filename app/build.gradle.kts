import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("jacoco")
}

android {
    compileSdk = 35

    defaultConfig {
        applicationId = "org.blitzortung.android.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 346
        versionName = "2.4.5"
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
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            apiVersion.set(KotlinVersion.KOTLIN_2_2)
        }
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

    buildToolsVersion = "35.0.0"
}

val daggerVersion = "2.57.2"

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.media:media:1.7.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.android.material:material:1.13.0")

    // Dagger2
    implementation("com.google.dagger:dagger:$daggerVersion")
    implementation("com.google.dagger:dagger-android:$daggerVersion")
    implementation("com.google.dagger:dagger-android-support:$daggerVersion")
    implementation("androidx.test.ext:junit-ktx:1.3.0")
    kapt("com.google.dagger:dagger-android-processor:$daggerVersion")
    kapt("com.google.dagger:dagger-compiler:$daggerVersion")
    compileOnly("javax.annotation:jsr250-api:1.0")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("androidx.test.ext:junit-ktx:1.3.0")

    // Kotlin Coroutines Testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // Turbine - Flow Testing
    testImplementation("app.cash.turbine:turbine:1.2.1")

    // AndroidX Arch Core Testing (LiveData/ViewModel testing)
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // AndroidX Test Rules
    testImplementation("androidx.test:rules:1.7.0")

    // Fragment Testing
    debugImplementation("androidx.fragment:fragment-testing:1.8.9")

    // Instrumented Testing
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.3.0")
    androidTestImplementation("io.mockk:mockk-android:1.14.6")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // Compose Testing (if needed in future)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.9.5")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.9.5")
}

kapt {
    includeCompileClasspath = false
}

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    // Add files that should not be listed in the report (e.g. generated Files from dagger)
    val fileFilter = listOf("**/*Dagger.*")

    val kotlinDebugTree =
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }

    val mainSrc = "$projectDir/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(kotlinDebugTree))

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
