pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "switchboard"

include(":switchboard-annotations")
include(":switchboard-core")
include(":switchboard-android")
include(":switchboard-ksp")
include(":switchboard-compose")
include(":switchboard-shake")
include(":switchboard-firebase")
include(":switchboard-okhttp")
include(":sample")

