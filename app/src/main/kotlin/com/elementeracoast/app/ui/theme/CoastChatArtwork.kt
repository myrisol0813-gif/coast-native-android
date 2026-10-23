package com.elementeracoast.app.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.R

/** Bundled artwork belongs to the existing preset; it has no separate saved state. */
internal data class CoastChatArtwork(
    val background: Int,
    val topFrame: Int,
    val bottomFrame: Int
)

private val StarryCoastArtwork = CoastChatArtwork(
    background = R.drawable.starry_coast_background,
    topFrame = R.drawable.starry_coast_top_frame,
    bottomFrame = R.drawable.starry_coast_bottom_frame
)

internal fun CoastThemePreset.chatArtwork(): CoastChatArtwork? =
    if (this == CoastThemePreset.StarryCoast) StarryCoastArtwork else null

@Composable
internal fun ChatArtworkBackground(artwork: CoastChatArtwork, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        Image(
            painter = painterResource(artwork.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            // Wide/short windows retain the entire moon and snake. In portrait,
            // anchor the original at the top and crop only the lower clouds.
            contentScale = if (maxWidth > maxHeight) ContentScale.Fit else ContentScale.FillWidth,
            alignment = Alignment.TopCenter
        )
    }
}

internal data class ChatArtworkLayout(
    val frameWidth: Float,
    val topScrollClearance: Float,
    val bottomScrollClearance: Float
)

internal fun chatArtworkLayout(widthDp: Float, heightDp: Float): ChatArtworkLayout {
    // Keep the complete frames proportional, including with an IME or in landscape.
    // Their visible silhouettes occupy at most 55% of a short viewport together.
    val frameWidth = minOf(widthDp, heightDp * 1.25f, 600f).coerceAtLeast(0f)
    return ChatArtworkLayout(
        frameWidth = frameWidth,
        // Measured alpha > 3 bounds of the unmodified originals. The nearly
        // transparent export fringe must not create large empty scroll margins.
        topScrollClearance = frameWidth * 450f / 2172f,
        bottomScrollClearance = frameWidth * 468f / 2039f
    )
}

/** Foreground images never own input; only the list's scrollable ends gain clearance. */
@Composable
internal fun ChatArtworkViewport(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(PaddingValues) -> Unit
) {
    val artwork = LocalCoastAppearance.current.preset.chatArtwork()
    if (artwork == null) {
        // Preserve the previous viewport and painting path for every other preset.
        Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
            content(PaddingValues(0.dp))
        }
        return
    }

    BoxWithConstraints(modifier = modifier.clipToBounds(), contentAlignment = Alignment.TopCenter) {
        val layout = chatArtworkLayout(maxWidth.value, maxHeight.value)
        content(
            PaddingValues(
                top = layout.topScrollClearance.dp,
                bottom = layout.bottomScrollClearance.dp
            )
        )
        // Draw after the real timeline, without padding or shrinking its viewport.
        Image(
            painter = painterResource(artwork.topFrame),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.TopCenter)
                .width(layout.frameWidth.dp).aspectRatio(2172f / 724f)
        )
        Image(
            painter = painterResource(artwork.bottomFrame),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.BottomCenter)
                .width(layout.frameWidth.dp).aspectRatio(2039f / 771f)
        )
    }
}
