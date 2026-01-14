plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("org.markup.poet.cli.MainKt")
        }
    }
    
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":asciidoc-parser"))
                implementation(project(":ast-graphviz-export"))
            }
        }
        
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
