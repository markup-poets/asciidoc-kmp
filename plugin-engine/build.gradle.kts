plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "org.markup-poet"
version = providers.gradleProperty("VERSION_NAME").get()

kotlin {
    jvmToolchain(17)
    jvm()
    android {
        namespace = "org.markup.poet.plugin.engine"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                }
            }
        }
    }
    iosArm64()
    iosSimulatorArm64()
    linuxX64()
    macosArm64()
    // The browser brings its own WebAssembly engine; see BrowserPluginEngine.
    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(project(":plugin-api"))
        }

        // Every target Chasm (the pure-Kotlin WASM runtime) is published for.
        val chasmMain = create("chasmMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.chasm)
            }
        }
        jvmMain.get().dependsOn(chasmMain)
        androidMain.get().dependsOn(chasmMain)
        nativeMain.get().dependsOn(chasmMain)

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
