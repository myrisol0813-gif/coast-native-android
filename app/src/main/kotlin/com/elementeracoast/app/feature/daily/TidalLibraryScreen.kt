package com.elementeracoast.app.feature.daily

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import androidx.compose.ui.platform.LocalContext

private fun JsonObject.s(name: String): String = this[name]?.jsonPrimitive?.contentOrNull.orEmpty()
private fun JsonObject.n(name: String): Int = this[name]?.jsonPrimitive?.intOrNull ?: 0
private fun JsonObject.rows(name: String): List<JsonObject> = (this[name] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }

private enum class LibraryPage { Shelf, Book, Chapter, Notes }
@Composable
fun TidalLibraryScreen(repository: SideRoomsRepository, onSnackbar: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(LibraryPage.Shelf) }
    var books by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var bookId by remember { mutableStateOf("") }
    var book by remember { mutableStateOf<JsonObject?>(null) }
    var notes by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var chapter by remember { mutableStateOf<JsonObject?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var start by remember { mutableIntStateOf(0) }
    var focus by remember { mutableIntStateOf(-1) }
    var loading by remember { mutableStateOf(true) }
    var revision by remember { mutableIntStateOf(0) }
    var writeNote by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<JsonObject?>(null) }
    var annotationParagraph by remember { mutableIntStateOf(-1) }
    var pendingBookDelete by remember { mutableStateOf(false) }
    var pendingNoteDelete by remember { mutableStateOf<JsonObject?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            loading = true
            try {
                val request = LibraryImport.parse(context, uri)
                val result = repository.importBook(request)
                bookId = result["book"]?.jsonObject?.s("id").orEmpty()
                page = LibraryPage.Book
                revision++
                onSnackbar("书籍已经放进潮中书房。")
            } catch (error: Exception) { onSnackbar("导入失败：${error.message}") }
            finally { loading = false }
        }
    }
    LaunchedEffect(page, bookId, index, start, revision) {
        loading = true
        try {
            when (page) {
                LibraryPage.Shelf -> { books = repository.books().rows("books") }
                LibraryPage.Book -> {
                    book = repository.book(bookId)["book"]?.jsonObject
                    notes = repository.notes(bookId).rows("notes")
                }
                LibraryPage.Notes -> {
                    book = repository.book(bookId)["book"]?.jsonObject
                    notes = repository.notes(bookId).rows("notes")
                }
                LibraryPage.Chapter -> {
                    chapter = repository.chapter(bookId, index, start)["chapter"]?.jsonObject
                    notes = repository.notes(bookId).rows("notes")
                    val last = chapter?.rows("paragraphs")?.lastOrNull()
                    if (last != null) repository.progress(bookId, index, last.n("index"))
                }
            }
        } catch (error: Exception) { onSnackbar("潮中书房暂时无法读取：${error.message}") }
        finally { loading = false }
    }
    fun openBook(id: String) { bookId = id; page = LibraryPage.Book }
    fun openChapter(chapterIndex: Int, paragraph: Int = 0) {
        index = chapterIndex; start = (paragraph / 12) * 12
        focus = paragraph; page = LibraryPage.Chapter
    }
    fun edit(note: JsonObject?, paragraph: Int = -1) {
        editingNote = note; annotationParagraph = paragraph; writeNote = true
    }
    val surface = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 15.dp)
    Column(surface, verticalArrangement = Arrangement.spacedBy(13.dp)) {
        when (page) {
            LibraryPage.Shelf -> {
                Text("我们共用一间书房，各自留笔迹。", style = MaterialTheme.typography.bodyMedium)
                Text("支持 EPUB 和 TXT 文字阅读，暂不支持 PDF、图片与复杂版式。文件最多 8 MB、正文最多 60 万字、160 章。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                DailyPrimaryButton("＋ 导入书籍") { filePicker.launch(arrayOf("*/*")) }
                if (books.isEmpty() && !loading) Text("书架还空着。挑一本我们一起读的书吧。")
                books.forEach { entry ->
                    DailySurfaceCard(onClick = { openBook(entry.s("id")) }) {
                        Text(entry.s("title"), fontWeight = FontWeight.SemiBold)
                        Text("${entry.s("format").uppercase()} · ${entry.n("chapters_count")} 章",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            LibraryPage.Book -> {
                TextButton(onClick = { page = LibraryPage.Shelf }) { Text("‹ 书架") }
                val current = book
                if (current != null) {
                    Text(current.s("title"), style = MaterialTheme.typography.titleLarge)
                    val progress = current.rows("progress")
                    progress.forEach { p -> Text(
                        "${if (p.s("author") == "xiaohan") "小寒" else "Myri"} · 第 ${p.n("chapter_index") + 1} 章 第 ${p.n("paragraph_index") + 1} 段",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) }
                    current.rows("chapters").forEach { chapterEntry ->
                        DailySurfaceCard(onClick = { openChapter(chapterEntry.n("chapter_index")) }) {
                            Text(chapterEntry.s("title"), fontWeight = FontWeight.Medium)
                        }
                    }
                    TextButton(onClick = { page = LibraryPage.Notes }) { Text("查看双方批注与读后感 ›") }
                    val reflections = notes.filter { it.s("kind") == "reflection" }
                    Text("读后感", fontWeight = FontWeight.Bold)
                    reflections.forEach { note -> NoteBlock(note,
                        onJump = {  }, onEdit = { edit(note) }, onDelete = { pendingNoteDelete = note }) }
                    TextButton(onClick = { edit(null) }) { Text("＋ 写一篇读后感") }
                    TextButton(onClick = { pendingBookDelete = true }) { Text("删除整本书及全部笔记") }
                }
            }
            LibraryPage.Chapter -> {
                TextButton(onClick = { page = LibraryPage.Book }) { Text("‹ 目录") }
                val pageData = chapter
                if (pageData != null) {
                    Text(pageData.s("chapter_title"), style = MaterialTheme.typography.titleLarge)
                    pageData.rows("paragraphs").forEach { paragraph ->
                        DailySurfaceCard {
                            Text(paragraph.s("text"),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = androidx.compose.ui.unit.TextUnit(29f, androidx.compose.ui.unit.TextUnitType.Sp)
                                ))
                            TextButton(onClick = { edit(null, paragraph.n("index")) }) { Text("划线 · 写批注") }
                            notes.filter { note -> note.s("kind") == "annotation" && note.n("chapter_index") == index
                                && note.n("paragraph_index") == paragraph.n("index") }.forEach { note ->
                                NoteBlock(note,onJump={},
                                    onEdit={ edit(note) },onDelete={ pendingNoteDelete = note })
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = start > 0, onClick = { start = (start-12).coerceAtLeast(0) }) { Text("‹ 上一页") }
                        Text("第 ${start+1} 段起",style=MaterialTheme.typography.labelSmall)
                        val next = pageData["next_paragraph"]?.jsonPrimitive?.intOrNull
                        TextButton(enabled = next != null, onClick = { if (next != null) start = next }) { Text("下一页 ›") }
                    }
                }
            }
            LibraryPage.Notes -> {
                TextButton(onClick = { page = LibraryPage.Book }) { Text("‹ 返回目录") }
                Text("批注与读后感",style=MaterialTheme.typography.titleLarge)
                for (author in listOf("xiaohan","myri")) {
                    Text(if (author=="xiaohan") "小寒的笔记" else "Myri 的笔记", fontWeight = FontWeight.Bold)
                    notes.filter { it.s("author")==author }.forEach { note ->
                        NoteBlock(note, onJump = {
                            if (note.s("kind")=="annotation") openChapter(note.n("chapter_index"),note.n("paragraph_index"))
                        },onEdit={edit(note)},onDelete={pendingNoteDelete=note})
                    }
                }
            }
        }
        if (loading) Text("正在翻阅海岸书房…",color=MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (writeNote) {
        val current=editingNote
        var body by remember(current,annotationParagraph) { mutableStateOf(current?.s("body").orEmpty()) }
        val kind=current?.s("kind") ?: if (annotationParagraph >= 0) "annotation" else "reflection"
        AlertDialog(
            onDismissRequest = { writeNote = false }, title = { Text(if (kind=="annotation") "书页边写字" else "留一篇读后感") },
            text = { DailyField("正文 · 1—4000 字",body,{body=it.take(4000)},minLines=5,maxLines=10) },
            confirmButton = { TextButton(onClick = {
                if(body.isBlank()){onSnackbar("先写一点批注。");return@TextButton}
                scope.launch {
                    try {
                        if(current!=null)repository.editNote(current.s("id"),buildJsonObject { put("body",body) })
                        else repository.writeNote(bookId,buildJsonObject {
                            put("kind",kind);put("body",body)
                            if(kind=="annotation"){
                                put("chapter_index",index);put("paragraph_index",annotationParagraph)
                            }
                        })
                        writeNote=false;revision++
                        onSnackbar("小寒的笔记已经留在书页边上。")
                    } catch (e: Exception) {onSnackbar("笔记未保存：${e.message}")}
                }
            }) { Text("保存") } },
            dismissButton = {TextButton(onClick={writeNote=false}){Text("取消")} }
        )
    }
    pendingNoteDelete?.let { note ->
        AlertDialog(onDismissRequest = { pendingNoteDelete = null },
            title={Text("删去这条笔记？")},
            text={Text(note.s("body"))},
            confirmButton={TextButton(onClick={
                pendingNoteDelete=null
                scope.launch { try {repository.deleteNote(note.s("id"));revision++}
                    catch(e:Exception){onSnackbar("删除失败：${e.message}")} }
            }){Text("删除")}},
            dismissButton={TextButton(onClick={pendingNoteDelete=null}){Text("取消")}}
        )
    }
    if(pendingBookDelete) {
        AlertDialog(onDismissRequest={pendingBookDelete=false},title={Text("删除这本书？")},
            text={Text("将连同 Myri 和小寒在这里写的全部批注、读后感与阅读进度一起删除，无法撤销。")},
            confirmButton={TextButton(onClick={
                pendingBookDelete=false;scope.launch{
                    try{repository.deleteBook(bookId);page=LibraryPage.Shelf;revision++}
                    catch(e:Exception){onSnackbar("删除失败：${e.message}")}
                }
            }){Text("确认删除")}},
            dismissButton={TextButton(onClick={pendingBookDelete=false}){Text("保留书籍")}})
    }
}

@Composable
private fun NoteBlock(note: JsonObject, onJump:()->Unit, onEdit:()->Unit, onDelete:()->Unit) {
    val isHuman=note.s("author")=="xiaohan"
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text("${if(isHuman)"小寒" else "Myri"} · ${if(note.s("kind")=="annotation")"批注" else "读后感"}",
            style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(note.s("kind")=="annotation") Text(
            note.s("quote").take(180),modifier=Modifier.clickable(onClick=onJump),
            style=MaterialTheme.typography.bodySmall.copy(color=MaterialTheme.colorScheme.primary,textDecoration=TextDecoration.Underline))
        Text(note.s("body"),style=MaterialTheme.typography.bodyMedium)
        if(isHuman) Row {
            TextButton(onClick=onEdit){Text("编辑")}
            TextButton(onClick=onDelete){Text("删除")}
        }
    }
}
