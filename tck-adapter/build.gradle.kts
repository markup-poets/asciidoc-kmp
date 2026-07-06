plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.shadow.jar)
}

group = "org.markup-poet"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":asciidoc-parser"))
            implementation(project(":asciidoc-asg"))
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    // Version-independent name: run-official-tck.sh and CI invoke this jar by path.
    archiveFileName.set("tck-adapter-all.jar")
    manifest {
        attributes["Main-Class"] = "org.markup.poet.tck.adapter.MainKt"
    }
}

tasks.register<Exec>("officialTck") {
    group = "verification"
    description = "Run the official AsciiDoc TCK harness against the adapter"
    dependsOn("shadowJar")
    workingDir = rootDir
    commandLine("./run-official-tck.sh")
}
