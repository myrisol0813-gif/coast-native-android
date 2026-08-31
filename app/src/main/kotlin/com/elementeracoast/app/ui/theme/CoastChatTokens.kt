package com.elementeracoast.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CoastChatTokens {
    val TopBarHeight = 50.dp
    val TopBarHorizontalPadding = 4.dp
    val TopBarTitleSize = 18.sp
    val TopBarModelSize = 13.sp
    val TopBarMenuGlyph = 21.dp
    val TopBarActionGlyph = 19.dp
    val TopBarChevronGlyph = 12.dp
    val TopBarModelGap = 4.dp
    val TopBarModelHorizontalPadding = 0.dp
    val TopBarModelVerticalPadding = 2.dp
    val TopBarDividerThickness = 0.5.dp
    const val TopBarDividerAlpha = 0.62f
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
    val DogtalkSaveTouchWidth = 88.dp
    val DogtalkSaveTouchHeight = 44.dp
    val DogtalkSaveVisualMinWidth = 68.dp
    val DogtalkSaveVisualHeight = 30.dp
    val DogtalkSaveRadius = 15.dp
    val DogtalkSaveHorizontalPadding = 14.dp
    val DogtalkSaveTextSize = 12.sp
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
    val DrawerContentVerticalPadding = 10.dp
    val DrawerHeaderTopPadding = 6.dp
    val DrawerHeaderBottomPadding = 6.dp
    val DrawerCloseGlyph = 22.dp
    val DrawerSearchRadius = 13.dp
    val DrawerSearchHorizontalPadding = 10.dp
    val DrawerSearchVerticalPadding = 6.dp
    val DrawerSearchGlyph = 17.dp
    val DrawerSearchGap = 7.dp
    val DrawerTextSize = 15.sp
    val DrawerSecondaryTextSize = 12.sp
    val DrawerItemGap = 2.dp
    val DrawerEntryRadius = 12.dp
    val DrawerEntryHorizontalPadding = 11.dp
    val DrawerEntryVerticalPadding = 8.dp
    val DrawerEntrySubtitleVerticalPadding = 7.dp
    val DrawerEntryGlyph = 19.dp
    val DrawerEntryGap = 10.dp
    val DrawerStatusHeight = 62.dp
    val DrawerStatusGap = 7.dp
    val DrawerStatusBottomPadding = 11.dp
    val DrawerStatusRadius = 13.dp
    val DrawerStatusVerticalPadding = 7.dp
    val DrawerBottomVerticalPadding = 7.dp
    val DrawerUtilityIconBox = 38.dp
    val DrawerUtilityIcon = 25.dp
    val DrawerUtilityTitleSize = 16.sp
    val DrawerUtilitySubtitleSize = 12.sp
    val DrawerUtilityGap = 11.dp
    val ConversationVerticalPadding = 9.dp
    val ConversationMoreGlyph = 19.dp
}

val DogtalkCardLight = Color(0xFFF8F7F4)
val DogtalkFieldLight = Color.White

@Composable
fun coastDogtalkCardColor(): Color = if (MaterialTheme.colorScheme.background.luminance() > .55f) DogtalkCardLight
else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)

@Composable
fun coastDogtalkFieldColor(): Color = if (MaterialTheme.colorScheme.background.luminance() > .55f) DogtalkFieldLight
else MaterialTheme.colorScheme.surface.copy(alpha = .96f)
