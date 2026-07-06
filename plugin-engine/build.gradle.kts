plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "org.markup-poet"
version = "1.0.0"

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
            implementation(libs.chasm)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
