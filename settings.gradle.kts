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
include(":theming")
include(":examples")
include(":html-renderer")
include(":cli-app")
include(":html-cli")
include(":tck-quality-testing")
include(":antora-resolution")
include(":antora-assembler")
