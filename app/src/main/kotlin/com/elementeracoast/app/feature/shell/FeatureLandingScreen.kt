package com.elementeracoast.app.feature.shell

import androidx.compose.runtime.Composable
import com.elementeracoast.app.core.local.LocalActionLogStore
import com.elementeracoast.app.core.local.LocalChatStore
import com.elementeracoast.app.core.local.LocalDailyStore
import com.elementeracoast.app.core.local.LocalMemoryStore
import com.elementeracoast.app.core.local.LocalPreferencesStore
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.feature.actionlog.ActionLogScreen
import com.elementeracoast.app.feature.daily.DailyScreen
import com.elementeracoast.app.feature.memory.MemoryScreen
import com.elementeracoast.app.feature.wolf.WolfPage
import com.elementeracoast.app.feature.wolf.WolfScreen

@Composable
internal fun FeatureLandingScreen(
    feature: FeatureDestination,
    preferencesStore: LocalPreferencesStore,
    chatStore: LocalChatStore,
    dailyStore: LocalDailyStore,
    memoryStore: LocalMemoryStore,
    actionLogStore: LocalActionLogStore,
    roomType: RoomType,
    conversationId: String,
    onSnackbar: (String) -> Unit
) {
    when (feature) {
        FeatureDestination.Daily -> DailyScreen(
            store = dailyStore,
            actionLogStore = actionLogStore,
            roomType = roomType,
            conversationId = conversationId,
            onSnackbar = onSnackbar
        )
        FeatureDestination.Memory -> MemoryScreen(
            store = memoryStore,
            actionLogStore = actionLogStore,
            roomType = roomType,
            conversationId = conversationId,
            onSnackbar = onSnackbar
        )
        FeatureDestination.Wolf -> WolfScreen(
            preferencesStore = preferencesStore,
            chatStore = chatStore,
            actionLogStore = actionLogStore,
            currentRoomType = roomType,
            conversationId = conversationId,
            onSnackbar = onSnackbar
        )
        FeatureDestination.Appearance -> WolfScreen(
            preferencesStore = preferencesStore,
            chatStore = chatStore,
            actionLogStore = actionLogStore,
            initialPage = WolfPage.Appearance,
            currentRoomType = roomType,
            conversationId = conversationId,
            onSnackbar = onSnackbar
        )
        FeatureDestination.ActionLog -> ActionLogScreen(actionLogStore)
    }
}
