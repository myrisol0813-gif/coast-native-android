package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

@Composable
fun ThemeSwitcher(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier.size(38.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) {
            val y = size.height / 2
            val radius = 2.6.dp.toPx()
            drawCircle(quiet.copy(alpha = 0.38f), radius, Offset(size.width * 0.22f, y))
            drawCircle(primary.copy(alpha = 0.86f), radius, Offset(size.width * 0.50f, y))
            drawCircle(quiet.copy(alpha = 0.62f), radius, Offset(size.width * 0.78f, y))
        }
    }
}
