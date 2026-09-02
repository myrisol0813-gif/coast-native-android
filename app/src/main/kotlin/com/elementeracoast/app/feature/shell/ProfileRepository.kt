package com.elementeracoast.app.feature.shell

import com.elementeracoast.app.core.network.CoastApiClient
import com.elementeracoast.app.core.remote.RemoteCacheStore
import com.elementeracoast.app.core.remote.RemoteDailyProfile
import com.elementeracoast.app.core.remote.RemoteModelCatalogResponse
import com.elementeracoast.app.core.remote.RemoteProfile

interface ProfileRepository {
    fun cachedProfile(): RemoteProfile?
    fun cachedDailyProfile(): RemoteDailyProfile?
    fun cachedModels(): RemoteModelCatalogResponse?
    suspend fun refreshProfile(): RemoteProfile
    suspend fun refreshDailyProfile(): RemoteDailyProfile
    suspend fun refreshModels(force: Boolean = false): RemoteModelCatalogResponse
    suspend fun setCurrentChatModel(modelId: String): RemoteProfile
}

class DefaultProfileRepository(
    private val api: CoastApiClient,
    private val cache: RemoteCacheStore
) : ProfileRepository {
    override fun cachedProfile(): RemoteProfile? = cache.profile()
    override fun cachedDailyProfile(): RemoteDailyProfile? = cache.dailyProfile()
    override fun cachedModels(): RemoteModelCatalogResponse? = cache.modelCatalog()

    override suspend fun refreshProfile(): RemoteProfile = api.getProfile().also(cache::putProfile)
    override suspend fun refreshDailyProfile(): RemoteDailyProfile = api.getDailyProfile().also(cache::putDailyProfile)
    override suspend fun refreshModels(force: Boolean): RemoteModelCatalogResponse = api.listModels(force).also(cache::putModelCatalog)

    override suspend fun setCurrentChatModel(modelId: String): RemoteProfile {
        val current = refreshProfile()
        val updated = api.putProfile(current.copy(currentChatModel = modelId))
        cache.putProfile(updated)
        return updated
    }
}
