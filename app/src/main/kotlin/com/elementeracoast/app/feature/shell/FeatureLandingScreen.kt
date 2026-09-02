package com.elementeracoast.app.feature.shell

import androidx.compose.runtime.Composable
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.feature.daily.DailyLanding
import com.elementeracoast.app.feature.letters.IslandLetterScreen
import com.elementeracoast.app.feature.memory.MemoryLanding
import com.elementeracoast.app.feature.serpentdesk.SerpentDeskScreen
import com.elementeracoast.app.feature.wolf.WolfScreen

@Composable
internal fun FeatureLandingScreen(
    feature: FeatureDestination,
    shellState: CoastShellState,
    services: LocalFeatureServices,
    messages: List<ChatMessage>,
    onBackToChat: () -> Unit,
    onSelectModel: (String) -> Unit,
    onRefreshModels: () -> Unit,
    onImportMessages: (List<ChatMessage>) -> Unit,
    onLocalActionLogged: (String, String, String) -> Unit,
    onPlaceholder: (String) -> Unit
) {
    when (feature) {
        FeatureDestination.Daily -> DailyLanding(
            store = services.daily,
            onBackToChat = onBackToChat,
            onActionLogged = onLocalActionLogged,
            onSnackbar = onPlaceholder
        )
        FeatureDestination.Memory -> MemoryLanding(
            store = services.memory,
            onBackToChat = onBackToChat,
            onActionLogged = onLocalActionLogged,
            onSnackbar = onPlaceholder
        )
        FeatureDestination.Wolf -> WolfScreen(
            store = services.wolf,
            shellState = shellState,
            messages = messages,
            onSelectModel = onSelectModel,
            onRefreshModels = onRefreshModels,
            onImportMessages = onImportMessages,
            onActionLogged = onLocalActionLogged,
            onSnackbar = onPlaceholder
        )
        FeatureDestination.ActionLog -> SerpentDeskScreen(
            actionLogStore = services.actionLog,
            conversationId = shellState.activeConversationId,
            focusIds = shellState.actionLogFocusIds
        )
        FeatureDestination.IslandLetter -> IslandLetterScreen(
            store = services.islandLetter,
            conversationId = shellState.activeConversationId,
            modelName = shellState.currentModel,
            onBack = onBackToChat,
            onSnackbar = onPlaceholder
        )
    }
}
