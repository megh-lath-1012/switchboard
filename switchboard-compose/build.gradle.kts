plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.vanniktech.maven.publish)
    alias(libs.plugins.kover)
}

android {
    namespace = "services.pixelpulse.switchboard.compose"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Targeted suppressions for known upstream lint detector crashes (AGP 8.7.3 + Compose BOM 2026.04.01).
    // The ComposableFlowOperatorDetector and NonNullableMutableLiveDataDetector both crash inside
    // LintDriver.kt:3740 when analyzing Compose source. These are NOT our code bugs.
    lint {
        abortOnError = false
        disable += "FlowOperatorInvokedInComposition"
        disable += "NullSafeMutableLiveData"
        // Third-party lint JAR uses an outdated lint API version — cannot be fixed in our code
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
    api(project(":switchboard-core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.rule)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.activity.compose)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("roborazzi.test.record", project.hasProperty("roborazzi.test.record"))
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    
    coordinates(
        groupId = "services.pixelpulse",
        artifactId = "switchboard-compose",
        version = "0.1.0"
    )
    
    pom {
        name.set("Switchboard Compose Debug UI")
        description.set("Jetpack Compose debug screen for inspecting and overriding feature flags.")
        url.set("https://github.com/megh-lath-1012/switchboard")
        
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        
        developers {
            developer {
                id.set("meghlath")
                name.set("Megh Lath")
                email.set("meghlath09@gmail.com")
                url.set("https://pixelpulse.services")
            }
        }
        
        scm {
            connection.set("scm:git:git://github.com/megh-lath-1012/switchboard.git")
            developerConnection.set("scm:git:ssh://github.com:megh-lath-1012/switchboard.git")
            url.set("https://github.com/megh-lath-1012/switchboard")
        }
    }
}
