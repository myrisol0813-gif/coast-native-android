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

val zhuqueFangsongVersion = "v0.212"
val zhuqueFangsongZipSha256 = "bb8b661a7643d2296a72d9d10530a00949419c4e527fb61783f73c2ba1a8c062"
val zhuqueFangsongZipUrl = "https://github.com/TrionesType/zhuque/releases/download/$zhuqueFangsongVersion/ZhuqueFangsong-$zhuqueFangsongVersion.zip"
val zhuqueFangsongTarget = layout.projectDirectory.file("src/main/res/font/zhuque_fangsong_regular.ttf")

android {
    namespace = "com.elementeracoast.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.elementeracoast.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 57
        versionName = "0.1.55-zhuque-fangsong-01"
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

val installZhuqueFangsong by tasks.registering {
    description = "Download the official Zhuque Fangsong preview font into Android resources."
    outputs.file(zhuqueFangsongTarget)
    doLast {
        val workDir = layout.buildDirectory.dir("zhuque-fangsong").get().asFile
        workDir.mkdirs()
        val zipFile = java.io.File(workDir, "ZhuqueFangsong-$zhuqueFangsongVersion.zip")
        java.net.URI(zhuqueFangsongZipUrl).toURL().openStream().use { input ->
            zipFile.outputStream().use { output -> input.copyTo(output) }
        }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(zipFile.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        check(digest == zhuqueFangsongZipSha256) { "Zhuque Fangsong zip SHA-256 mismatch: $digest" }

        val target = zhuqueFangsongTarget.asFile
        target.parentFile.mkdirs()
        var copied = false
        java.util.zip.ZipInputStream(zipFile.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val name = entry.name.substringAfterLast('/')
                if (!entry.isDirectory && !copied && (name == "ZhuqueFangsong-Regular.ttf" || (name.contains("ZhuqueFangsong") && name.endsWith(".ttf")))) {
                    target.outputStream().use { output -> zip.copyTo(output) }
                    copied = true
                }
            }
        }
        check(copied && target.length() > 0L) { "Zhuque Fangsong TTF was not found in $zhuqueFangsongZipUrl" }
        println("Installed Zhuque Fangsong $zhuqueFangsongVersion into ${target.relativeTo(projectDir)}")
    }
}

tasks.matching { it.name in setOf("preBuild", "preDebugBuild", "preReleaseBuild") }.configureEach {
    dependsOn(installZhuqueFangsong)
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
