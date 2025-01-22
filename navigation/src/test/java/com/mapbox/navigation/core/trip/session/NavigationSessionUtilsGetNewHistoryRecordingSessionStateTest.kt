package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.internal.HistoryRecordingSessionState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.reflect.KClass

@RunWith(Parameterized::class)
class NavigationSessionUtilsGetNewHistoryRecordingSessionStateTest(
    private val isDriving: Boolean,
    private val hasRoutes: Boolean,
    private val expected: KClass<out HistoryRecordingSessionState>,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}, {1} -> {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(false, false, HistoryRecordingSessionState.Idle::class),
                arrayOf(false, true, HistoryRecordingSessionState.Idle::class),
                arrayOf(true, false, HistoryRecordingSessionState.FreeDrive::class),
                arrayOf(true, true, HistoryRecordingSessionState.ActiveGuidance::class),
            )
        }
    }

    @Test
    fun getNewHistoryRecordingState() {
        val actual = NavigationSessionUtils.getNewHistoryRecordingSessionState(isDriving, hasRoutes)
        assertEquals(expected, actual::class)
    }
}
