package com.elementeracoast.app.feature.boot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementeracoast.app.ui.brand.CoastBrandMark
import com.elementeracoast.app.ui.brand.CoastMuted
import com.elementeracoast.app.ui.brand.CoastPaper

@Composable
fun CoastBootScreen() {
    val darkSurface = MaterialTheme.colorScheme.background.luminance() < .35f
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(.72f))
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .fillMaxWidth(.88f)
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
        Spacer(Modifier.height(18.dp))
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
        Spacer(Modifier.height(28.dp))
        CircularProgressIndicator(
            modifier = Modifier.height(22.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.weight(1f))
    }
}
