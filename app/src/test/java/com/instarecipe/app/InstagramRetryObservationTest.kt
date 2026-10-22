package com.instarecipe.app

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstagramRetryObservationTest {
    private val requestedId = UUID.randomUUID()
    private val retryTag = InstagramExtractionWork.explicitRetryTag(7L, "fingerprint")

    @Test
    fun alreadyTerminalRequestedWorkIsStillSelected() {
        val terminal = InstagramRetryWorkSnapshot(requestedId, isFinished = true, tags = setOf(retryTag))

        assertEquals(
            terminal,
            selectInstagramRetryWork(listOf(terminal), requestedId, retryTag)
        )
    }

    @Test
    fun compatibleRetainedRetryIsSelectedWhenKeepDiscardsNewRequest() {
        val retained = InstagramRetryWorkSnapshot(
            UUID.randomUUID(),
            isFinished = false,
            tags = setOf(retryTag)
        )

        assertEquals(
            retained,
            selectInstagramRetryWork(listOf(retained), requestedId, retryTag)
        )
    }

    @Test
    fun unrelatedRetainedImportIsRejected() {
        val unrelated = InstagramRetryWorkSnapshot(
            UUID.randomUUID(),
            isFinished = false,
            tags = setOf("instagram-first-import")
        )

        assertNull(selectInstagramRetryWork(listOf(unrelated), requestedId, retryTag))
    }

    @Test
    fun retainedRetryIsStillSelectedAfterItFinishesBeforeObservation() {
        val retainedId = UUID.randomUUID()
        val retained = InstagramRetryWorkSnapshot(
            retainedId,
            isFinished = true,
            tags = setOf(retryTag)
        )

        assertEquals(
            retained,
            selectInstagramRetryWork(listOf(retained), retainedId, retryTag)
        )
    }
}
