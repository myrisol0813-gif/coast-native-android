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
import com.elementeracoast.app.feature.daily.DailyRepository
import com.elementeracoast.app.feature.daily.DefaultDailyRepository
import com.elementeracoast.app.feature.memory.DefaultThoughtSoilRepository
import com.elementeracoast.app.feature.memory.ThoughtSoilRepository

data class CoastBackendGraph(
    val auth: AuthRepository,
    val conversations: ConversationRepository,
    val profile: ProfileRepository,
    val chat: ChatRepository,
    val thoughtSoil: ThoughtSoilRepository,
    val daily: DailyRepository
) {
    companion object {
        fun production(context: Context, persistence: LocalPersistence): CoastBackendGraph {
            val authStore = AndroidKeystoreAuthStore(context.applicationContext)
            val config = CoastApiConfig.production()
            val http = CoastHttpClient(config, authStore).client
            val api = CoastApiClient(config, http)
            val cache = RemoteCacheStore(persistence)
            return CoastBackendGraph(
                auth = DefaultAuthRepository(authStore, api),
                conversations = DefaultConversationRepository(api, cache),
                profile = DefaultProfileRepository(api, cache),
                chat = DefaultChatRepository(api, cache),
                thoughtSoil = DefaultThoughtSoilRepository(api),
                daily = DefaultDailyRepository(api, cache)
            )
        }
    }
}
