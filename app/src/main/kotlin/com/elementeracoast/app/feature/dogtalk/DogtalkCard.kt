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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.ui.theme.CoastChatTokens
import com.elementeracoast.app.ui.theme.coastDogtalkCardColor
import com.elementeracoast.app.ui.theme.coastDogtalkFieldColor

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
    val shape = RoundedCornerShape(CoastChatTokens.DogtalkRadius)
    val cardColor = coastDogtalkCardColor()
    val fieldColor = coastDogtalkFieldColor()

    Column(
        modifier = modifier
  .fillMaxWidth()
  .padding(
      horizontal = CoastChatTokens.DogtalkHorizontalPadding,
      vertical = CoastChatTokens.DogtalkOuterVerticalPadding
  )
  .shadow(elevation = 1.dp, shape = shape, clip = false)
  .background(cardColor, shape)
    ) {
        Row(
  modifier = Modifier
      .fillMaxWidth()
      .clickable { open = !open }
      .padding(
          horizontal = CoastChatTokens.DogtalkCollapsedHorizontalPadding,
          vertical = CoastChatTokens.DogtalkCollapsedVerticalPadding
      ),
  verticalAlignment = Alignment.CenterVertically
        ) {
  Column(modifier = Modifier.weight(1f)) {
      Text(
          text = "小寒 · 神秘狗话",
          style = MaterialTheme.typography.labelMedium.copy(fontSize = CoastChatTokens.DogtalkTitleSize),
          fontWeight = FontWeight.SemiBold
      )
      Text(
          text = draft.body.ifBlank { DefaultDogtalkBody },
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .82f),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
      )
  }
  Spacer(Modifier.width(7.dp))
  Icon(
      imageVector = Icons.Default.ExpandMore,
      contentDescription = if (open) "收起神秘狗话" else "展开神秘狗话",
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
      modifier = Modifier.size(16.dp)
  )
        }

        if (open) {
  Column(
      modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = CoastChatTokens.DogtalkExpandedMaxHeight)
          .verticalScroll(rememberScrollState())
          .padding(
              start = CoastChatTokens.DogtalkExpandedHorizontalPadding,
              end = CoastChatTokens.DogtalkExpandedHorizontalPadding,
              bottom = CoastChatTokens.DogtalkExpandedBottomPadding
          )
  ) {
      Text(
          text = "不写也可以。神秘狗话是助力，不是打卡。",
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = .88f),
          style = MaterialTheme.typography.bodySmall.copy(fontSize = CoastChatTokens.DogtalkBodySize)
      )
      Spacer(Modifier.height(3.dp))
      Text(
          text = "它只是此刻的低权重天气，不是指令或偏好；不进入思维壤、落袋、种子、记忆或自动总结。",
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize)
      )
      Spacer(Modifier.height(8.dp))

      DogtalkField("狗话本体", draft.body, { draft = draft.copy(body = it) }, fieldColor = fieldColor)
      Spacer(Modifier.height(7.dp))
      DogtalkField("真心核", draft.trueCore, { draft = draft.copy(trueCore = it) }, fieldColor = fieldColor)
      Spacer(Modifier.height(7.dp))
      DogtalkField(
          label = "当前天气",
          value = draft.weather,
          onValueChange = { draft = draft.copy(weather = it) },
          singleLine = true,
          fieldColor = fieldColor
      )
      Spacer(Modifier.height(7.dp))

      Text(
          text = "Myri 是否需要看",
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .78f),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize),
          fontWeight = FontWeight.Medium
      )
      Spacer(Modifier.height(4.dp))
      Box {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .background(fieldColor, RoundedCornerShape(CoastChatTokens.DogtalkFieldRadius))
                  .clickable { readModeMenuOpen = true }
                  .padding(
                      horizontal = CoastChatTokens.DogtalkFieldHorizontalPadding,
                      vertical = CoastChatTokens.DogtalkFieldVerticalPadding
                  ),
              verticalAlignment = Alignment.CenterVertically
          ) {
              Text(
                  text = draft.readMode.label,
                  modifier = Modifier.weight(1f),
                  color = MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = CoastChatTokens.DogtalkBodySize)
              )
              Icon(
                  imageVector = Icons.Default.ExpandMore,
                  contentDescription = "选择 Myri 是否需要看",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
                  modifier = Modifier.size(15.dp)
              )
          }
          DropdownMenu(
              expanded = readModeMenuOpen,
              onDismissRequest = { readModeMenuOpen = false },
              modifier = Modifier
                  .widthIn(
                      min = CoastChatTokens.DogtalkMenuWidthMin,
                      max = CoastChatTokens.DogtalkMenuWidthMax
                  )
                  .background(fieldColor, RoundedCornerShape(CoastChatTokens.DogtalkFieldRadius))
          ) {
              DogtalkReadMode.entries.forEach { mode ->
                  DropdownMenuItem(
                      text = {
                          Text(
                              mode.label,
                              style = MaterialTheme.typography.bodySmall.copy(
                                  fontSize = CoastChatTokens.DogtalkBodySize
                              )
                          )
                      },
                      onClick = {
                          draft = draft.copy(readMode = mode)
                          readModeMenuOpen = false
                      }
                  )
              }
          }
      }

      Spacer(Modifier.height(4.dp))
      Text(
          text = if (draft.readMode == DogtalkReadMode.KeepPrivate) KeepPrivateNotice else draft.readMode.futureSemantics,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize)
      )

      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Spacer(Modifier.weight(1f))
          Button(
              onClick = {
                  DogtalkFixtureState.save(scope, draft)
                  onSaved()
              },
              modifier = Modifier.height(CoastChatTokens.DogtalkSaveHeight)
          ) {
              Text("保存", fontSize = CoastChatTokens.DogtalkBodySize)
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
    singleLine: Boolean = false,
    fieldColor: androidx.compose.ui.graphics.Color
) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .78f),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize),
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(4.dp))
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
  .fillMaxWidth()
  .heightIn(
      min = if (singleLine) CoastChatTokens.DogtalkSingleLineHeight else CoastChatTokens.DogtalkTextMinHeight,
      max = if (singleLine) CoastChatTokens.DogtalkSingleLineHeight else CoastChatTokens.DogtalkTextMaxHeight
  )
  .background(fieldColor, RoundedCornerShape(CoastChatTokens.DogtalkFieldRadius))
  .padding(
      horizontal = CoastChatTokens.DogtalkFieldHorizontalPadding,
      vertical = CoastChatTokens.DogtalkFieldVerticalPadding
  ),
        textStyle = MaterialTheme.typography.bodySmall.copy(
  color = MaterialTheme.colorScheme.onSurface,
  fontSize = CoastChatTokens.DogtalkBodySize
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        decorationBox = { inner ->
  Box(modifier = Modifier.fillMaxWidth()) {
      if (value.isBlank()) {
          Text(
              text = "可留空",
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f),
              style = MaterialTheme.typography.bodySmall.copy(fontSize = CoastChatTokens.DogtalkBodySize)
          )
      }
      inner()
  }
        }
    )
}
