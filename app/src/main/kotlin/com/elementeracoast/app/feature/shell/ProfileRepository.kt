package com.elementeracoast.app.feature.shell

import com.elementeracoast.app.core.network.CoastApiClient
import com.elementeracoast.app.core.remote.RemoteCacheStore
import com.elementeracoast.app.core.remote.RemoteDailyProfile
import com.elementeracoast.app.core.remote.RemoteModelCatalogResponse
import com.elementeracoast.app.core.remote.RemoteProfile

class ProfileRepository(
    private val api: CoastApiClient,
    private val cache: RemoteCacheStore
) {
    fun cachedProfile(): RemoteProfile? = cache.profile()
    fun cachedDailyProfile(): RemoteDailyProfile? = cache.dailyProfile()
    fun cachedModels(): RemoteModelCatalogResponse? = cache.modelCatalog()

    suspend fun refreshProfile(): RemoteProfile = api.getProfile().also(cache::putProfile)
    suspend fun refreshDailyProfile(): RemoteDailyProfile = api.getDailyProfile().also(cache::putDailyProfile)
    suspend fun refreshModels(force: Boolean = false): RemoteModelCatalogResponse = api.listModels(force).also(cache::putModelCatalog)

    suspend fun setCurrentChatModel(modelId: String): RemoteProfile {
        val current = refreshProfile()
        val updated = api.putProfile(current.copy(currentChatModel = modelId))
        cache.putProfile(updated)
        return updated
    }
}
