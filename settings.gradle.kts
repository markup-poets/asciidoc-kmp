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
include(":html-renderer")
include(":cli-app")
include(":tck-quality-testing")
