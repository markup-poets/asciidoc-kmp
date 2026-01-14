pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "asciidoc-Konverter"
include(":asciidoc-parser")
include(":ast-graphviz-export")
include(":document-processing")
include(":cli-app")
