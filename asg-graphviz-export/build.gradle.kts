import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "org.markup-poet"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
    jvm()
    android {
        namespace = "org.markup.poet.asciidoc.export"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            // Dependency on the core library module
            implementation(project(":asciidoc-parser"))
        }

        val appleMain by creating {
            dependsOn(commonMain.get())
        }

        macosArm64Main.get().dependsOn(appleMain)
        iosX64Main.get().dependsOn(appleMain)
        iosArm64Main.get().dependsOn(appleMain)
        iosSimulatorArm64Main.get().dependsOn(appleMain)

        val linuxMain by creating {
            dependsOn(commonMain.get())
        }

        linuxX64Main.get().dependsOn(linuxMain)

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.property)
            // Also depend on the library for testing
            implementation(project(":asciidoc-parser"))
        }
    }
}