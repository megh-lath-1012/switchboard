plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
    alias(libs.plugins.kover) apply false
}

allprojects {
    if (project.hasProperty("signing.keyId")) {
        val keyId = project.property("signing.keyId") as String
        if (keyId.length == 16) {
            project.setProperty("signing.keyId", keyId.substring(8))
        }
    }
    if (project.hasProperty("signingInMemoryKeyId")) {
        val keyId = project.property("signingInMemoryKeyId") as String
        if (keyId.length == 16) {
            project.setProperty("signingInMemoryKeyId", keyId.substring(8))
        }
    }
}

