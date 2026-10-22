package com.instarecipe.app

import org.junit.Assert.*
import org.junit.Test

class CookingTimerStateTest {
    @Test fun delayedUpdatesUseTheDeadlineRatherThanCountingTicks() {
        val timer = CookingTimerState().addSeconds(60, 1_000L, 7)
        assertEquals(60, timer.remainingSeconds(1_000L))
        assertEquals(15, timer.remainingSeconds(46_001L))
        assertEquals(0, timer.remainingSeconds(90_000L))
    }

    @Test fun pauseAndResumeKeepOnlyTheRemainingTime() {
        val paused = CookingTimerState().addSeconds(60, 1_000L, 7).paused(21_000L)
        assertFalse(paused.running)
        assertEquals(40, paused.remainingSeconds(800_000L))
        assertEquals(30, paused.resumed(900_000L, 7).remainingSeconds(910_000L))
    }

    @Test fun addingTimeUsesTheActualRemainingDeadline() {
        val timer = CookingTimerState().addSeconds(60, 1_000L, 7)
        assertEquals(75, timer.addSeconds(60, 46_000L, 7).remainingSeconds(46_000L))
    }

    @Test fun aRunningTimerSurvivesProcessRestorationButNotAReboot() {
        val timer = CookingTimerState().addSeconds(60, 1_000L, 7)
        assertEquals(timer, timer.restored(7))
        assertFalse(timer.restored(8).running)
        assertEquals(0, timer.restored(8).remainingSeconds(0L))
    }
}
