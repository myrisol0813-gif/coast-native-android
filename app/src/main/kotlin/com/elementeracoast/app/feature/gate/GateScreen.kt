package com.elementeracoast.app.feature.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.ui.brand.CoastBrandMarkAnimation
import com.elementeracoast.app.ui.brand.CoastMuted
import com.elementeracoast.app.ui.brand.rememberCoastGateMotion
import kotlinx.coroutines.launch

/**
 * Native Gate composition. The entrance animation is the Boot phase: unlike the
 * previous implementation, no separate static logo/spinner screen interrupts
 * the source PWA timing before the interactive Gate appears.
 */
@Composable
fun GateScreen(
    password: String,
    onPasswordChange: (String) -> Unit,
    onEnter: () -> Unit
) {
    val motion = rememberCoastGateMotion()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var mailboxState by remember { mutableStateOf(MailboxEntryState()) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = GateVisualTokens.ScreenHorizontalPadding)
    ) {
        val compact = maxHeight < GateVisualTokens.CompactHeightThreshold
        val markWidth = if (compact) GateVisualTokens.CompactBrandMarkWidth else GateVisualTokens.BrandMarkWidth
        val passwordGap = if (compact) GateVisualTokens.CompactTaglineToPassword else GateVisualTokens.TaglineToPassword
        val textOffsetPx = with(density) { 6.dp.toPx() }
        val formOffsetPx = with(density) { 10.dp.toPx() }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = GateVisualTokens.ContentVerticalShift)
                .widthIn(max = GateVisualTokens.ContentMaxWidth)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CoastBrandMarkAnimation(
                motion = motion,
                modifier = Modifier.width(markWidth),
                separatorColor = MaterialTheme.colorScheme.background
            )

            Spacer(Modifier.height(GateVisualTokens.MarkToTitle))
            Text(
                text = "Elementera Coast",
                modifier = Modifier.graphicsLayer {
                    alpha = motion.brand.coerceIn(0f, 1f)
                    translationY = textOffsetPx * (1f - motion.brand.coerceIn(0f, 1f))
                },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = GateVisualTokens.TitleSize,
                fontWeight = FontWeight(650),
                letterSpacing = GateVisualTokens.TitleLetterSpacing,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(GateVisualTokens.TitleToTagline))
            Text(
                text = "沿海岸保存回声",
                modifier = Modifier.graphicsLayer {
                    alpha = motion.tagline.coerceIn(0f, 1f)
                    translationY = textOffsetPx * (1f - motion.tagline.coerceIn(0f, 1f))
                },
                color = CoastMuted,
                fontSize = GateVisualTokens.TaglineSize,
                fontWeight = FontWeight(450),
                letterSpacing = GateVisualTokens.TaglineLetterSpacing,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(passwordGap))
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = motion.form.coerceIn(0f, 1f)
                    translationY = formOffsetPx * (1f - motion.form.coerceIn(0f, 1f))
                }
            ) {
                GatePasswordField(
                    password = password,
                    onPasswordChange = onPasswordChange,
                    onSubmit = onEnter,
                    enabled = motion.form >= .98f,
                    modifier = Modifier.width(GateVisualTokens.PasswordWidth)
                )
            }

            Spacer(Modifier.height(GateVisualTokens.PasswordToMailbox))
            Text(
                text = "海岸信箱",
                modifier = Modifier
                    .graphicsLayer { alpha = motion.form.coerceIn(0f, 1f) }
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(enabled = motion.form >= .98f) {
                        mailboxState = mailboxState.open()
                    }
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                color = CoastMuted,
                fontSize = GateVisualTokens.MailboxEntrySize,
                letterSpacing = GateVisualTokens.MailboxEntryLetterSpacing
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }

    MailboxEntryDialog(
        state = mailboxState,
        onDismiss = { mailboxState = mailboxState.close() },
        onNavigate = { page -> mailboxState = mailboxState.show(page) },
        onPlaceholderSubmit = {
            scope.launch {
                snackbarHostState.showSnackbar("海岸信箱后续接入；本轮不会发送或保存暗号。")
            }
        }
    )
}
