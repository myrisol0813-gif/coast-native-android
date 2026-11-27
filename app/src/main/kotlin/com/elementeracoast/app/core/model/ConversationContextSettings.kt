package com.elementeracoast.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object RealityContextFields {
    const val DATE = "date"
    const val TIME = "time"
    const val BATTERY = "battery"
    const val WEATHER = "weather"
    const val LOCATION = "location"
    const val RECENT_APP = "recent_app"
    const val APP_USAGE = "app_usage"
    const val CALENDAR = "calendar"
    const val STEPS = "steps"

    val all = listOf(DATE, TIME, BATTERY, WEATHER, LOCATION, RECENT_APP, APP_USAGE, CALENDAR, STEPS)
}

const val DEFAULT_WORKBENCH_PROMPT = "这里有一些你可以使用的工具。你可以依照自己的判断，让它们自然地加入对话、阅读、创作与探索，不必等我逐项提出要求，也不必为了使用而使用。希望你用得顺手，也用得开心。"

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
data class ConversationContextSettings(
    @SerialName("reality_enabled") val realityEnabled: Boolean = false,
    @SerialName("reality_fields") val realityFields: List<String> = emptyList(),
    @SerialName("short_reply_mode") val shortReplyMode: Boolean = false,
    @SerialName("weather_city") val weatherCity: String = "",
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("cross_window_auto") val crossWindowAuto: Boolean = false,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("local_model_tools_enabled") val localModelToolsEnabled: Boolean = false,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("local_model_write_enabled") val localModelWriteEnabled: Boolean = false,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("sticker_send_enabled") val stickerSendEnabled: Boolean = false,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("sticker_add_enabled") val stickerAddEnabled: Boolean = false,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("reply_sequence_enabled") val replySequenceEnabled: Boolean = false,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("reply_sequence_max_messages") val replySequenceMaxMessages: Int? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("reply_sequence_tool_budget") val replySequenceToolBudget: Int? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("reply_sequence_token_budget") val replySequenceTokenBudget: Int? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("workbench_prompt") val workbenchPrompt: String? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("segmented_bubbles") val segmentedBubbles: Boolean = false,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("sticker_display_size") val stickerDisplaySize: Int = 144,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("dream_readback_enabled") val dreamReadbackEnabled: Boolean = false
) {
    fun normalized(): ConversationContextSettings = copy(
        realityFields = realityFields.filter(RealityContextFields.all::contains).distinct(),
        workbenchPrompt = workbenchPrompt?.take(4000),
        stickerDisplaySize = stickerDisplaySize.takeIf { it in 64..256 } ?: 144,
        replySequenceMaxMessages = replySequenceMaxMessages?.takeIf { it in 1..20 },
        replySequenceToolBudget = replySequenceToolBudget?.takeIf { it in 0..128 },
        replySequenceTokenBudget = replySequenceTokenBudget?.takeIf { it in 1..1000000 },
        weatherCity = weatherCity.replace(Regex("[\\r\\n\\t]+"), " ").trim().take(80)
    )

    fun has(field: String): Boolean = realityEnabled && field in realityFields
}
