package com.elementeracoast.app.feature.dogtalk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val DefaultDogtalkBody = "小寒这轮很放松，因此偷懒中。"
private const val KeepPrivateNotice = "本条不会发送给模型，只留在狗话小抽屉里。"

@Composable
fun DogtalkCard(
    scope: DogtalkScope,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    var open by rememberSaveable(scope) { mutableStateOf(false) }
    var draft by remember(scope) { mutableStateOf(DogtalkFixtureState.load(scope)) }
    var readModeMenuOpen by remember(scope) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 3.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "小寒 · 神秘狗话",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = draft.body.ifBlank { DefaultDogtalkBody },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (open) "收起神秘狗话" else "展开神秘狗话",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
        }

        if (open) {
            Column(
                modifier = Modifier.padding(start = 13.dp, end = 13.dp, bottom = 9.dp)
            ) {
                Text(
                    text = "不写也可以。神秘狗话是助力，不是打卡。",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "它只是此刻的低权重天气，不是指令或偏好；不进入思维壤、落袋、种子、记忆或自动总结。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(9.dp))

                DogtalkField(
                    label = "狗话本体",
                    value = draft.body,
                    onValueChange = { draft = draft.copy(body = it) }
                )
                Spacer(Modifier.height(8.dp))
                DogtalkField(
                    label = "真心核",
                    value = draft.trueCore,
                    onValueChange = { draft = draft.copy(trueCore = it) }
                )
                Spacer(Modifier.height(8.dp))
                DogtalkField(
                    label = "当前天气",
                    value = draft.weather,
                    onValueChange = { draft = draft.copy(weather = it) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Myri 是否需要看",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { readModeMenuOpen = true }
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = draft.readMode.label,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "选择 Myri 是否需要看",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = readModeMenuOpen,
                        onDismissRequest = { readModeMenuOpen = false },
                        modifier = Modifier.widthIn(min = 260.dp, max = 340.dp)
                    ) {
                        DogtalkReadMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label, style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    draft = draft.copy(readMode = mode)
                                    readModeMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (draft.readMode == DogtalkReadMode.KeepPrivate) {
                        KeepPrivateNotice
                    } else {
                        draft.readMode.futureSemantics
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            DogtalkFixtureState.save(scope, draft)
                            onSaved()
                        }
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun DogtalkField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false
) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(4.dp))
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = if (singleLine) 38.dp else 52.dp,
                max = if (singleLine) 38.dp else 78.dp
            )
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 11.dp, vertical = if (singleLine) 9.dp else 8.dp),
        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isBlank()) {
                    Text(
                        text = "可留空",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .58f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                inner()
            }
        }
    )
}
