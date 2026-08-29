package com.elementeracoast.app.core.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CoastSessionStore(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("coast_native_session", Context.MODE_PRIVATE)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.firstOrNull { it.name == COOKIE_NAME }?.let { cookie ->
            if (cookie.value.isBlank() || cookie.expiresAt <= System.currentTimeMillis()) {
                clearSession()
            } else {
                prefs.edit()
                    .putString(KEY_COOKIE_VALUE, cookie.value)
                    .putLong(KEY_COOKIE_EXPIRES, cookie.expiresAt)
                    .apply()
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (url.host != COAST_HOST) return emptyList()
        val value = prefs.getString(KEY_COOKIE_VALUE, null) ?: return emptyList()
        val expiresAt = prefs.getLong(KEY_COOKIE_EXPIRES, 0L)
        if (expiresAt <= System.currentTimeMillis()) {
            clearSession()
            return emptyList()
        }
        return listOf(
            Cookie.Builder()
                .name(COOKIE_NAME)
                .value(value)
                .hostOnlyDomain(COAST_HOST)
                .path("/")
                .expiresAt(expiresAt)
                .secure()
                .httpOnly()
                .build(),
        )
    }

    fun clearSession() {
        prefs.edit().remove(KEY_COOKIE_VALUE).remove(KEY_COOKIE_EXPIRES).apply()
    }

    fun saveSelectedModel(modelId: String) = prefs.edit().putString(KEY_MODEL, modelId).apply()
    fun selectedModel(): String = prefs.getString(KEY_MODEL, "").orEmpty()

    companion object {
        private const val COAST_HOST = "app.elementeracoast.com"
        private const val COOKIE_NAME = "__Host-coast_session"
        private const val KEY_COOKIE_VALUE = "cookie_value"
        private const val KEY_COOKIE_EXPIRES = "cookie_expires"
        private const val KEY_MODEL = "selected_model"
    }
}
