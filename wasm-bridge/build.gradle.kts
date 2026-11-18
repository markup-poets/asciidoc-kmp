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
            implementation(project(":plugin-integration"))
        }
        wasmJsTest.dependencies {
            implementation(libs.kotlin.test)
            // Supplies the standard plugin binaries to the tests; the bridge itself fetches them.
            implementation(project(":standard-plugins"))
        }
    }
}

// The standard plugins are not embedded in the bundle (that would more than
// quadruple it for every page). They ship as separate files in `plugins/`
// next to it and are fetched by enableStandardPlugins().
val standardPluginFiles = tasks.register<Copy>("standardPluginFiles") {
    listOf("icons-fontawesome", "stem-mathjax", "video-embed").forEach { name ->
        dependsOn(":plugin-$name:compileProductionExecutableKotlinWasmWasiOptimize")
        from(rootProject.layout.projectDirectory.dir("plugins/$name/build/compileSync/wasmWasi/main/productionExecutable/optimized")) {
            include("*.wasm")
            rename { "$name.wasm" }
        }
    }
    into(layout.buildDirectory.dir("dist/wasmJs/productionExecutable/plugins"))
}

tasks.named("wasmJsBrowserDistribution") {
    finalizedBy(standardPluginFiles)
}
