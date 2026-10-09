import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipInputStream

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

val nanoOldSongVersion = "v1.3"
val nanoOldSongUrl = "https://github.com/Hansha2011/NanoOldSong/releases/download/$nanoOldSongVersion/NanoOldSongA-Regular.ttf"
val nanoOldSongTarget = layout.projectDirectory.file("src/main/res/font/nano_old_song_a_regular.ttf")

val chillHuoSongVersion = "HuoSongv1.000"
val chillHuoSongZipUrl = "https://github.com/Warren2060/ChillMovableType/releases/download/$chillHuoSongVersion/ChillHuoSong_F.zip"
val chillHuoSongTarget = layout.projectDirectory.file("src/main/res/font/chill_huo_song_regular.ttf")

android {
    namespace = "com.elementeracoast.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.elementeracoast.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 76
        versionName = "0.1.74-chatgpt-tool-roundtrip"
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

fun sha256(file: File): String = MessageDigest.getInstance("SHA-256")
    .digest(file.readBytes())
    .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

fun downloadFile(url: String, target: File) {
    URI(url).toURL().openStream().use { input: InputStream -> target.outputStream().use { output: OutputStream -> input.copyTo(output) } }
}

fun installDirectFont(url: String, target: File, displayName: String) {
    target.parentFile.mkdirs()
    downloadFile(url, target)
    check(target.length() > 0L) { "$displayName TTF download was empty: $url" }
    println("Installed $displayName into ${target.relativeTo(projectDir)} sha256=${sha256(target)}")
}

fun installZipFont(url: String, target: File, displayName: String, expectedZipSha256: String? = null, selectEntry: (String) -> Boolean) {
    val workDir = layout.buildDirectory.dir("reading-fonts").get().asFile
    workDir.mkdirs()
    val zipFile = File(workDir, url.substringAfterLast('/'))
    downloadFile(url, zipFile)
    val digest = sha256(zipFile)
    if (expectedZipSha256 != null) check(digest == expectedZipSha256) { "$displayName zip SHA-256 mismatch: $digest" }
    target.parentFile.mkdirs()
    var copied = false
    val seenFontEntries = mutableListOf<String>()
    ZipInputStream(zipFile.inputStream()).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name.substringAfterLast('/')
            val isFont = name.endsWith(".ttf", ignoreCase = true) || name.endsWith(".otf", ignoreCase = true)
            if (!entry.isDirectory && isFont) seenFontEntries += name
            if (!entry.isDirectory && !copied && selectEntry(name)) {
                target.outputStream().use { output -> zip.copyTo(output) }
                copied = true
            }
            zip.closeEntry()
        }
    }
    check(copied && target.length() > 0L) { "$displayName TTF/OTF was not found in $url; font entries=$seenFontEntries" }
    println("Installed $displayName into ${target.relativeTo(projectDir)} zipSha256=$digest fontSha256=${sha256(target)}")
}

val installReadingFonts by tasks.registering {
    description = "Download the three paper-reading fonts into Android resources."
    outputs.files(zhuqueFangsongTarget, nanoOldSongTarget, chillHuoSongTarget)
    doLast {
        installZipFont(zhuqueFangsongZipUrl, zhuqueFangsongTarget.asFile, "Zhuque Fangsong $zhuqueFangsongVersion", zhuqueFangsongZipSha256) { name ->
            name == "ZhuqueFangsong-Regular.ttf" || (name.contains("ZhuqueFangsong") && name.endsWith(".ttf", ignoreCase = true))
        }
        installDirectFont(nanoOldSongUrl, nanoOldSongTarget.asFile, "Nano Old Song A $nanoOldSongVersion")
                // Cmap glyph coverage is verified in CI before packaging.
        installZipFont(chillHuoSongZipUrl, chillHuoSongTarget.asFile, "Chill Huo Song $chillHuoSongVersion") { name ->
            val lower = name.lowercase()
            val isFont = lower.endsWith(".ttf") || lower.endsWith(".otf")
            isFont && !lower.contains("con")
        }
    }
}

tasks.matching { it.name in setOf("preBuild", "preDebugBuild", "preReleaseBuild") }.configureEach { dependsOn(installReadingFonts) }

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
