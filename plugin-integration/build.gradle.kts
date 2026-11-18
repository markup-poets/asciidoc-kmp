plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "org.markup-poet"
version = providers.gradleProperty("VERSION_NAME").get()

kotlin {
    jvmToolchain(17)
    jvm()
    android {
        namespace = "org.markup.poet.plugin.integration"
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

    sourceSets {
        commonMain.dependencies {
            api(project(":plugin-api"))
            api(project(":plugin-engine"))
            api(project(":asciidoc-parser"))
            api(project(":asciidoc-asg"))
            api(project(":html-renderer"))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(project(":standard-plugins"))
        }
    }
}
