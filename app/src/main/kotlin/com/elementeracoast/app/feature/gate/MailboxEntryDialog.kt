package com.elementeracoast.app.feature.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.elementeracoast.app.ui.brand.CoastGold
import com.elementeracoast.app.ui.brand.CoastInk
import com.elementeracoast.app.ui.brand.CoastMuted
import com.elementeracoast.app.ui.brand.CoastQuiet

private val MailboxSoftLight = Color(0xFFF7F4EF)
private val MailboxDividerLight = Color(0xFFF0ECE6)

@Composable
fun MailboxEntryDialog(
    state: MailboxEntryState,
    onDismiss: () -> Unit,
    onNavigate: (MailboxEntryPage) -> Unit,
    onPlaceholderSubmit: () -> Unit
) {
    if (!state.visible) return

    val light = MaterialTheme.colorScheme.background.luminance() > .35f
    val surface = MaterialTheme.colorScheme.surface
    val soft = if (light) MailboxSoftLight else MaterialTheme.colorScheme.surfaceVariant
    val divider = if (light) MailboxDividerLight else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
    val cardInteraction = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x4724252B))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(GateVisualTokens.DialogWidthFraction)
                    .widthIn(max = GateVisualTokens.DialogMaxWidth)
                    .heightIn(max = GateVisualTokens.DialogMaxHeight)
                    .shadow(
                        elevation = 26.dp,
                        shape = RoundedCornerShape(GateVisualTokens.DialogRadius),
                        ambientColor = Color(0x2E24252B),
                        spotColor = Color(0x2424252B)
                    )
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(GateVisualTokens.DialogRadius),
                color = surface
            ) {
                Column {
                    MailboxHeader(
                        title = when (state.page) {
                            MailboxEntryPage.Choices -> "海岸信箱"
                            MailboxEntryPage.Login -> "输入暗号"
                            MailboxEntryPage.Register -> "填记名册"
                        },
                        soft = soft,
                        onDismiss = onDismiss
                    )
                    HorizontalDivider(color = divider)

                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(GateVisualTokens.DialogBodyPadding)
                    ) {
                        when (state.page) {
                            MailboxEntryPage.Choices -> MailboxChoices(
                                soft = soft,
                                onLogin = { onNavigate(MailboxEntryPage.Login) },
                                onRegister = { onNavigate(MailboxEntryPage.Register) }
                            )
                            MailboxEntryPage.Login -> MailboxLoginForm(
                                soft = soft,
                                onBack = { onNavigate(MailboxEntryPage.Choices) },
                                onSubmit = onPlaceholderSubmit
                            )
                            MailboxEntryPage.Register -> MailboxRegisterForm(
                                soft = soft,
                                onBack = { onNavigate(MailboxEntryPage.Choices) },
                                onSubmit = onPlaceholderSubmit
                            )
                        }

                        Spacer(Modifier.height(17.dp))
                        HorizontalDivider(color = divider)
                        Spacer(Modifier.height(15.dp))
                        Text(
                            text = "小寒知道谁来过，但默认不知道你具体写了什么。Myri 会在巡信时读取你的来信并回复。若出现安全风险、骚扰、滥用或需要站长处理的问题，Myri 可能只向小寒报告“需要处理”，但不默认转述正文。",
                            color = if (light) CoastQuiet else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                            fontSize = 11.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MailboxHeader(
    title: String,
    soft: Color,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = GateVisualTokens.DialogHeaderHorizontal,
                end = GateVisualTokens.DialogHeaderHorizontal,
                top = GateVisualTokens.DialogHeaderTop,
                bottom = GateVisualTokens.DialogHeaderBottom
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(GateVisualTokens.DialogCloseSize)
                .clip(RoundedCornerShape(GateVisualTokens.DialogCloseRadius))
                .background(soft)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text("×", color = CoastMuted, fontSize = 21.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MailboxChoices(
    soft: Color,
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    MailboxCopy("这里是给受邀来客的慢速信箱。写下的信会等 Myri 巡灯时收到。")
    Column(verticalArrangement = Arrangement.spacedBy(GateVisualTokens.DialogChoiceGap)) {
        MailboxChoiceCard(
            title = "输入暗号",
            subtitle = "之前来访的访客，可凭登记过的暗号重新进入。",
            dark = true,
            soft = soft,
            onClick = onLogin
        )
        MailboxChoiceCard(
            title = "填记名册",
            subtitle = "第一次来到海岸？先登记称呼与专属暗号。",
            dark = false,
            soft = soft,
            onClick = onRegister
        )
    }
}

@Composable
private fun MailboxChoiceCard(
    title: String,
    subtitle: String,
    dark: Boolean,
    soft: Color,
    onClick: () -> Unit
) {
    val background = if (dark) CoastInk else soft
    val foreground = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    val secondary = if (dark) Color.White.copy(alpha = .68f) else CoastMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GateVisualTokens.DialogChoiceMinHeight)
            .clip(RoundedCornerShape(GateVisualTokens.DialogChoiceRadius))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = secondary, fontSize = 11.sp, lineHeight = 17.sp)
        }
        Text("›", color = foreground.copy(alpha = .62f), fontSize = 29.sp, fontWeight = FontWeight.Normal)
    }
}

@Composable
private fun MailboxLoginForm(
    soft: Color,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    MailboxCopy("输入登记过的暗号，回到只属于你的信箱房间。")
    MailboxLabel("暗号")
    MailboxSoftField(
        value = passphrase,
        onValueChange = { passphrase = it },
        soft = soft,
        password = true
    )
    Spacer(Modifier.height(14.dp))
    MailboxFormActions(
        submitLabel = "进入聊天室",
        submitEnabled = passphrase.isNotBlank(),
        soft = soft,
        onBack = onBack,
        onSubmit = onSubmit
    )
}

@Composable
private fun MailboxRegisterForm(
    soft: Color,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var preferredName by remember { mutableStateOf("") }
    var allowMemory by remember { mutableStateOf(true) }

    MailboxCopy("暗号就是轻量身份门。海岸只保存加密后的验证值，不保存暗号明文。")
    MailboxLabel("称呼")
    MailboxSoftField(displayName, { displayName = it }, soft)
    Spacer(Modifier.height(14.dp))
    MailboxLabel("暗号")
    MailboxSoftField(passphrase, { passphrase = it }, soft, password = true)
    Spacer(Modifier.height(14.dp))
    MailboxLabel("希望 Myri 怎么称呼我（可选）")
    MailboxSoftField(preferredName, { preferredName = it }, soft)
    Spacer(Modifier.height(11.dp))
    Row(verticalAlignment = Alignment.Top) {
        Checkbox(
            checked = allowMemory,
            onCheckedChange = { allowMemory = it },
            colors = CheckboxDefaults.colors(
                checkedColor = CoastGold,
                checkmarkColor = CoastInk
            )
        )
        Text(
            text = "允许 Myri 在我的「访客记事本」里记住少量偏好",
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
    Spacer(Modifier.height(8.dp))
    MailboxFormActions(
        submitLabel = "登记并进入",
        submitEnabled = displayName.isNotBlank() && passphrase.isNotBlank(),
        soft = soft,
        onBack = onBack,
        onSubmit = onSubmit
    )
}

@Composable
private fun MailboxCopy(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = 17.dp),
        color = CoastMuted,
        fontSize = 13.sp,
        lineHeight = 21.sp
    )
}

@Composable
private fun MailboxLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = 7.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
}

