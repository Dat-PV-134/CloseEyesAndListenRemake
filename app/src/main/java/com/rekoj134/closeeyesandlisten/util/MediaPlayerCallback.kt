package com.rekoj134.closeeyesandlisten.util

interface MediaPlayerCallback {
    fun onPrepared()
    fun onFailed()
    fun onDone()
}