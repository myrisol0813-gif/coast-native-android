package com.elementeracoast.app.core.local

/** A wake never implicitly gains all regular tools, nor the permissions of
 * a previous chat turn. The ordinary local read/write switches still apply. */
internal object LocalDreamToolPolicy{
    fun allowed(c:LocalDreamLedger.Claim,name:String):Boolean{
        val e=c.config.execution?:return false
        return when(name){
            "dream_read_history"->e.readHistory
            "dream_write_note"->e.writeNote
            "dream_send_message"->e.allowMessage
            "dream_memory_candidate"->e.allowMemoryCandidate
            "dream_schedule_extra"->c.config.allowExtra&&e.extraPerDay>0
            "independent_web_search","independent_web_read"->e.allowWeb
            "library_books","library_read","library_notes"->e.allowLibrary
            "memory_search"->e.allowMemory
            "global_excerpt_propose"->e.allowSelfPortraitProposal&&e.includeSelfPortrait
            "user_portrait_propose"->e.allowUserPortraitProposal&&e.includeUserPortrait
            "list_daily_moments","list_daily_diaries"->e.allowDailyRead
            "worldbook_list"->e.allowWorldbookRead
            "cross_window_search","cross_window_read_recent","cross_window_keyword_search","cross_window_read_messages"->e.allowCrossWindow
            "dream_readback"->e.allowDreamReadback
            "calendar_list"->e.allowCalendar
            "vault_search","vault_read"->e.allowCollection
            "library_write"->e.allowLibraryWrite
            "create_moment","create_diary"->e.allowDailyWrite
            "moment_comment","moment_like"->e.allowDailyInteract
            "calendar_create"->e.allowCalendarWrite
            "voice_create"->e.allowVoice&&e.voiceLimit>0
            "image_generate"->e.allowImage&&e.imageLimit>0
            else->false
        }
    }
}
