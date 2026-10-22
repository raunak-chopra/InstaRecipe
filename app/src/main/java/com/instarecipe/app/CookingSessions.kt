package com.instarecipe.app

import android.content.Context
import android.provider.Settings

/** One current cooking session, stored separately from recipe data and API-key preferences. */
internal object CookingSessions {
    private const val FILE = "instarecipe_cooking_session"

    fun bootCount(context: Context): Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)

    fun load(context: Context, recipeId: Long? = null): CookingSession? {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val savedId = prefs.getLong("recipe_id", 0L)
        if (savedId <= 0L || (recipeId != null && recipeId != savedId)) return null
        val timer = CookingTimerState(
            prefs.getLong("timer_deadline", 0L), prefs.getInt("timer_paused_seconds", 0),
            prefs.getBoolean("timer_running", false), prefs.getInt("timer_boot", 0)
        ).restored(bootCount(context))
        return CookingSession(savedId, prefs.getInt("step", 0).coerceAtLeast(0),
            prefs.getFloat("scale", 1f).takeIf { it.isFinite() && it > 0f } ?: 1f,
            prefs.getString("checked", "").orEmpty(), timer)
    }

    fun save(context: Context, session: CookingSession) {
        if (session.recipeId <= 0L) return
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putLong("recipe_id", session.recipeId).putInt("step", session.step)
            .putFloat("scale", session.servingScale).putString("checked", session.checkedIngredientsCsv)
            .putLong("timer_deadline", session.timer.deadlineElapsedMillis)
            .putInt("timer_paused_seconds", session.timer.pausedSeconds)
            .putBoolean("timer_running", session.timer.running).putInt("timer_boot", session.timer.bootCount).apply()
    }

    fun clear(context: Context, recipeId: Long) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getLong("recipe_id", 0L) == recipeId) prefs.edit().clear().apply()
    }
}
