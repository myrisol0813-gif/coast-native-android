package com.elementeracoast.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SnowLetterSurfaceRole {
    AssistantBubble,
    UserBubble,
    ComposerField,
    ComposerButton,
    ActionButton,
    StatusCard,
    DogtalkCard,
    DogtalkField
}

@Composable
fun SnowLetterSurface(
    modifier: Modifier = Modifier,
    role: SnowLetterSurfaceRole,
    fallbackColor: Color,
    fallbackShape: Shape = RoundedCornerShape(18.dp),
    fallbackBorder: BorderStroke? = null,
    fallbackElevation: Dp = 0.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val appearance = LocalCoastAppearance.current
    val isMessageSurface = role == SnowLetterSurfaceRole.AssistantBubble || role == SnowLetterSurfaceRole.UserBubble
    val messageAlpha = appearance.messageSurfaceAlpha.coerceIn(.55f, 1f)
    val isLetter = role == SnowLetterSurfaceRole.AssistantBubble && appearance.paperMode == CoastPaperMode.Wave

    // The letter is a straight rounded surface: no skew, irregular cut path, or protruding edges.
    // A very soft shadow and thin edge preserve its paper feeling without competing with text.
    val surfaceModifier = if (isLetter) {
        val paperShape = RoundedCornerShape(17.dp)
        val shadowTint = Color(0xFF8495A6)
        Modifier
            .shadow(
                elevation = 2.dp,
                shape = paperShape,
                clip = false,
                ambientColor = shadowTint.copy(alpha = .10f * messageAlpha),
                spotColor = shadowTint.copy(alpha = .07f * messageAlpha)
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = messageAlpha), paperShape)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .23f * messageAlpha), paperShape)
    } else {
        val color = if (isMessageSurface) fallbackColor.copy(alpha = fallbackColor.alpha * messageAlpha) else fallbackColor
        Modifier
            .optionalShadow(fallbackElevation, fallbackShape)
            .background(color, fallbackShape)
            .then(if (fallbackBorder != null) Modifier.border(fallbackBorder, fallbackShape) else Modifier)
    }

    Box(
        modifier = modifier.then(surfaceModifier),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun snowLetterInnerPadding(role: SnowLetterSurfaceRole): PaddingValues =
    if (role == SnowLetterSurfaceRole.AssistantBubble) {
        PaddingValues(horizontal = 13.dp, vertical = 10.dp)
    } else {
        PaddingValues(0.dp)
    }

@Composable
fun snowLetterSheetContainerColor(): Color = MaterialTheme.colorScheme.background

@Composable
fun snowLetterComposerGlyphColor(fallback: Color): Color = fallback

private fun Modifier.optionalShadow(elevation: Dp, shape: Shape): Modifier =
    if (elevation.value > 0f) shadow(elevation, shape, clip = false) else this
