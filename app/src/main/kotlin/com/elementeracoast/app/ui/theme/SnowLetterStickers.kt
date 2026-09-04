package com.elementeracoast.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.elementeracoast.app.R

/**
 * Stable decorative anchor points for Snow Letter sticker assets.
 *
 * Stickers are deliberately kept out of feature/business layout code. A feature
 * only chooses a semantic slot; size, offset, opacity, and stacking live here.
 */
enum class SnowLetterStickerSlot {
    MessageTopRight,
    ComposerTopRight,
    EmptyStateBottomRight,
    FeatureCornerTopRight
}

/**
 * Curated repository-owned assets that are safe to place through Snow Letter.
 *
 * Keep this list intentionally small. Real illustration PNG/WebP assets belong
 * in Android resources and should be added here rather than referenced directly
 * from feature screens.
 */
enum class SnowLetterStickerAsset {
    PaperPencil,
    WolfMark
}

@Immutable
private data class SnowLetterStickerSpec(
    val alignment: Alignment,
    val offsetX: Dp,
    val offsetY: Dp,
    val size: Dp,
    val alphaMultiplier: Float,
    val zIndex: Float
)

@Immutable
private data class SnowLetterStickerAssetSpec(
    val drawableRes: Int,
    val rotationZ: Float,
    val contentPadding: Dp
)

private fun stickerSpec(slot: SnowLetterStickerSlot): SnowLetterStickerSpec = when (slot) {
    SnowLetterStickerSlot.MessageTopRight -> SnowLetterStickerSpec(
        alignment = Alignment.TopEnd,
        offsetX = 8.dp,
        offsetY = (-18).dp,
        size = 54.dp,
        alphaMultiplier = .96f,
        zIndex = 2f
    )
    SnowLetterStickerSlot.ComposerTopRight -> SnowLetterStickerSpec(
        alignment = Alignment.TopEnd,
        offsetX = (-54).dp,
        offsetY = (-24).dp,
        size = 46.dp,
        alphaMultiplier = .90f,
        zIndex = 2f
    )
    SnowLetterStickerSlot.EmptyStateBottomRight -> SnowLetterStickerSpec(
        alignment = Alignment.BottomEnd,
        offsetX = (-14).dp,
        offsetY = (-12).dp,
        size = 76.dp,
        alphaMultiplier = .92f,
        zIndex = 1f
    )
    SnowLetterStickerSlot.FeatureCornerTopRight -> SnowLetterStickerSpec(
        alignment = Alignment.TopEnd,
        offsetX = (-12).dp,
        offsetY = 10.dp,
        size = 56.dp,
        alphaMultiplier = .84f,
        zIndex = 1f
    )
}

private fun stickerAssetSpec(asset: SnowLetterStickerAsset): SnowLetterStickerAssetSpec = when (asset) {
    SnowLetterStickerAsset.PaperPencil -> SnowLetterStickerAssetSpec(
        drawableRes = R.drawable.ic_coast_new_chat,
        rotationZ = -5f,
        contentPadding = 8.dp
    )
    SnowLetterStickerAsset.WolfMark -> SnowLetterStickerAssetSpec(
        drawableRes = R.drawable.ic_coast_wolf,
        rotationZ = 4f,
        contentPadding = 7.dp
    )
}

/**
 * Draws one repository-owned visual as a restrained paper sticker.
 *
 * Feature screens never reference drawable resources directly. When the final
 * Snow Letter illustration pack is imported, only this mapping needs to change.
 */
@Composable
fun SnowLetterSticker(
    asset: SnowLetterStickerAsset,
    modifier: Modifier = Modifier
) {
    val spec = stickerAssetSpec(asset)
    val shape = RoundedCornerShape(if (asset == SnowLetterStickerAsset.PaperPencil) 12.dp else 16.dp)
    val tint = when (asset) {
        SnowLetterStickerAsset.PaperPencil -> MaterialTheme.colorScheme.primary.copy(alpha = .88f)
        SnowLetterStickerAsset.WolfMark -> MaterialTheme.colorScheme.onSurface.copy(alpha = .78f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(rotationZ = spec.rotationZ)
            .shadow(elevation = 1.5.dp, shape = shape, clip = false)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .96f), shape)
            .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .28f), shape)
            .padding(spec.contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(spec.drawableRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Places decorative Snow Letter content without changing the surrounding
 * feature geometry or behavior.
 *
 * The anchor is invisible outside the Snow Letter preset. Decoration opacity
 * follows the existing wardrobe decoration slider, so sticker tuning does not
 * need another settings path.
 */
@Composable
fun BoxScope.SnowLetterStickerAnchor(
    slot: SnowLetterStickerSlot,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val enabled = LocalCoastAppearance.current.preset.usesSnowLetterDecorations()
    if (!enabled || !visible) return

    val spec = stickerSpec(slot)
    val wardrobeAlpha = LocalSnowLetterVisuals.current.decorationAlpha.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .align(spec.alignment)
            .offset(x = spec.offsetX, y = spec.offsetY)
            .size(spec.size)
            .alpha((wardrobeAlpha * spec.alphaMultiplier).coerceIn(0f, 1f))
            .zIndex(spec.zIndex)
            .clearAndSetSemantics { }
            .then(modifier),
        contentAlignment = Alignment.Center,
        content = content
    )
}
