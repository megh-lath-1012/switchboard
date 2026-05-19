plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech.maven.publish)
    alias(libs.plugins.kover)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    api(project(":switchboard-annotations"))
    implementation(libs.coroutines.core)
    
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    
    coordinates(
        groupId = "services.pixelpulse",
        artifactId = "switchboard-core",
        version = "0.1.0"
    )
    
    pom {
        name.set("Switchboard Core")
        description.set("Type-safe feature flag runtime engine — KMP-ready, zero Android deps.")
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
