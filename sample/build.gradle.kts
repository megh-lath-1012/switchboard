plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "services.pixelpulse.switchboard.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "services.pixelpulse.switchboard.sample"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        disable += "FlowOperatorInvokedInComposition"
        disable += "NullSafeMutableLiveData"
        disable += "ObsoleteLintCustomCheck"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":switchboard-android"))
    implementation(project(":switchboard-compose"))
    implementation(project(":switchboard-shake"))
    implementation(project(":switchboard-okhttp"))

    ksp(project(":switchboard-ksp"))

    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.coroutines.android)

    // MockWebServer for offline API simulation
    implementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    implementation(libs.okhttp)
}
