package com.elementeracoast.app.feature.daily

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun MomentCard(
    moment: LocalMoment,
    avatarUri: String,
    avatarFallback: String,
    authorLabel: String,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onDelete: () -> Unit,
    onMyriComment: () -> Unit
) {
    DailySurfaceCard {
        Row {
            DailyAvatar(avatarUri, avatarFallback)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(authorLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    moment.text,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal)
                )

                if (moment.comments.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    moment.comments.takeLast(5).forEachIndexed { index, comment ->
                        MomentComment(comment)
                        if (index != moment.comments.takeLast(5).lastIndex) Spacer(Modifier.height(5.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f))
                Spacer(Modifier.height(4.dp))
                MomentActionRows(
                    moment = moment,
                    footer = momentFooter(moment),
                    onLike = onLike,
                    onComment = onComment,
                    onDelete = onDelete,
                    onMyriComment = onMyriComment
                )
            }
        }
    }
}

@Composable
private fun MomentComment(comment: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("小寒") }
            append("：")
            withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append(comment) }
        },
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal
        )
    )
}

private fun momentFooter(moment: LocalMoment): String {
    val clock = runCatching {
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(moment.createdAt))
    }.getOrDefault("")
    val date = runCatching { LocalDate.parse(moment.date).format(DateTimeFormatter.ofPattern("MM月dd日")) }.getOrDefault(moment.date)
    return if (clock.isBlank()) date else "$date · $clock"
}
