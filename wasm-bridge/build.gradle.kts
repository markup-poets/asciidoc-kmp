plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "org.markup-poet"
version = "0.1.1"

kotlin {
    wasmJs {
        outputModuleName.set("asciidoc-kmp")
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":asciidoc-parser"))
            implementation(project(":html-renderer"))
        }
    }
}
