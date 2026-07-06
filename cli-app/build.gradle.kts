plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.shadow.jar)
}

kotlin {
    jvmToolchain(17)
    jvm {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("org.markup.poet.asciidoc.processor.MainKt")
        }
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Main-Class"] = "org.markup.poet.asciidoc.processor.MainKt"
    }
}

kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":asciidoc-parser"))
                implementation(project(":ast-graphviz-export"))
                implementation(project(":document-processing"))
                implementation(project(":html-renderer"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
