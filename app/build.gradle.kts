plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

import java.io.FileInputStream
import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.iris.iriscode"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.iris.iriscode"
        minSdk = 26
        targetSdk = 28
        versionCode = project.property("VERSION_CODE").toString().toInt()
        versionName = "${project.property("VERSION_MAJOR")}.${project.property("VERSION_MINOR")}.${project.property("VERSION_PATCH")}"
    }

    signingConfigs {
        create("release") {
            storeFile = keystoreProperties.getProperty("storeFile")?.let {
                rootProject.file(it)
            }
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Security
    implementation(libs.security.crypto)
    
    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // OkHttp + SSE
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)

    // Lottie
    implementation(libs.lottie)
    
    // Icons
    implementation(libs.compose.icons.lucide)

    // Markdown
    implementation(libs.compose.markdown)

    // Diff
    implementation(libs.java.diff.utils)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.documentfile)
    
    // Termux Terminal (vendored from github.com/termux/termux-app)
    implementation(project(":termux:emulator"))
    implementation(project(":termux:view"))
}
