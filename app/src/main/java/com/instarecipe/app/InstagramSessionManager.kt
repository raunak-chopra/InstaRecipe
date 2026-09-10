package com.instarecipe.app

import android.content.Context
import android.webkit.CookieManager

object InstagramSessionManager {
    fun isLoggedIn(context: Context): Boolean {
        val cookies = getCookies(context) ?: return false
        return cookies.contains("sessionid=") && cookies.contains("ds_user_id=")
    }

    fun getCookies(context: Context): String? {
        // First check CookieManager
        val webViewCookies = runCatching {
            CookieManager.getInstance().getCookie("https://www.instagram.com")
        }.getOrNull()

        return webViewCookies?.takeIf { it.contains("sessionid=") }
    }

    fun getUserId(context: Context): String? {
        val cookies = getCookies(context) ?: return null
        return extractCookieValue(cookies, "ds_user_id")
    }

    fun getCsrfToken(context: Context): String? {
        val cookies = getCookies(context) ?: return null
        return extractCookieValue(cookies, "csrftoken")
    }

    fun saveSession(context: Context, cookieString: String) {
        // CookieManager is WebView's persisted, app-private source of truth. Do not
        // duplicate authentication cookies into plaintext preferences.
        if (cookieString.contains("sessionid=")) CookieManager.getInstance().flush()
    }

    fun clearSession(context: Context) {
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
