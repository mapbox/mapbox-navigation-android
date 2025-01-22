package com.mapbox.navigation.core.trip.session

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NavigationSessionUtilsIsDrivingTest(
    private val state: TripSessionState,
    private val expected: Boolean,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} -> {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(TripSessionState.STARTED, true),
                arrayOf(TripSessionState.STOPPED, false),
            )
        }
    }

    @Test
    fun isDriving() {
        val actual = NavigationSessionUtils.isDriving(state)
        assertEquals(expected, actual)
    }
}
