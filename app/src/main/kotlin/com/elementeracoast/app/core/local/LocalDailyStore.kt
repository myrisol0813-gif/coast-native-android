package com.elementeracoast.app.core.local

import com.elementeracoast.app.core.model.DailyMoment
import com.elementeracoast.app.core.model.DailyProfile
import com.elementeracoast.app.core.model.DailyState
import com.elementeracoast.app.core.model.DiaryEntry
import com.elementeracoast.app.core.model.PetStatus
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocalDailyStore(
    private val persistence: LocalPersistence,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val momentsKey = "daily.moments.v26"
    private val diariesKey = "daily.diaries.v26"
    private val profileKey = "daily.profile.v26"
    private val petKey = "daily.pet.v26"

    private val _state = MutableStateFlow(load())
    val state: StateFlow<DailyState> = _state.asStateFlow()

    fun createMoment(content: String): DailyMoment? {
        val clean = content.trim().take(4000)
        if (clean.isBlank()) return null
        val moment = DailyMoment(
            id = "moment-${UUID.randomUUID()}",
            content = clean,
            createdAt = now()
        )
        _state.update { it.copy(moments = listOf(moment) + it.moments) }
        persistMoments()
        return moment
    }

    fun editMoment(id: String, content: String): Boolean {
        val clean = content.trim().take(4000)
        if (clean.isBlank()) return false
        var changed = false
        _state.update { state ->
            state.copy(moments = state.moments.map { moment ->
                if (moment.id == id) {
                    changed = true
                    moment.copy(content = clean, updatedAt = now())
                } else moment
            })
        }
        if (changed) persistMoments()
        return changed
    }

    fun deleteMoment(id: String): Boolean {
        val before = _state.value.moments.size
        _state.update { it.copy(moments = it.moments.filterNot { moment -> moment.id == id }) }
        val changed = before != _state.value.moments.size
        if (changed) persistMoments()
        return changed
    }

    fun toggleMomentLike(id: String): Boolean {
        var changed = false
        _state.update { state ->
            state.copy(moments = state.moments.map { moment ->
                if (moment.id == id) {
                    changed = true
                    moment.copy(liked = !moment.liked, updatedAt = now())
                } else moment
            })
        }
        if (changed) persistMoments()
        return changed
    }

    fun addMomentComment(id: String, comment: String): Boolean {
        val clean = comment.trim().take(500)
        if (clean.isBlank()) return false
        var changed = false
        _state.update { state ->
            state.copy(moments = state.moments.map { moment ->
                if (moment.id == id) {
                    changed = true
                    moment.copy(comments = moment.comments + clean, updatedAt = now())
                } else moment
            })
        }
        if (changed) persistMoments()
        return changed
    }

    fun setAvatarUri(uri: String) {
        _state.update { it.copy(profile = it.profile.copy(avatarUri = uri.take(2000))) }
        persistProfile()
    }

    fun setCoverUri(uri: String) {
        _state.update { it.copy(profile = it.profile.copy(coverUri = uri.take(2000))) }
        persistProfile()
    }

    fun createDiary(
        date: String = LocalDate.now().toString(),
        weather: String,
        mood: String,
        tags: List<String>,
        content: String
    ): DiaryEntry? {
        val clean = content.trim().take(8000)
        if (clean.isBlank()) return null
        val entry = DiaryEntry(
            id = "diary-${UUID.randomUUID()}",
            date = date.take(20),
            weather = weather.trim().take(40),
            mood = mood.trim().take(40),
            tags = tags.map(String::trim).filter(String::isNotBlank).take(12),
            content = clean,
            createdAt = now()
        )
        _state.update { it.copy(diaries = listOf(entry) + it.diaries) }
        persistDiaries()
        return entry
    }

    fun editDiary(entry: DiaryEntry): Boolean {
        if (entry.content.isBlank()) return false
        var changed = false
        _state.update { state ->
            state.copy(diaries = state.diaries.map { current ->
                if (current.id == entry.id) {
                    changed = true
                    entry.copy(updatedAt = now())
                } else current
            })
        }
        if (changed) persistDiaries()
        return changed
    }

    fun deleteDiary(id: String): Boolean {
        val before = _state.value.diaries.size
        _state.update { it.copy(diaries = it.diaries.filterNot { entry -> entry.id == id }) }
        val changed = before != _state.value.diaries.size
        if (changed) persistDiaries()
        return changed
    }

    fun setPetStatus(status: PetStatus) {
        _state.update { it.copy(petStatus = status) }
        persistence.write(petKey, status.name)
    }

    private fun load(): DailyState = DailyState(
        moments = loadMoments(),
        diaries = loadDiaries(),
        profile = loadProfile(),
        petStatus = runCatching { PetStatus.valueOf(persistence.read(petKey).orEmpty()) }
            .getOrDefault(PetStatus.Resting)
    )

    private fun persistMoments() {
        persistence.write(momentsKey, _state.value.moments.joinToString("\n") { moment ->
            LocalCodec.pack(
                moment.id,
                moment.content,
                moment.liked,
                LocalCodec.encodeList(moment.comments),
                moment.createdAt,
                moment.updatedAt
            )
        })
    }

    private fun loadMoments(): List<DailyMoment> = LocalCodec.lines(persistence.read(momentsKey)).mapNotNull { f ->
        if (f.size < 6) return@mapNotNull null
        runCatching {
            DailyMoment(
                id = f[0],
                content = f[1],
                liked = f[2].toBoolean(),
                comments = LocalCodec.decodeList(f[3]),
                createdAt = f[4].toLong(),
                updatedAt = f[5].toLong()
            )
        }.getOrNull()
    }

    private fun persistDiaries() {
        persistence.write(diariesKey, _state.value.diaries.joinToString("\n") { entry ->
            LocalCodec.pack(
                entry.id,
                entry.date,
                entry.weather,
                entry.mood,
                LocalCodec.encodeList(entry.tags),
                entry.content,
                entry.createdAt,
                entry.updatedAt
            )
        })
    }

    private fun loadDiaries(): List<DiaryEntry> = LocalCodec.lines(persistence.read(diariesKey)).mapNotNull { f ->
        if (f.size < 8) return@mapNotNull null
        runCatching {
            DiaryEntry(
                id = f[0],
                date = f[1],
                weather = f[2],
                mood = f[3],
                tags = LocalCodec.decodeList(f[4]),
                content = f[5],
                createdAt = f[6].toLong(),
                updatedAt = f[7].toLong()
            )
        }.getOrNull()
    }

    private fun persistProfile() {
        val profile = _state.value.profile
        persistence.write(profileKey, LocalCodec.pack(profile.avatarUri, profile.coverUri))
    }

    private fun loadProfile(): DailyProfile {
        val f = LocalCodec.unpack(persistence.read(profileKey).orEmpty())
        return if (f.size >= 2) DailyProfile(f[0], f[1]) else DailyProfile()
    }
}
