import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "org.markup.poet"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
    jvm()
    android {
        namespace = "org.markup.poet.tck"
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

    sourceSets {
        commonMain.dependencies {
            // Dependencies on library modules for testing
            implementation(project(":asciidoc-parser"))
            implementation(project(":html-renderer"))
            implementation(project(":document-processing"))
            implementation(project(":ast-graphviz-export"))
            
            // JSON serialization for fixture loading
            implementation(libs.kotlinx.serialization.json)

            // Multiplatform time and date
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
        }
        
        jvmMain {
            dependencies {
                // JGit for git operations on JVM
                implementation("org.eclipse.jgit:org.eclipse.jgit:7.6.0.202603022253-r")
                // Coroutines for async operations
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.property)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
    }
}

// The official TCK harness is the source of truth for conformance:
// run `./run-official-tck.sh` or `./gradlew :tck-adapter:officialTck`.
// The jvmTest fixture replay below is the fast inner development loop.
// This module is a strict gate: test failures fail the build. Genuinely
// unimplemented features are marked with @Ignore (reason in the test name/KDoc),
// never left failing.

tasks.named<Test>("jvmTest") {
    // All TckConfig default paths ("tck-quality-testing/...") are repo-root-relative;
    // running from the module dir used to create a nested tck-quality-testing/tck-quality-testing/.
    workingDir = rootDir
}

tasks.named<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest>("linuxX64Test") {
    // Same repo-root-relative path convention as jvmTest above.
    workingDir = rootDir.absolutePath
    // Native binaries have no classpath; ResourceLoader.linux.kt resolves
    // fixture resources under $TCK_ROOT/tck-quality-testing/src/*/resources.
    environment("TCK_ROOT", rootDir.absolutePath)
}
