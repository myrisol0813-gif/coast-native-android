package com.elementeracoast.app.ui.theme

/** Local reading preferences; names match the Native paper-font wardrobe. */
enum class CoastFontMode(val label: String) {
    Myraes("朱雀仿宋"),
    NanoOldSong("纳米老宋"),
    ChillHuoSong("寒蝉活宋");

    companion object {
        fun fromStored(value: String): CoastFontMode = when (value) {
            "Soft" -> NanoOldSong
            "Note" -> ChillHuoSong
            else -> entries.firstOrNull { it.name == value } ?: Myraes
        }
    }
}

enum class CoastPaperMode(val label: String) {
    Wave("轻柔撕纸边"),
    Smooth("圆润无框信纸");

    companion object {
        fun fromStored(value: String): CoastPaperMode = entries.firstOrNull { it.name == value } ?: Wave
    }
}
