package com.elementeracoast.app.feature.shell

import android.content.Context
import com.elementeracoast.app.core.auth.AndroidKeystoreAuthStore
import com.elementeracoast.app.core.auth.AuthRepository
import com.elementeracoast.app.core.auth.DefaultAuthRepository
import com.elementeracoast.app.core.local.LocalPersistence
import com.elementeracoast.app.core.network.CoastApiClient
import com.elementeracoast.app.core.network.CoastApiConfig
import com.elementeracoast.app.core.network.CoastHttpClient
import com.elementeracoast.app.core.remote.RemoteCacheStore
import com.elementeracoast.app.feature.chat.ChatRepository
import com.elementeracoast.app.feature.chat.DefaultChatRepository
import com.elementeracoast.app.feature.chat.ModelMetadataRemoteDataSource
import com.elementeracoast.app.feature.daily.DailyRepository
import com.elementeracoast.app.feature.daily.DefaultDailyRepository
import com.elementeracoast.app.feature.dogtalk.DefaultDogtalkRepository
import com.elementeracoast.app.feature.dogtalk.DogtalkRepository
import com.elementeracoast.app.feature.memory.DefaultMemoryRepository
import com.elementeracoast.app.feature.memory.DefaultThoughtSoilRepository
import com.elementeracoast.app.feature.memory.MemoryRemoteDataSource
import com.elementeracoast.app.feature.memory.MemoryRepository
import com.elementeracoast.app.feature.memory.ThoughtSoilRepository
import com.elementeracoast.app.feature.wolf.WolfStore

data class CoastBackendGraph(
    val auth: AuthRepository,
    val conversations: ConversationRepository,
    val profile: ProfileRepository,
    val chat: ChatRepository,
    val thoughtSoil: ThoughtSoilRepository,
    val daily: DailyRepository,
    val memory: MemoryRepository,
    val dogtalk: DogtalkRepository
) {
    companion object {
        fun production(context: Context, persistence: LocalPersistence): CoastBackendGraph {
            val authStore = AndroidKeystoreAuthStore(context.applicationContext)
            val config = CoastApiConfig.production()
            val http = CoastHttpClient(config, authStore).client
            val api = CoastApiClient(config, http)
            val cache = RemoteCacheStore(persistence)
            val memoryRemote = MemoryRemoteDataSource(config, http)
            val metadataRemote = ModelMetadataRemoteDataSource(config, http)
            val titleRemote = ConversationTitleRemoteDataSource(config, http)
            val chatSettingsProvider = {
                mapOf("recentTurns" to WolfStore.readRecentTurns(persistence).toString())
            }
            return CoastBackendGraph(
                auth = DefaultAuthRepository(authStore, api),
                conversations = DefaultConversationRepository(api, cache, titleRemote),
                profile = DefaultProfileRepository(api, cache),
                chat = DefaultChatRepository(api, cache, metadataRemote, settingsProvider = chatSettingsProvider),
                thoughtSoil = DefaultThoughtSoilRepository(api),
                daily = DefaultDailyRepository(api, cache),
                memory = DefaultMemoryRepository(memoryRemote, cache),
                dogtalk = DefaultDogtalkRepository(config, http)
            )
        }
    }
}