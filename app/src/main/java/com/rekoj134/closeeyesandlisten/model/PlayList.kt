package com.rekoj134.closeeyesandlisten.model

data class PlayList(
    val id: Int = 0,
    val playListName: String = "",
    val previewImage: String = "",
    val items: List<MusicItem>? = null
)