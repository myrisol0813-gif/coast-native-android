package com.elementeracoast.app.ui.theme

/** Local reading preferences; names match the PWA appearance options. */
enum class CoastFontMode(val label: String) {
    Myraes("Myraes 纸页"),
    Soft("柔和现代"),
    Note("小狗手札");

    companion object {
        fun fromStored(value: String): CoastFontMode = entries.firstOrNull { it.name == value } ?: Myraes
    }
}

enum class CoastPaperMode(val label: String) {
    Wave("轻柔撕纸边"),
    Smooth("圆润无框信纸");

    companion object {
        fun fromStored(value: String): CoastPaperMode = entries.firstOrNull { it.name == value } ?: Wave
    }
}
