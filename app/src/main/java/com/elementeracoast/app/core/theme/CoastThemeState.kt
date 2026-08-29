package com.elementeracoast.app.core.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CoastThemeState(context: Context) {
    private val prefs = context.getSharedPreferences("coast_native_theme", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(CoastThemeMode.fromKey(prefs.getString(KEY_MODE, null)))
    val mode: StateFlow<CoastThemeMode> = _mode

    fun set(mode: CoastThemeMode) {
        _mode.value = mode
        prefs.edit().putString(KEY_MODE, mode.key).apply()
    }

    fun cycle() {
        val values = CoastThemeMode.entries
        set(values[(values.indexOf(_mode.value) + 1) % values.size])
    }

    private companion object { const val KEY_MODE = "theme_mode" }
}

enum class CoastThemeMode(val key: String, val label: String) {
    DEEP_COAST("deep_coast", "深海旧金"),
    TIDE_LIGHT("tide_light", "潮汐纸白"),
    NIGHT_GOLD("night_gold", "夜航金"),
    ;

    companion object {
        fun fromKey(key: String?): CoastThemeMode = entries.firstOrNull { it.key == key } ?: DEEP_COAST
    }
}
