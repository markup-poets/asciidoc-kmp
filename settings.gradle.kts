pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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
include(":asg-graphviz-export")
include(":document-processing")
include(":html-renderer")
include(":cli-app")
include(":html-cli")
include(":tck-quality-testing")
include(":antora-resolution")
include(":antora-assembler")
include(":plugin-api")
include(":plugin-engine")
include(":plugin-integration")
include(":plugin-sdk")
include(":plugin-icons-fontawesome")
project(":plugin-icons-fontawesome").projectDir = file("plugins/icons-fontawesome")
include(":plugin-stem-mathjax")
project(":plugin-stem-mathjax").projectDir = file("plugins/stem-mathjax")
include(":plugin-video-embed")
project(":plugin-video-embed").projectDir = file("plugins/video-embed")
include(":standard-plugins")
include(":wasm-bridge")
