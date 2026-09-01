package com.elementeracoast.app.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.feature.daily.DailyLanding
import com.elementeracoast.app.feature.memory.MemoryLanding

data class LandingCardSpec(
    val title: String,
    val subtitle: String
)

@Composable
internal fun FeatureLandingScreen(
    feature: FeatureDestination,
    onPlaceholder: (String) -> Unit
) {
    when (feature) {
        FeatureDestination.Daily -> DailyLanding(onPlaceholder)
        FeatureDestination.Memory -> MemoryLanding(onPlaceholder)
        FeatureDestination.Wolf -> CoastLandingCards(
            cards = listOf(
                LandingCardSpec("小狼窝", "Kryo 的视觉与偏好空间"),
                LandingCardSpec("头像与主题", "后续再接持久化设置"),
                LandingCardSpec("导入 / 导出", "诊断与搬运暂留后续")
            ),
            onPlaceholder = onPlaceholder
        )
        FeatureDestination.Desk -> CoastLandingCards(
            cards = listOf(
                LandingCardSpec("Myri 画像", "头像、描述、偏好"),
                LandingCardSpec("Myri 气泡", "记录视觉偏好"),
                LandingCardSpec("桌面便签", "小蛇书桌本地便签"),
                LandingCardSpec("本轮桌面", "这一轮递出的纸条与动用家具"),
                LandingCardSpec("海岸词典", "按关键词翻开的专有名词"),
                LandingCardSpec("工作台记录", "海岸家具的成功、失败与脱敏摘要")
            ),
            onPlaceholder = onPlaceholder
        )
    }
}

@Composable
internal fun CoastLandingCards(
    cards: List<LandingCardSpec>,
    onPlaceholder: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        items(cards) { card ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(22.dp)
                    )
                    .clickable {
                        onPlaceholder("${card.title}：Native v1 先保留轻壳，真实数据接线另开后端施工轮。")
                    }
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✦",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        card.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        card.subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
