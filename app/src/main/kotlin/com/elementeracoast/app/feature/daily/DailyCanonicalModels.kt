package com.elementeracoast.app.feature.daily

data class DailyComment(
    val id: String,
    val author: String,
    val text: String,
    val modelId: String? = null,
    val createdAt: String = ""
) {
    val authorLabel: String get() = if (author == "xiaohan") "小寒" else "Myri"
}

data class DailyMoment(
    val id: String,
    val date: String,
    val author: String,
    val source: String,
    val text: String,
    val displayAuthor: String,
    val modelLabel: String? = null,
    val symbol: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val liked: Boolean = false,
    val likeCount: Int = 0,
    val comments: List<DailyComment> = emptyList()
) {
    val isXiaohan: Boolean get() = author == "xiaohan"
}

data class DailyDiary(
    val id: String,
    val date: String,
    val author: String,
    val source: String,
    val weather: String,
    val mood: String,
    val tags: List<String>,
    val text: String,
    val displayAuthor: String,
    val modelLabel: String? = null,
    val symbol: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class DailyProfile(
    val xiaohanAvatarDataUrl: String = "",
    val myriAvatarDataUrl: String = "",
    val momentCoverDataUrl: String = "",
    val updatedAt: String? = null
)

data class DailySnapshot(
    val moments: List<DailyMoment> = emptyList(),
    val diaries: List<DailyDiary> = emptyList(),
    val profile: DailyProfile = DailyProfile()
)

enum class DailyProfileImageField { XiaohanAvatar, MyriAvatar, MomentCover }
