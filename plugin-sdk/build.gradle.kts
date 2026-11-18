plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

group = "org.markup-poet"
version = providers.gradleProperty("VERSION_NAME").get()

// Guest-side support for plugins written in Kotlin/Wasm: the memory bridge
// and the request/response plumbing of the plugin ABI.
kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmWasi { nodejs() }

    sourceSets {
        commonMain.dependencies {
            api(project(":plugin-api"))
        }
    }
}
