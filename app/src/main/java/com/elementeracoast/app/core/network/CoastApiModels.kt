package com.elementeracoast.app.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SessionEnvelope(
    val ok: Boolean = false,
    val authenticated: Boolean = false,
    @SerialName("expires_at") val expiresAt: Long? = null,
)

@Serializable
data class ApiModel(
    val id: String = "",
    val name: String = "",
    @SerialName("is_free") val isFree: Boolean = false,
    val available: Boolean = true,
)

@Serializable
data class ModelGroups(
    @SerialName("openai_chat") val openAiChat: List<ApiModel> = emptyList(),
    @SerialName("free_test") val freeTest: List<ApiModel> = emptyList(),
)

@Serializable
data class ModelDefaults(val chat: String = "")

@Serializable
data class ModelCatalogEnvelope(
    val ok: Boolean = false,
    val groups: ModelGroups = ModelGroups(),
    val defaults: ModelDefaults = ModelDefaults(),
)

@Serializable
data class ChatProfile(
    @SerialName("assistant_avatar_dataurl") val assistantAvatarDataUrl: String = "",
    @SerialName("current_chat_model") val currentChatModel: String = "",
    @SerialName("current_image_model") val currentImageModel: String = "",
    @SerialName("model_box") val modelBox: ModelBox = ModelBox(),
)

@Serializable
data class ModelBox(
    val chat: List<String> = emptyList(),
    val free: List<String> = emptyList(),
    val image: List<String> = emptyList(),
)

@Serializable
data class ProfileEnvelope(val ok: Boolean = false, val profile: ChatProfile = ChatProfile())

@Serializable
data class ConversationDto(
    val id: String = "",
    val title: String = "新聊天",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class ConversationsEnvelope(val ok: Boolean = false, val conversations: List<ConversationDto> = emptyList())

@Serializable
data class ConversationEnvelope(val ok: Boolean = false, val conversation: ConversationDto = ConversationDto())

@Serializable
data class ChatVariantDto(
    val id: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("model_id") val modelId: String? = null,
    val usage: UsageDto? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
    @SerialName("generation_source") val generationSource: String? = null,
)

@Serializable
data class UserBranchDto(
    val active: Int = 0,
    val variants: List<ChatVariantDto> = emptyList(),
)

@Serializable
data class AssistantBranchDto(
    @SerialName("activeByUserVariant") val activeByUserVariant: Map<String, Int> = emptyMap(),
    @SerialName("variantsByUserVariant") val variantsByUserVariant: Map<String, List<ChatVariantDto>> = emptyMap(),
)

@Serializable
data class ChatTurnDto(
    val id: String = "",
    val user: UserBranchDto = UserBranchDto(),
    val assistant: AssistantBranchDto = AssistantBranchDto(),
)

@Serializable
data class ChatHistoryDto(
    val version: Int = 4,
    @SerialName("updated_at") val updatedAt: String = "",
    val turns: List<ChatTurnDto> = emptyList(),
)

@Serializable
data class HistoryEnvelope(val ok: Boolean = false, val history: ChatHistoryDto = ChatHistoryDto())

@Serializable
data class UsageDto(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

@Serializable
data class SimpleChatMessageDto(val role: String, val content: String, @SerialName("turn_id") val turnId: String? = null)

@Serializable
data class ChatRequestDto(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("source_turn_id") val sourceTurnId: String,
    @SerialName("local_date") val localDate: String,
    @SerialName("local_datetime") val localDateTime: String,
    val model: String,
    val messages: List<SimpleChatMessageDto>,
    val settings: JsonElement,
    val stream: Boolean = true,
)

sealed interface ChatStreamEvent {
    data class Meta(val model: String) : ChatStreamEvent
    data class Delta(val content: String) : ChatStreamEvent
    data class Usage(val usage: UsageDto) : ChatStreamEvent
    data class Done(val finishReason: String) : ChatStreamEvent
    data class Error(val type: String, val message: String) : ChatStreamEvent
    data object Tool : ChatStreamEvent
    data object DeskSlip : ChatStreamEvent
}
