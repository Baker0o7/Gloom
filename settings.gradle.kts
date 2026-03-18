pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// Enable toolchain auto-provisioning
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

rootProject.name = "Gloom"
include(":app:android")
include(":app:desktop")

include(":api")
include(":shared")
include(":ui")

include(":lint:rules")