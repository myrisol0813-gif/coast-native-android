package com.elementeracoast.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CoastChatTokens {
    val TopBarHeight = 56.dp
    val TopBarHorizontalPadding = 10.dp
    val TopBarTitleSize = 20.sp
    val TopBarModelSize = 14.sp
    val TopBarMenuGlyph = 24.dp
    val TopBarActionGlyph = 21.dp
    val TopBarChevronGlyph = 15.dp
    val TopBarModelGap = 6.dp
    val TopBarModelHorizontalPadding = 4.dp
    val TopBarModelVerticalPadding = 6.dp

    val TimelineHorizontalPadding = 32.dp
    val TimelineTopPadding = 25.dp
    val TimelineBottomPadding = 14.dp
    val MessageGap = 20.dp
    val ChatBodySize = 15.sp
    val ChatBodyLineHeight = 22.sp
    const val UserBubbleWidth = .82f
    val UserBubbleRadius = 18.dp
    val UserBubbleHorizontalPadding = 13.dp
    val UserBubbleVerticalPadding = 9.dp
    val AssistantAvatarSize = 30.dp
    val AssistantAvatarGap = 12.dp
    val AssistantStarSize = 13.sp
    val StreamingGap = 5.dp

    val DogtalkHorizontalPadding = 30.dp
    val DogtalkOuterVerticalPadding = 3.dp
    val DogtalkRadius = 15.dp
    val DogtalkCollapsedHorizontalPadding = 12.dp
    val DogtalkCollapsedVerticalPadding = 7.dp
    val DogtalkExpandedHorizontalPadding = 12.dp
    val DogtalkExpandedBottomPadding = 8.dp
    val DogtalkExpandedMaxHeight = 292.dp
    val DogtalkFieldRadius = 11.dp
    val DogtalkSingleLineHeight = 36.dp
    val DogtalkTextMinHeight = 48.dp
    val DogtalkTextMaxHeight = 68.dp
    val DogtalkFieldHorizontalPadding = 10.dp
    val DogtalkFieldVerticalPadding = 7.dp
    val DogtalkTitleSize = 12.sp
    val DogtalkMetaSize = 11.sp
    val DogtalkBodySize = 13.sp
    val DogtalkMenuWidthMin = 236.dp
    val DogtalkMenuWidthMax = 318.dp
    val DogtalkSaveHeight = 36.dp

    val ComposerHorizontalPadding = 12.dp
    val ComposerVerticalPadding = 7.dp
    val ComposerTouchTarget = 48.dp
    val ComposerVisualButton = 41.dp
    val ComposerActionGlyph = 19.dp
    val ComposerPlusGlyph = 21.dp
    val ComposerPillMinHeight = 48.dp
    val ComposerPillMaxHeight = 132.dp
    val ComposerPillRadius = 25.dp
    val ComposerPillStartPadding = 16.dp
    val ComposerPillEndPadding = 6.dp
    val ComposerPillVerticalPadding = 7.dp
    val ComposerTextSize = 15.sp
    val ComposerTextLineHeight = 21.sp
    val ComposerMicTouch = 32.dp
    val ComposerMicGlyph = 19.dp
    val ComposerGap = 7.dp

    val DrawerWidth = 326.dp
    val DrawerOuterHorizontalPadding = 14.dp
    val DrawerContentVerticalPadding = 12.dp
    val DrawerHeaderTopPadding = 10.dp
    val DrawerHeaderBottomPadding = 9.dp
    val DrawerCloseGlyph = 23.dp
    val DrawerSearchRadius = 15.dp
    val DrawerSearchHorizontalPadding = 12.dp
    val DrawerSearchVerticalPadding = 9.dp
    val DrawerSearchGlyph = 18.dp
    val DrawerSearchGap = 8.dp
    val DrawerTextSize = 15.sp
    val DrawerSecondaryTextSize = 13.sp
    val DrawerItemGap = 2.dp
    val DrawerEntryRadius = 12.dp
    val DrawerEntryHorizontalPadding = 11.dp
    val DrawerEntryVerticalPadding = 9.dp
    val DrawerEntrySubtitleVerticalPadding = 7.dp
    val DrawerEntryGlyph = 19.dp
    val DrawerEntryGap = 10.dp
    val DrawerStatusHeight = 62.dp
    val DrawerStatusGap = 7.dp
    val DrawerStatusBottomPadding = 13.dp
    val DrawerStatusRadius = 13.dp
    val DrawerStatusVerticalPadding = 7.dp
    val DrawerSectionTopPadding = 10.dp
    val DrawerSectionBottomPadding = 5.dp
    val DrawerBottomVerticalPadding = 7.dp
    val ConversationVerticalPadding = 9.dp
    val ConversationMoreGlyph = 19.dp
}

val DogtalkCardLight = Color(0xFFF8F7F4)
val DogtalkFieldLight = Color.White

@Composable
fun coastDogtalkCardColor(): Color = if (MaterialTheme.colorScheme.background.luminance() > .55f) {
    DogtalkCardLight
} else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)
}

@Composable
fun coastDogtalkFieldColor(): Color = if (MaterialTheme.colorScheme.background.luminance() > .55f) {
    DogtalkFieldLight
} else {
    MaterialTheme.colorScheme.surface.copy(alpha = .96f)
}
