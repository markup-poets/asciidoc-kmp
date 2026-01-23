plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("org.markup.poet.html.cli.MainKt")
        }
    }
    
    // Native macOS target
    macosArm64 {
        binaries {
            executable {
                entryPoint = "org.markup.poet.html.cli.main"
                baseName = "html-renderer"
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":asciidoc-parser"))
                implementation(project(":html-renderer"))
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        
        val jvmMain by getting
        
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        
        val macosArm64Main by getting {
            dependsOn(nativeMain)
        }
    }
}
