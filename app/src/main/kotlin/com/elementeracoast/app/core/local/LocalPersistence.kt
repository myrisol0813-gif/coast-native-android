package com.elementeracoast.app.core.local

import android.content.Context

interface LocalPersistence {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun remove(key: String)
}

class SharedPreferencesPersistence(context: Context) : LocalPersistence {
    private val preferences = context.getSharedPreferences("elementera_native_local_v26", Context.MODE_PRIVATE)

    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}
