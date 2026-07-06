plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "org.markup-poet"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
    jvm()
    iosX64()
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
        }
    }
}
