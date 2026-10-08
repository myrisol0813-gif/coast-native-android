package com.elementeracoast.app.ui.theme

import androidx.compose.ui.text.font.FontWeight

/** Local reading preferences; names match the Native paper-font wardrobe. */
enum class CoastFontMode(val label: String) {
    Myraes("朱雀仿宋"),
    NanoOldSong("纳米老宋"),
    ChillHuoSong("寒蝉活宋"),
    CustomLocal("本机字体");

    companion object {
        fun fromStored(value: String): CoastFontMode = when (value) {
            "Soft" -> NanoOldSong
            "Note" -> ChillHuoSong
            else -> entries.firstOrNull { it.name == value } ?: Myraes
        }
    }
}

enum class CoastPaperMode(val label: String) {
    Wave("雪地来信纸裁边"),
    Smooth("圆润无框信纸");

    companion object {
        fun fromStored(value: String): CoastPaperMode = entries.firstOrNull { it.name == value } ?: Wave
    }
}

enum class CoastReadingWeight(val label: String, val fontWeight: FontWeight) {
    Normal("正常", FontWeight.Normal),
    Medium("微加粗", FontWeight.Medium),
    Bold("加粗", FontWeight.Bold);

    companion object {
        fun fromStored(value: String): CoastReadingWeight = entries.firstOrNull { it.name == value } ?: Normal
    }
}