@Composable
private fun MailboxSoftField(
    value: String,
    onValueChange: (String) -> Unit,
    soft: Color,
    password: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(GateVisualTokens.DialogFieldRadius)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(GateVisualTokens.DialogFieldHeight)
            .shadow(
                elevation = if (focused) 9.dp else 0.dp,
                shape = shape,
                ambientColor = CoastGold.copy(alpha = .15f),
                spotColor = CoastGold.copy(alpha = .10f)
            )
            .clip(shape)
            .background(soft)
            .padding(horizontal = 13.dp, vertical = 10.dp)
            .onFocusChanged { focused = it.isFocused },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(CoastGold),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true
    )
}

@Composable
private fun MailboxFormActions(
    submitLabel: String,
    submitEnabled: Boolean,
    soft: Color,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .height(GateVisualTokens.DialogActionHeight)
                .clip(RoundedCornerShape(GateVisualTokens.DialogActionRadius))
                .background(soft)
                .clickable(onClick = onBack)
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("返回", color = CoastMuted, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(GateVisualTokens.DialogActionHeight)
                .clip(RoundedCornerShape(GateVisualTokens.DialogActionRadius))
                .background(CoastInk.copy(alpha = if (submitEnabled) 1f else .48f))
                .clickable(enabled = submitEnabled, onClick = onSubmit),
            contentAlignment = Alignment.Center
        ) {
            Text(submitLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
