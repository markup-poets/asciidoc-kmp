plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "org.markup-poet"
version = providers.gradleProperty("VERSION_NAME").get()

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
