package com.mapbox.navigation.core.trip.session

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.reflect.KClass

@RunWith(Parameterized::class)
class NavigationSessionUtilsGetNewNavigationSessionStateTest(
    private val isDriving: Boolean,
    private val hasRoutes: Boolean,
    private val expected: KClass<out NavigationSessionState>,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}, {1} -> {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(false, false, NavigationSessionState.Idle::class),
                arrayOf(false, true, NavigationSessionState.Idle::class),
                arrayOf(true, false, NavigationSessionState.FreeDrive::class),
                arrayOf(true, true, NavigationSessionState.ActiveGuidance::class),
            )
        }
    }

    @Test
    fun getNewNavigationSessionState() {
        val actual = NavigationSessionUtils.getNewNavigationSessionState(isDriving, hasRoutes)
        assertEquals(expected, actual::class)
    }
}
