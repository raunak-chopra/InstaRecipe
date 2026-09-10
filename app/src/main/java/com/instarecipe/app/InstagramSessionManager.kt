package com.instarecipe.app

import android.content.Context
import android.webkit.CookieManager

object InstagramSessionManager {
    private const val PREFS_NAME = "instagram_session_prefs"
    private const val KEY_COOKIES = "saved_cookies"
    private const val KEY_USER_ID = "saved_user_id"
    private const val KEY_CONNECTED = "is_connected"

    fun isLoggedIn(context: Context): Boolean {
        val cookies = getCookies(context) ?: return false
        return cookies.contains("sessionid=") && cookies.contains("ds_user_id=")
    }

    fun getCookies(context: Context): String? {
        // First check CookieManager
        val webViewCookies = runCatching {
            CookieManager.getInstance().getCookie("https://www.instagram.com")
        }.getOrNull()

        if (!webViewCookies.isNullOrBlank() && webViewCookies.contains("sessionid=")) {
            // Keep preferences in sync
            saveSession(context, webViewCookies)
            return webViewCookies
        }

        // Fallback to persisted preferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_COOKIES, null)?.takeIf { it.contains("sessionid=") }
    }

    fun getUserId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedId = prefs.getString(KEY_USER_ID, null)
        if (!storedId.isNullOrBlank()) return storedId

        val cookies = getCookies(context) ?: return null
        return extractCookieValue(cookies, "ds_user_id")
    }

    fun getCsrfToken(context: Context): String? {
        val cookies = getCookies(context) ?: return null
        return extractCookieValue(cookies, "csrftoken")
    }

    fun saveSession(context: Context, cookieString: String) {
        val userId = extractCookieValue(cookieString, "ds_user_id")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COOKIES, cookieString)
            .putString(KEY_USER_ID, userId)
            .putBoolean(KEY_CONNECTED, true)
            .apply()
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        runCatching {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
        }
    }

    fun extractCookieValue(cookieString: String, key: String): String? {
        val pattern = Regex("""(?:^|;\s*)${Regex.escape(key)}=([^;]+)""")
        return pattern.find(cookieString)?.groupValues?.getOrNull(1)?.trim()
    }
}
