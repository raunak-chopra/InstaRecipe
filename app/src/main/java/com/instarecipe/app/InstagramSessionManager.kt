package com.instarecipe.app

import android.content.Context
import android.webkit.CookieManager

/**
 * Opt-in, app-private Instagram WebView session used only for the user's personal imports.
 * Never copy cookies into preferences, logs, analytics, or recipe data.
 */
object InstagramSessionManager {
    fun getCookies(context: Context): String? = runCatching {
        CookieManager.getInstance().getCookie("https://www.instagram.com")
    }.getOrNull()?.takeIf { it.contains("sessionid=") && it.contains("ds_user_id=") }

    fun isLoggedIn(context: Context): Boolean = !getCookies(context).isNullOrBlank()

    fun getUserId(context: Context): String? = getCookies(context)?.let { extractCookieValue(it, "ds_user_id") }

    fun getCsrfToken(context: Context): String? = getCookies(context)?.let { extractCookieValue(it, "csrftoken") }

    fun clearSession(context: Context) {
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }

    fun extractCookieValue(cookieString: String, key: String): String? =
        Regex("(?:^|;\\s*)${Regex.escape(key)}=([^;]+)")
            .find(cookieString)?.groupValues?.getOrNull(1)?.trim()
}
