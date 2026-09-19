package com.elementeracoast.app.feature.serpentdesk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

enum class NativeApkInstallLaunch {
    InstallerOpened,
    PermissionSettingsOpened
}

fun installNativeApk(context: Context, download: NativeApkDownload): NativeApkInstallLaunch {
    val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
    val safeName = download.filename
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("""[^A-Za-z0-9._()\-]+"""), "_")
        .take(160)
        .ifBlank { "Elementera-Coast-update.apk" }
        .let { if (it.endsWith(".apk", ignoreCase = true)) it else "$it.apk" }
    val apkFile = File(updatesDir, safeName)
    apkFile.writeBytes(download.bytes)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        val settingsIntent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(settingsIntent)
        return NativeApkInstallLaunch.PermissionSettingsOpened
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
    return NativeApkInstallLaunch.InstallerOpened
}
