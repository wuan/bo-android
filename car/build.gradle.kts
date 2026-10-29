import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.android.library")
}

android {
    compileSdk = 37

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
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

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    namespace = "org.blitzortung.android.car"
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.car.app)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.car.app.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    jvmArgs("-Xmx4g", "-XX:MaxMetaspaceSize=1g")
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 2)
}
