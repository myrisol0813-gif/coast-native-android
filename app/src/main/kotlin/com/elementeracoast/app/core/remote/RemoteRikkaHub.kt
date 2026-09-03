package com.elementeracoast.app.core.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteRikkaHubImportSummary(
    @SerialName("conversation_id") val conversationId: String,
    val source: String = "rikkahub",
    @SerialName("source_window_id") val sourceWindowId: String = "",
    @SerialName("message_count") val messageCount: Int = 0,
    @SerialName("attachment_count") val attachmentCount: Int = 0,
    @SerialName("imported_at") val importedAt: Long = 0L
)

@Serializable
data class RemoteRikkaHubImportListResponse(
    val ok: Boolean = false,
    val imports: List<RemoteRikkaHubImportSummary> = emptyList()
)

@Serializable
data class RemoteRikkaHubAttachment(
    val id: String,
    val name: String = "附件",
    @SerialName("mime_type") val mimeType: String = "application/octet-stream",
    @SerialName("byte_length") val byteLength: Long = 0L,
    @SerialName("data_url") val dataUrl: String = ""
)

@Serializable
data class RemoteRikkaHubMessage(
    val id: String,
    val role: String,
    val content: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("model_id") val modelId: String = "",
    val attachments: List<RemoteRikkaHubAttachment> = emptyList()
)

@Serializable
data class RemoteRikkaHubHistory(
    val source: String = "rikkahub",
    val messages: List<RemoteRikkaHubMessage> = emptyList()
)

@Serializable
data class RemoteRikkaHubHistoryResponse(
    val ok: Boolean = false,
    val history: RemoteRikkaHubHistory = RemoteRikkaHubHistory()
)
