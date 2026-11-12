import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.tapbot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tapbot"
        minSdk = 26
        targetSdk = 35
        versionCode = 240
        versionName = "2.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val localProps = Properties().apply {
                val localPropsFile = rootProject.file("local.properties")
                if (localPropsFile.exists()) {
                    localPropsFile.inputStream().use { load(it) }
                }
            }
            val keystoreFile = rootProject.file("keystore/release.keystore")
            val ksPassword = System.getenv("TAPBOT_KEYSTORE_PASSWORD")
                ?: localProps.getProperty("TAPBOT_KEYSTORE_PASSWORD")
            val ksAlias = System.getenv("TAPBOT_KEY_ALIAS")
                ?: localProps.getProperty("TAPBOT_KEY_ALIAS")
            val ksKeyPassword = System.getenv("TAPBOT_KEY_PASSWORD")
                ?: localProps.getProperty("TAPBOT_KEY_PASSWORD")
            if (keystoreFile.exists() &&
                !ksPassword.isNullOrBlank() &&
                !ksAlias.isNullOrBlank() &&
                !ksKeyPassword.isNullOrBlank()
            ) {
                storeFile = keystoreFile
                storePassword = ksPassword
                keyAlias = ksAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile?.exists() == true) {
                signingConfig = releaseConfig
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:security"))
    implementation(project(":core:network"))
    implementation(project(":core:logging"))
    implementation(project(":core:runner"))
    implementation(project(":core:ui"))
    implementation(project(":feature:catalog"))
    implementation(project(":feature:botdetail"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
