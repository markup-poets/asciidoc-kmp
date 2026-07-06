plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    jvmToolchain(17)
    androidLibrary {
        namespace = "org.markup.poet.examples"
        compileSdk = 36
        minSdk = 24
    }
    
    jvm()
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    linuxX64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":theming"))
                implementation(libs.kotlin.test)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
