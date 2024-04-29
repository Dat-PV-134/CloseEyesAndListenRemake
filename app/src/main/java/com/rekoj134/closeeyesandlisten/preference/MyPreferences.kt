package com.rekoj134.closeeyesandlisten.preference

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class MyPreferences private constructor() {
    companion object {
        private val myPreferences = MyPreferences()
        private lateinit var sharedPreferences: SharedPreferences
        private const val SHARED_PREFERENCES = "shared_preferences"
        private const val PREF_LANGUAGE = "pref_language"

        fun getInstance(context: Context) : MyPreferences {
            if (!this::sharedPreferences.isInitialized) {
                synchronized(MyPreferences::class.java) {
                    sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
                }
            }
            return myPreferences
        }
    }

    fun getPrefLanguage() : String? {
        return sharedPreferences.getString(PREF_LANGUAGE, null)
    }

    fun setPrefLanguage(value: String?) {
        sharedPreferences.edit().putString(PREF_LANGUAGE, value).apply()
    }
}