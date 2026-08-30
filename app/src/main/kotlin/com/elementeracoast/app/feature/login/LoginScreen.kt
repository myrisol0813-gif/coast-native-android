package com.elementeracoast.app.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementeracoast.app.ui.brand.CoastBrandMark
import com.elementeracoast.app.ui.brand.CoastGold
import com.elementeracoast.app.ui.brand.CoastMuted
import com.elementeracoast.app.ui.brand.CoastPaper

@Composable
fun LoginScreen(
    password: String,
    onPasswordChange: (String) -> Unit,
    onEnter: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var mailboxHint by remember { mutableStateOf(false) }
    val darkSurface = MaterialTheme.colorScheme.background.luminance() < .35f
    val inputShape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(.66f))

        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .fillMaxWidth(.92f)
                .clip(RoundedCornerShape(if (darkSurface) 38.dp else 0.dp))
                .background(if (darkSurface) CoastPaper else MaterialTheme.colorScheme.background)
                .padding(horizontal = if (darkSurface) 10.dp else 0.dp, vertical = if (darkSurface) 14.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            CoastBrandMark(
                modifier = Modifier.fillMaxWidth(),
                separatorColor = CoastPaper
            )
        }

        Spacer(Modifier.height(17.dp))
        Text(
            "Elementera Coast",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(7.dp))
        Text(
            "沿海岸保存回声",
            color = if (darkSurface) MaterialTheme.colorScheme.onSurfaceVariant else CoastMuted,
            style = MaterialTheme.typography.bodyMedium,
            letterSpacing = 2.2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(54.dp))
        Row(
            modifier = Modifier
                .widthIn(max = 306.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = inputShape,
                    ambientColor = Color(0x1A24252B),
                    spotColor = Color(0x1224252B)
                )
                .background(MaterialTheme.colorScheme.surface, inputShape)
                .then(
                    if (focused) Modifier.border(1.5.dp, CoastGold.copy(alpha = .55f), inputShape)
                    else Modifier
                )
                .height(58.dp)
                .padding(start = 20.dp, end = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (password.isEmpty()) {
                    Text(
                        "输入海岸密码",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .62f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                BasicTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.4.sp
                    ),
                    cursorBrush = SolidColor(CoastGold),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }

            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = password.isNotBlank(), onClick = onEnter),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "进入海岸",
                    tint = if (password.isNotBlank()) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .42f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.height(25.dp))
        Text(
            "海岸信箱",
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable { mailboxHint = !mailboxHint }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (darkSurface) MaterialTheme.colorScheme.onSurfaceVariant else CoastMuted,
            style = MaterialTheme.typography.bodyMedium,
            letterSpacing = 1.5.sp
        )
        if (mailboxHint) {
            Spacer(Modifier.height(5.dp))
            Text(
                "Native v1 先保留入口；访客暗号与记名册仍走后续 P1。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))
    }
}
