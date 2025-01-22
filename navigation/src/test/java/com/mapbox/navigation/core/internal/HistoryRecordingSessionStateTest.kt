package com.mapbox.navigation.core.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryRecordingSessionStateTest {

    @Test
    fun `Idle sessionId is empty`() {
        assertEquals("", HistoryRecordingSessionState.Idle.sessionId)
    }
}
