package com.elementeracoast.app.model

data class CoastModel(
    val id: String,
    val name: String,
    val isFree: Boolean = false,
    val available: Boolean = true,
)
