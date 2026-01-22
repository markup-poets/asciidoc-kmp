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
    
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":asciidoc-parser"))
                implementation(project(":html-renderer"))
            }
        }
    }
}
