plugins {
    id("com.android.library") version "9.1.0"
    id("org.jetbrains.kotlin.android") version "2.4.10"
}

android {
    namespace = "ai.andalus.jse.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isReturnDefaultValues = false
    }
}

dependencies {
    implementation("com.google.ar:core:1.57.0")

    testImplementation("junit:junit:4.13.2")
}
