package com.elementeracoast.app.core.local

import com.elementeracoast.app.core.model.CoastThemeMode
import com.elementeracoast.app.core.model.DEFAULT_LOCAL_MODELS
import com.elementeracoast.app.core.model.LocalPreferencesState
import com.elementeracoast.app.core.model.LocalProfile
import com.elementeracoast.app.core.model.RunControlSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocalPreferencesStore(
    private val persistence: LocalPersistence
) {
    private val key = "wolf.preferences.v26"
    private val _state = MutableStateFlow(load())
    val state: StateFlow<LocalPreferencesState> = _state.asStateFlow()

    fun updateProfile(nickname: String, signature: String) {
        mutate { current ->
            current.copy(
                profile = current.profile.copy(
                    nickname = nickname.trim().take(80).ifBlank { "小寒" },
                    signature = signature.trim().take(80).ifBlank { nickname.trim().take(80).ifBlank { "小寒" } }
                )
            )
        }
    }

    fun setTheme(theme: CoastThemeMode) = mutate { it.copy(theme = theme) }

    fun setUserBubble(value: String) = mutate {
        it.copy(profile = it.profile.copy(userBubble = value.ifBlank { "default" }))
    }

    fun setAccent(value: String) = mutate {
        it.copy(profile = it.profile.copy(accent = value.ifBlank { "orange" }))
    }

    fun setModel(model: String) {
        if (model !in _state.value.models) return
        mutate { it.copy(currentModel = model) }
    }

    fun updateRunControl(transform: (RunControlSettings) -> RunControlSettings) {
        mutate { it.copy(runControl = transform(it.runControl).sanitized()) }
    }

    private fun mutate(transform: (LocalPreferencesState) -> LocalPreferencesState) {
        _state.update(transform)
        save(_state.value)
    }

    private fun load(): LocalPreferencesState {
        val fields = LocalCodec.unpack(persistence.read(key).orEmpty())
        if (fields.size < 16) return LocalPreferencesState()
        return runCatching {
            LocalPreferencesState(
                theme = CoastThemeMode.valueOf(fields[0]),
                profile = LocalProfile(
                    nickname = fields[1].ifBlank { "小寒" },
                    signature = fields[2].ifBlank { "小寒" },
                    userBubble = fields[3].ifBlank { "default" },
                    accent = fields[4].ifBlank { "orange" }
                ),
                runControl = RunControlSettings(
                    recentTurns = fields[5].toInt(),
                    comfortTokens = fields[6].toInt(),
                    outputLength = fields[7],
                    maxOutputTokens = fields[8].toInt(),
                    expression = fields[9],
                    streamingEnabled = fields[10].toBooleanStrictOrNull() ?: false,
                    soilBudget = fields[11].toInt(),
                    seedCooldownTurns = fields[12].toInt(),
                    worldbookEnabled = fields[13].toBooleanStrictOrNull() ?: true,
                    worldbookLimit = fields[14].toInt(),
                    memoryLimit = fields[15].toInt()
                ).sanitized(),
                currentModel = fields.getOrNull(16)?.takeIf { it in DEFAULT_LOCAL_MODELS }
                    ?: DEFAULT_LOCAL_MODELS.first(),
                models = DEFAULT_LOCAL_MODELS
            )
        }.getOrDefault(LocalPreferencesState())
    }

    private fun save(value: LocalPreferencesState) {
        val run = value.runControl
        persistence.write(
            key,
            LocalCodec.pack(
                value.theme.name,
                value.profile.nickname,
                value.profile.signature,
                value.profile.userBubble,
                value.profile.accent,
                run.recentTurns,
                run.comfortTokens,
                run.outputLength,
                run.maxOutputTokens,
                run.expression,
                run.streamingEnabled,
                run.soilBudget,
                run.seedCooldownTurns,
                run.worldbookEnabled,
                run.worldbookLimit,
                run.memoryLimit,
                value.currentModel
            )
        )
    }
}

private fun RunControlSettings.sanitized(): RunControlSettings = copy(
    recentTurns = recentTurns.coerceIn(2, 12),
    comfortTokens = comfortTokens.coerceIn(2000, 12000),
    outputLength = outputLength.takeIf { it in setOf("auto", "short", "long") } ?: "auto",
    maxOutputTokens = maxOutputTokens.coerceIn(64, 65536),
    expression = expression.takeIf { it in setOf("stable", "balanced", "expansive") } ?: "balanced",
    soilBudget = soilBudget.coerceIn(300, 4000),
    seedCooldownTurns = seedCooldownTurns.coerceIn(0, 8),
    worldbookLimit = worldbookLimit.coerceIn(0, 6),
    memoryLimit = memoryLimit.coerceIn(0, 12)
)
