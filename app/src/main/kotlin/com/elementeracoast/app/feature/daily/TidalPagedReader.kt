package com.elementeracoast.app.feature.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

private fun JsonObject.field(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
private fun JsonObject.intField(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

private data class ReaderPalette(val id: String, val name: String, val paper: Color, val ink: Color, val highlight: Color)
private val palettes = listOf(
    ReaderPalette("paper", "暖纸", Color(0xFFF9F3E8), Color(0xFF3B4551), Color(0xFFEADB99)),
    ReaderPalette("white", "素白", Color.White, Color(0xFF293847), Color(0xFFF3DF9B)),
    ReaderPalette("blue", "雾蓝", Color(0xFFEDF4F8), Color(0xFF30475B), Color(0xFFD9D4A3)),
    ReaderPalette("night", "夜读", Color(0xFF222B39), Color(0xFFE6E4DC), Color(0xFF71603F))
)

@Composable
internal fun TidalPagedReader(
    bookTitle: String,
    chapterTitle: String,
    chapterIndex: Int,
    chapters: List<JsonObject>,
    paragraphs: List<JsonObject>,
    notes: List<JsonObject>,
    initialParagraph: Int,
    onChapter: (Int) -> Unit,
    onHighlight: (paragraphIndex: Int, start: Int, end: Int) -> Unit,
    onNote: (JsonObject) -> Unit,
    onProgress: (Int) -> Unit,
    onShowNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pref = remember(context.applicationContext) {
        context.applicationContext.getSharedPreferences("coast_reader_appearance", android.content.Context.MODE_PRIVATE)
    }
    var paletteId by remember { mutableStateOf(pref.getString("color", "paper").orEmpty()) }
    var fontSize by remember { mutableIntStateOf(pref.getInt("fontSize", 15).coerceIn(14, 17)) }
    val palette = palettes.find { it.id == paletteId } ?: palettes.first()
    var tocOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    val raw = remember(paragraphs) { paragraphs.map { (it.intField("index") ?: 0) to it.field("text") } }
    val pages = remember(raw, fontSize) {
        paginateTidalChapter(raw, when (fontSize) { 14 -> 620; 15 -> 520; 16 -> 440; else -> 380 })
    }
    val initial = remember(pages, initialParagraph) { tidalPageFor(pages, initialParagraph) }
    val pager = rememberPagerState(initialPage = initial, pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    LaunchedEffect(chapterIndex, fontSize, initial) {
        pager.scrollToPage(initial.coerceAtMost(pages.lastIndex))
    }
    LaunchedEffect(chapterIndex, pager.currentPage) {
        pages.getOrNull(pager.currentPage)?.lastOrNull()?.let { onProgress(it.paragraphIndex) }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 9.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box {
                TextButton(onClick = { tocOpen = true }) { Text("目录") }
                DropdownMenu(expanded = tocOpen, onDismissRequest = { tocOpen = false }) {
                    chapters.forEach { chapter ->
                        DropdownMenuItem(
                            text = { Text(chapter.field("title"), style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                tocOpen = false
                                onChapter(chapter.intField("chapter_index") ?: 0)
                            }
                        )
                    }
                }
            }
            TextButton(onClick = onShowNotes) { Text("笔记") }
            Text("第 ${pager.currentPage + 1} / ${pages.size} 页",
                modifier = Modifier.padding(top = 13.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box {
                TextButton(onClick = { settingsOpen = true }) { Text("纸色 · Aa") }
                DropdownMenu(expanded = settingsOpen, onDismissRequest = { settingsOpen = false }) {
                    palettes.forEach { option ->
                        DropdownMenuItem(text = { Text(option.name) }, onClick = {
                            paletteId = option.id
                            pref.edit().putString("color", option.id).apply()
                            settingsOpen = false
                        })
                    }
                    listOf(14, 15, 16, 17).forEach { point ->
                        DropdownMenuItem(text = { Text("字号 $point") }, onClick = {
                            fontSize = point
                            pref.edit().putInt("fontSize", point).apply()
                            settingsOpen = false
                        })
                    }
                }
            }
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().weight(1f),
            beyondViewportPageCount = 1
        ) { index ->
            Surface(
                color = palette.paper,
                contentColor = palette.ink,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 5.dp),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(bookTitle, style = MaterialTheme.typography.labelSmall,
                        color = palette.ink.copy(alpha = .65f))
                    Text(chapterTitle, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    pages[index].forEach { piece ->
                        SelectableBookText(
                            fragment = piece,
                            notes = notes.filter {
                                it.intField("chapter_index") == chapterIndex &&
                                    it.intField("paragraph_index") == piece.paragraphIndex
                            },
                            ink = palette.ink, highlight = palette.highlight, fontSize = fontSize,
                            onHighlight = { start, end -> onHighlight(piece.paragraphIndex, start, end) },
                            onNote = onNote
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(enabled = pager.currentPage > 0, onClick = {
                scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
            }) { Text("‹ 上一页") }
            TextButton(enabled = pager.currentPage < pages.lastIndex, onClick = {
                scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }) { Text("下一页 ›") }
        }
    }
}

@Composable
private fun SelectableBookText(
    fragment: TidalSegment,
    notes: List<JsonObject>,
    ink: Color,
    highlight: Color,
    fontSize: Int,
    onHighlight: (Int, Int) -> Unit,
    onNote: (JsonObject) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val decorated = remember(fragment, notes, highlight) {
        buildAnnotatedString {
            append(fragment.text)
            notes.forEach { note ->
                val begin = note.intField("start_offset") ?: return@forEach
                val end = note.intField("end_offset") ?: return@forEach
                val from = maxOf(begin, fragment.start) - fragment.start
                val to = minOf(end, fragment.end) - fragment.start
                if (to > from) addStyle(SpanStyle(background = highlight), from, to)
            }
        }
    }
    var textValue by remember(decorated) { mutableStateOf(TextFieldValue(decorated, TextRange.Zero)) }
    var selected by remember { mutableStateOf<TextRange?>(null) }
    var tappedNote by remember { mutableStateOf<JsonObject?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            BasicTextField(
                value = textValue,
                onValueChange = { changed ->
                    textValue = changed.copy(annotatedString = decorated)
                    val range = changed.selection
                    if (!range.collapsed) {
                        selected = range
                        tappedNote = null
                    } else {
                        selected = null
                        val point = fragment.start + range.start
                        tappedNote = notes.firstOrNull { note ->
                            val from = note.intField("start_offset")
                            val to = note.intField("end_offset")
                            from != null && to != null && point in from until to
                        }
                    }
                },
                readOnly = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = ink,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.73f).sp,
                    letterSpacing = .1.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(expanded = selected != null, onDismissRequest = { selected = null }) {
                DropdownMenuItem(text = { Text("复制") }, onClick = {
                    selected?.let { range ->
                        val from = range.min.coerceIn(0, fragment.text.length)
                        val end = range.max.coerceIn(from, fragment.text.length)
                        clipboard.setText(AnnotatedString(fragment.text.substring(from, end)))
                    }
                    selected = null
                })
                DropdownMenuItem(text = { Text("划线") }, onClick = {
                    selected?.let { range ->
                        val from = range.min.coerceIn(0, fragment.text.length)
                        val end = range.max.coerceIn(from, fragment.text.length)
                        if (end > from) onHighlight(fragment.start + from, fragment.start + end)
                    }
                    selected = null
                })
            }
            DropdownMenu(expanded = tappedNote != null, onDismissRequest = { tappedNote = null }) {
                tappedNote?.let { note ->
                    DropdownMenuItem(text = {
                        Column {
                            Text(if (note.field("author") == "xiaohan") "小寒的划线" else "Myri 的划线",
                                style = MaterialTheme.typography.labelSmall)
                            if (note.field("body").isNotBlank()) Text(note.field("body"))
                        }
                    }, onClick = { tappedNote = null })
                    if (note.field("author") == "xiaohan") {
                        DropdownMenuItem(text = { Text(if (note.field("kind") == "highlight") "写批注" else "编辑批注") },
                            onClick = { tappedNote = null; onNote(note) })
                    }
                }
            }
        }
        // Preserve legacy paragraph-level annotations on their original page.
        if (fragment.start == 0) notes.filter {
            it.field("kind") == "annotation" && it.intField("start_offset") == null
        }.forEach { note ->
            Text("${if (note.field("author") == "xiaohan") "小寒" else "Myri"}：${note.field("body")}",
                fontSize = 12.sp, lineHeight = 19.sp, color = ink.copy(alpha = .77f))
        }
    }
}
