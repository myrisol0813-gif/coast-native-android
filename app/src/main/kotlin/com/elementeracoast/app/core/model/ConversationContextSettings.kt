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
    @SerialName("workbench_prompt") val workbenchPrompt: String? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("segmented_bubbles") val segmentedBubbles: Boolean = false,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("sticker_display_size") val stickerDisplaySize: Int = 144
) {
    fun normalized(): ConversationContextSettings = copy(
        realityFields = realityFields.filter(RealityContextFields.all::contains).distinct(),
        workbenchPrompt = workbenchPrompt?.take(4000),
        stickerDisplaySize = stickerDisplaySize.takeIf { it in 64..256 } ?: 144,
        weatherCity = weatherCity.replace(Regex("[\\r\\n\\t]+"), " ").trim().take(80)
    )

    fun has(field: String): Boolean = realityEnabled && field in realityFields
}
