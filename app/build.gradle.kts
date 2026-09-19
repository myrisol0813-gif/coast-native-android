plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val signingPath = providers.environmentVariable("COAST_ANDROID_KEYSTORE_PATH")
val signingStorePassword = providers.environmentVariable("COAST_ANDROID_KEYSTORE_PASSWORD")
val signingAlias = providers.environmentVariable("COAST_ANDROID_KEY_ALIAS")
val signingKeyPassword = providers.environmentVariable("COAST_ANDROID_KEY_PASSWORD")
val stableSigningConfigured = listOf(signingPath, signingStorePassword, signingAlias, signingKeyPassword).all { it.isPresent }

android {
    namespace = "com.elementeracoast.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.elementeracoast.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 54
        versionName = "0.1.52-native-mailbox-iso-hotfix-01"
        buildConfigField("String", "COAST_API_BASE_URL", "\"https://app.elementeracoast.com\"")
    }

    signingConfigs {
        if (stableSigningConfigured) {
            create("coastStable") {
                storeFile = file(signingPath.get())
                storePassword = signingStorePassword.get()
                keyAlias = signingAlias.get()
                keyPassword = signingKeyPassword.get()
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            if (stableSigningConfigured) signingConfig = signingConfigs.getByName("coastStable")
        }
        release {
            isMinifyEnabled = false
            if (stableSigningConfigured) signingConfig = signingConfigs.getByName("coastStable")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/DEPENDENCIES")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}