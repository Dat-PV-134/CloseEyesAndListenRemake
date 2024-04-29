package com.rekoj134.closeeyesandlisten.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class MusicItem(
     val id: Int? = 0,
     val name: String? = null,
     val author: String? = null,
     val playList: String? = null,
     val image: String? = null,
     val src: String? = null,
     var isFavorite: Boolean = false
)