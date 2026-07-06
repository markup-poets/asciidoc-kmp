pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "asciidoc-Konverter"
include(":asciidoc-parser")
include(":asciidoc-asg")
include(":tck-adapter")
include(":ast-graphviz-export")
include(":document-processing")
include(":theming")
include(":examples")
include(":html-renderer")
include(":cli-app")
include(":html-cli")
include(":tck-quality-testing")
include(":antora-resolution")
include(":antora-assembler")
include(":plugin-api")
include(":plugin-engine")
include(":plugin-integration")
