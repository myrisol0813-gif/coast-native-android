package com.elementeracoast.app.core.remote

import com.elementeracoast.app.core.local.LocalPersistence
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RemoteCacheStore(
    private val persistence: LocalPersistence,
    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }
) {
    fun conversations(): List<RemoteConversation> = decode(KEY_CONVERSATIONS, ListSerializer(RemoteConversation.serializer())) ?: emptyList()
    fun putConversations(value: List<RemoteConversation>) = put(KEY_CONVERSATIONS, ListSerializer(RemoteConversation.serializer()), value)

    fun history(id: String): RemoteHistory? = decode(historyKey(id), RemoteHistory.serializer())
    fun putHistory(id: String, value: RemoteHistory) = put(historyKey(id), RemoteHistory.serializer(), value)
    fun removeHistory(id: String) = persistence.remove(historyKey(id))

    fun profile(): RemoteProfile? = decode(KEY_PROFILE, RemoteProfile.serializer())
    fun putProfile(value: RemoteProfile) = put(KEY_PROFILE, RemoteProfile.serializer(), value)

    fun dailyProfile(): RemoteDailyProfile? = decode(KEY_DAILY_PROFILE, RemoteDailyProfile.serializer())
    fun putDailyProfile(value: RemoteDailyProfile) = put(KEY_DAILY_PROFILE, RemoteDailyProfile.serializer(), value)

    fun modelCatalog(): RemoteModelCatalogResponse? = decode(KEY_MODELS, RemoteModelCatalogResponse.serializer())
    fun putModelCatalog(value: RemoteModelCatalogResponse) = put(KEY_MODELS, RemoteModelCatalogResponse.serializer(), value)

    private fun historyKey(id: String): String = "remote.history.${id.replace(Regex("[^A-Za-z0-9_.:-]"), "_")}"

    private fun <T> decode(key: String, serializer: kotlinx.serialization.KSerializer<T>): T? {
        val raw = persistence.get(key)
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
    }

    private fun <T> put(key: String, serializer: kotlinx.serialization.KSerializer<T>, value: T) {
        persistence.put(key, json.encodeToString(serializer, value))
    }

    companion object {
        private const val KEY_CONVERSATIONS = "remote.cache.conversations.v1"
        private const val KEY_PROFILE = "remote.cache.profile.v1"
        private const val KEY_DAILY_PROFILE = "remote.cache.daily-profile.v1"
        private const val KEY_MODELS = "remote.cache.models.v1"
    }
}
