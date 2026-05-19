plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.maven.publish)
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

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    
    coordinates(
        groupId = "services.pixelpulse",
        artifactId = "switchboard-shake",
        version = "0.1.0"
    )
    
    pom {
        name.set("Switchboard Shake Detector")
        description.set("Shake-to-open gesture for launching the Switchboard debug screen.")
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
