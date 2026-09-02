package com.elementeracoast.app.core.auth

import com.elementeracoast.app.core.network.CoastApiErrorKind
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.core.network.CoastApiClient

class AuthRepository(
    private val store: AuthStore,
    private val api: CoastApiClient
) {
    suspend fun login(password: String): AuthSession {
        val session = api.login(password)
        store.save(session)
        return session
    }

    suspend fun restore(): SessionRestoreResult {
        val session = store.load() ?: return SessionRestoreResult.Missing
        if (session.isExpired()) {
            store.clear()
            return SessionRestoreResult.Invalid("登录状态已过期，请重新输入海岸密码。")
        }
        return try {
            val response = api.getSession(session)
            if (!response.authenticated) {
                store.clear()
                SessionRestoreResult.Invalid("登录状态已失效，请重新进入海岸。")
            } else {
                val refreshed = session.copy(expiresAtEpochSeconds = response.expiresAt)
                store.save(refreshed)
                SessionRestoreResult.Restored(refreshed)
            }
        } catch (error: CoastApiException) {
            if (error.kind == CoastApiErrorKind.Unauthorized) {
                store.clear()
                SessionRestoreResult.Invalid("登录状态已失效，请重新进入海岸。")
            } else {
                SessionRestoreResult.Offline(session, error.message)
            }
        }
    }

    suspend fun logout() {
        val session = store.load()
        runCatching { api.logout(session) }
        store.clear()
    }

    fun current(): AuthSession? = store.load()
    fun clearConfirmedInvalidSession() = store.clear()
}
