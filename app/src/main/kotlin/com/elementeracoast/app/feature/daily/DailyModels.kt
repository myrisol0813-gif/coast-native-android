package com.elementeracoast.app.feature.daily

import java.time.LocalDate

data class LocalMoment(
    val id: String,
    val text: String,
    val liked: Boolean = false,
    val comments: List<String> = emptyList(),
    val createdAt: String = "",
    val date: String = LocalDate.now().toString()
)

data class LocalDiary(
    val id: String,
    val date: String,
    val weather: String,
    val mood: String,
    val tags: List<String>,
    val text: String
)

enum class PetMood(val label: String) {
    Resting("休息中"), Active("活跃"), Sleepy("困倦")
}

data class DailyState(
    val moments: List<LocalMoment> = emptyList(),
    val diaries: List<LocalDiary> = emptyList(),
    val profileAvatarUri: String = "",
    val myriAvatarUri: String = "",
    val coverUri: String = "",
    val petMood: PetMood = PetMood.Resting,
    val petNote: String = ""
)
