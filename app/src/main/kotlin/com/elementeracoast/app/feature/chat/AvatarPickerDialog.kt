package com.elementeracoast.app.feature.chat

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun AvatarPickerDialog(
    onDismiss: () -> Unit,
    onPickLocalImage: () -> Unit,
    onReset: () -> Unit,
    onFutureSync: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Myri 头像") },
        text = {
            Text("当前只改这台 Native 小屋里的本地头像，不上传、不写 profile。")
        },
        confirmButton = {
            TextButton(onClick = onPickLocalImage) { Text("选择本地图片") }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Column {
                TextButton(onClick = onReset) { Text("恢复默认") }
                TextButton(onClick = onFutureSync) { Text("未来同步到 profile") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}
