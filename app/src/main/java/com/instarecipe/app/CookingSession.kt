package com.instarecipe.app

/** A deadline keeps time accurate across delayed frames and background suspension. */
internal data class CookingTimerState(
    val deadlineElapsedMillis: Long = 0L,
    val pausedSeconds: Int = 0,
    val running: Boolean = false,
    val bootCount: Int = 0
) {
    fun remainingSeconds(nowElapsedMillis: Long): Int = if (running) {
        ((deadlineElapsedMillis - nowElapsedMillis).coerceAtLeast(0L) + 999L).div(1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    } else pausedSeconds

    fun addSeconds(seconds: Int, nowElapsedMillis: Long, currentBoot: Int): CookingTimerState {
        val remaining = (remainingSeconds(nowElapsedMillis).toLong() + seconds).coerceIn(0L, 86_400L).toInt()
        return CookingTimerState(nowElapsedMillis + remaining * 1000L, remaining, remaining > 0, currentBoot)
    }

    fun paused(nowElapsedMillis: Long): CookingTimerState = copy(pausedSeconds = remainingSeconds(nowElapsedMillis), running = false)

    fun resumed(nowElapsedMillis: Long, currentBoot: Int): CookingTimerState =
        copy(deadlineElapsedMillis = nowElapsedMillis + pausedSeconds * 1000L, running = pausedSeconds > 0, bootCount = currentBoot)

    fun restored(currentBoot: Int): CookingTimerState = if (running && currentBoot != bootCount) CookingTimerState(bootCount = currentBoot) else this
}

internal data class CookingSession(
    val recipeId: Long,
    val step: Int,
    val servingScale: Float,
    val checkedIngredientsCsv: String,
    val timer: CookingTimerState
)
