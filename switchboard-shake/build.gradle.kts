plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kover)
}

android {
    namespace = "services.pixelpulse.switchboard.shake"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Targeted suppressions for known upstream lint detector crashes (AGP 8.7.3 + Compose BOM 2026.04.01).
    lint {
        abortOnError = false
        disable += "FlowOperatorInvokedInComposition"
        disable += "NullSafeMutableLiveData"
        disable += "ObsoleteLintCustomCheck"
    }
}

kotlin {
    explicitApi()
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    api(project(":switchboard-compose"))
    
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
