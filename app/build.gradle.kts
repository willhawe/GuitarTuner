plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.whawe.guitartuner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.whawe.guitartuner"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storePath = providers.environmentVariable("GT_SIGN_STORE")
                .orElse(providers.gradleProperty("GT_SIGN_STORE")).orNull
            val storePasswordValue = providers.environmentVariable("GT_SIGN_STORE_PASS")
                .orElse(providers.gradleProperty("GT_SIGN_STORE_PASS")).orNull
            val keyAliasValue = providers.environmentVariable("GT_KEY_ALIAS")
                .orElse(providers.gradleProperty("GT_KEY_ALIAS")).orNull
            val keyPasswordValue = providers.environmentVariable("GT_KEY_PASS")
                .orElse(providers.gradleProperty("GT_KEY_PASS")).orNull

            if (storePath != null && storePasswordValue != null && keyAliasValue != null && keyPasswordValue != null) {
                storeFile = file(storePath)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseConfig = signingConfigs.getByName("release")
            val releaseTaskRequested = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }
            val releaseReady = releaseConfig.storeFile != null &&
                releaseConfig.storePassword != null &&
                releaseConfig.keyAlias != null &&
                releaseConfig.keyPassword != null
            if (!releaseReady && releaseTaskRequested) {
                throw GradleException("Release signing is not configured. Set GT_SIGN_STORE, GT_SIGN_STORE_PASS, GT_KEY_ALIAS, and GT_KEY_PASS.")
            }
            signingConfig = releaseConfig
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    // Removed Compose configuration since we're using traditional Views
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // Audio processing - using Android's built-in AudioRecord
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
