plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech.maven.publish)
    alias(libs.plugins.kover)
}

dependencies {
    implementation(project(":switchboard-annotations"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlin.compile.testing.ksp)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        allWarningsAsErrors = true
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin").configure {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-opt-in=com.google.devtools.ksp.KspExperimental",
            "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi"
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    
    coordinates(
        groupId = "services.pixelpulse",
        artifactId = "switchboard-ksp",
        version = "0.1.0"
    )
    
    pom {
        name.set("Switchboard KSP Processor")
        description.set("KSP processor generating type-safe flag accessors and registry.")
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
