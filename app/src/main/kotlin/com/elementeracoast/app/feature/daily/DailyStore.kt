package com.elementeracoast.app.feature.daily

import com.elementeracoast.app.core.local.LocalPersistence
import com.elementeracoast.app.core.local.LocalTextCodec
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DailyStore(private val persistence: LocalPersistence) {
    private val sequence = AtomicLong(1)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<DailyState> = _state.asStateFlow()

    fun publishMoment(
        text: String,
        date: String = LocalDate.now().toString(),
        author: MomentAuthor = MomentAuthor.Xiaohan
    ): LocalMoment? {
        val clean = text.trim()
        if (clean.isBlank()) return null
        val normalizedDate = normalizeDate(date)
        val moment = LocalMoment(
            id = id("moment"),
            text = clean.take(4000),
            createdAt = Instant.now().toString(),
            date = normalizedDate,
            author = author
        )
        update { it.copy(moments = listOf(moment) + it.moments) }
        return moment
    }

    fun editMoment(id: String, text: String) = update { state ->
        val clean = text.trim().take(4000)
        state.copy(moments = state.moments.map { if (it.id == id && clean.isNotBlank()) it.copy(text = clean) else it })
    }

    fun deleteMoment(id: String) = update { it.copy(moments = it.moments.filterNot { moment -> moment.id == id }) }

    fun toggleMomentLike(id: String) = update { state ->
        state.copy(moments = state.moments.map { if (it.id == id) it.copy(liked = !it.liked) else it })
    }

    fun addComment(id: String, comment: String) = update { state ->
        val clean = comment.trim().take(500)
        if (clean.isBlank()) state else state.copy(moments = state.moments.map {
            if (it.id == id) it.copy(comments = (it.comments + clean).takeLast(80)) else it
        })
    }

    fun saveDiary(
        id: String? = null,
        date: String = LocalDate.now().toString(),
        weather: String,
        mood: String,
        tags: List<String>,
        text: String
    ): LocalDiary? {
        val clean = text.trim()
        if (clean.isBlank()) return null
        val entry = LocalDiary(
            id = id ?: id("diary"),
            date = normalizeDate(date),
            weather = weather.trim().ifBlank { "未标注" }.take(40),
            mood = mood.trim().ifBlank { "未标注" }.take(40),
            tags = tags.map(String::trim).filter(String::isNotBlank).distinct().take(12),
            text = clean.take(12000)
        )
        update { state ->
            val exists = state.diaries.any { it.id == entry.id }
            state.copy(diaries = if (exists) state.diaries.map { if (it.id == entry.id) entry else it } else listOf(entry) + state.diaries)
        }
        return entry
    }

    fun deleteDiary(id: String) = update { it.copy(diaries = it.diaries.filterNot { entry -> entry.id == id }) }

    fun setProfileAvatar(uri: String) = update { it.copy(profileAvatarUri = uri) }
    fun setMyriAvatar(uri: String) = update { it.copy(myriAvatarUri = uri) }
    fun setCover(uri: String) = update { it.copy(coverUri = uri) }

    private fun update(transform: (DailyState) -> DailyState) {
        _state.value = transform(_state.value)
        persist(_state.value)
    }

    private fun persist(state: DailyState) {
        persistence.put(KEY_MOMENTS, state.moments.joinToString("\n") { moment ->
            LocalTextCodec.encodeFields(
                moment.id,
                moment.text,
                moment.liked.toString(),
                moment.comments.joinToString("\u001f"),
                moment.createdAt,
                moment.date,
                moment.author.name
            )
        })
        persistence.put(KEY_DIARIES, state.diaries.joinToString("\n") { diary ->
            LocalTextCodec.encodeFields(
                diary.id, diary.date, diary.weather, diary.mood, diary.tags.joinToString("\u001f"), diary.text
            )
        })
        persistence.put(KEY_AVATAR, state.profileAvatarUri)
        persistence.put(KEY_MYRI_AVATAR, state.myriAvatarUri)
        persistence.put(KEY_COVER, state.coverUri)
    }

    private fun load(): DailyState = DailyState(
        moments = persistence.get(KEY_MOMENTS).lineSequence().filter { it.isNotBlank() }.mapNotNull { line ->
            val f = LocalTextCodec.decodeFields(line)
            if (f.size < 5) null else LocalMoment(
                id = f[0],
                text = f[1],
                liked = f[2].toBoolean(),
                comments = f[3].split('\u001f').filter(String::isNotBlank),
                createdAt = f[4],
                date = f.getOrNull(5)?.takeIf(String::isNotBlank) ?: f[4].take(10).ifBlank { LocalDate.now().toString() },
                author = f.getOrNull(6)?.let { runCatching { MomentAuthor.valueOf(it) }.getOrNull() } ?: MomentAuthor.Xiaohan
            )
        }.toList(),
        diaries = persistence.get(KEY_DIARIES).lineSequence().filter { it.isNotBlank() }.mapNotNull { line ->
            val f = LocalTextCodec.decodeFields(line)
            if (f.size < 6) null else LocalDiary(
                id = f[0], date = f[1], weather = f[2], mood = f[3],
                tags = f[4].split('\u001f').filter(String::isNotBlank), text = f[5]
            )
        }.toList(),
        profileAvatarUri = persistence.get(KEY_AVATAR),
        myriAvatarUri = persistence.get(KEY_MYRI_AVATAR),
        coverUri = persistence.get(KEY_COVER)
    )

    private fun normalizeDate(value: String): String {
        val clean = value.trim().replace('/', '-')
        return runCatching { LocalDate.parse(clean).toString() }.getOrDefault(LocalDate.now().toString())
    }

    private fun id(prefix: String): String = "$prefix-${System.currentTimeMillis()}-${sequence.getAndIncrement()}"

    companion object {
        private const val KEY_MOMENTS = "daily.moments"
        private const val KEY_DIARIES = "daily.diaries"
        private const val KEY_AVATAR = "daily.profile.avatar"
        private const val KEY_MYRI_AVATAR = "daily.profile.myri-avatar"
        private const val KEY_COVER = "daily.cover"
    }
}
