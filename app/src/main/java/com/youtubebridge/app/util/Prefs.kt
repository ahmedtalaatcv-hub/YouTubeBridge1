package com.youtubebridge.app.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight persistent settings store.
 * Kept intentionally simple (SharedPreferences) instead of DataStore to avoid
 * extra coroutine plumbing for a handful of primitive flags.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("youtube_bridge_prefs", Context.MODE_PRIVATE)

    var port: Int
        get() = sp.getInt(KEY_PORT, DEFAULT_PORT)
        set(value) = sp.edit().putInt(KEY_PORT, value).apply()

    var autoStart: Boolean
        get() = sp.getBoolean(KEY_AUTO_START, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_START, value).apply()

    var darkMode: Boolean
        get() = sp.getBoolean(KEY_DARK_MODE, true)
        set(value) = sp.edit().putBoolean(KEY_DARK_MODE, value).apply()

    companion object {
        private const val KEY_PORT = "port"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_DARK_MODE = "dark_mode"
        const val DEFAULT_PORT = 3000
    }
}
