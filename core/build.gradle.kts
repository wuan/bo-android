import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
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

    namespace = "org.blitzortung.android"
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.osmdroid.android)

    // Dagger2
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.arch.core.testing)
}

tasks.withType<Test> {
    jvmArgs("-Xmx4g", "-XX:MaxMetaspaceSize=1g")
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 2)
}
