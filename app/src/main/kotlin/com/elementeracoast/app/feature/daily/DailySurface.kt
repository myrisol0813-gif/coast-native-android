package com.elementeracoast.app.feature.daily

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun DailySurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val base = modifier
        .fillMaxWidth()
        .shadow(elevation = 2.dp, shape = shape, clip = false)
        .background(MaterialTheme.colorScheme.surfaceVariant, shape)
    Column(
        modifier = (if (onClick == null) base else base.clickable(onClick = onClick))
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) { content() }
}

@Composable
internal fun DailyPrimaryButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = shape, clip = false)
            .background(MaterialTheme.colorScheme.primary, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun DailyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    minLines: Int = 1,
    maxLines: Int = 1
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (minLines > 3) 160.dp else 54.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            minLines = minLines,
            maxLines = maxLines,
            decorationBox = { inner ->
                if (value.isBlank() && placeholder.isNotBlank()) {
                    Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
                inner()
            }
        )
    }
}

@Composable
internal fun DailyAvatarStrip(
    profileUri: String,
    myriUri: String,
    onProfileClick: () -> Unit,
    onMyriClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = shape, clip = false)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        DailyAvatarChip("小寒", profileUri, "寒", onProfileClick)
        DailyAvatarChip("Myri", myriUri, "M", onMyriClick)
    }
}

@Composable
private fun DailyAvatarChip(
    label: String,
    uri: String,
    fallback: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DailyAvatar(uri, fallback, 26.dp)
        Spacer(Modifier.size(7.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal)
        )
    }
}

@Composable
internal fun DailyAvatar(uri: String, fallback: String, size: Dp = 54.dp) {
    val bitmap = rememberDailyBitmap(uri)
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(fallback, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun DailyCover(uri: String, onClick: () -> Unit) {
    val bitmap = rememberDailyBitmap(uri)
    val shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "碳硅圈封面",
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = shape, clip = false)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
            contentScale = ContentScale.FillWidth
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .shadow(elevation = 2.dp, shape = shape, clip = false)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text("轻触设置封面", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun rememberDailyBitmap(uri: String): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, uri) {
        value = if (uri.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri)).use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
            }.getOrNull()
        }
    }.value
}
