package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RouteAlternativesCacheManagerTest {

    private lateinit var routeAlternativesCacheManager: RouteAlternativesCacheManager
    private lateinit var navigationSession: NavigationSession
    private val navSessionObserverSlot = CapturingSlot<NavigationSessionStateObserver>()

    private val mockFirstRoute = mockk<DirectionsRoute>()
    private val mockSecondRoute = mockk<DirectionsRoute>()

    @Before
    fun setup() {
        navigationSession = mockk {
            every {
                registerNavigationSessionStateObserver(capture(navSessionObserverSlot))
            } just runs
        }
        routeAlternativesCacheManager = RouteAlternativesCacheManager(navigationSession)
    }

    @Test
    fun sanity() {
        assertNotNull(routeAlternativesCacheManager)
        assertNotNull(navigationSession)
    }

    @Test
    fun checkSameRoute() {
        nextState(NavigationSessionState.ActiveGuidance("-1"))

        routeAlternativesCacheManager.push(listOf(mockFirstRoute, mockSecondRoute))

        assertTrue(routeAlternativesCacheManager.areAlternatives(listOf(mockFirstRoute)))
    }

    @Test
    fun checkNotSameRoute() {
        nextState(NavigationSessionState.ActiveGuidance("-1"))

        routeAlternativesCacheManager.push(listOf(mockFirstRoute, mockSecondRoute))

        assertFalse(routeAlternativesCacheManager.areAlternatives(listOf(mockk())))
    }

    @Test
    fun cachedRoutesCleanUpWhenStateIsChangedToFreeDrive() {
        val alternatives = listOf(mockFirstRoute, mockSecondRoute)

        nextState(NavigationSessionState.ActiveGuidance("-1"))
        routeAlternativesCacheManager.push(alternatives)
        nextState(NavigationSessionState.FreeDrive("-1"))

        assertFalse(routeAlternativesCacheManager.areAlternatives(alternatives))
    }

    @Test
    fun cachedRoutesCleanUpWhenStateIsChangedToIdle() {
        val alternatives = listOf(mockFirstRoute, mockSecondRoute)

        nextState(NavigationSessionState.Idle)
        routeAlternativesCacheManager.push(alternatives)
        nextState(NavigationSessionState.FreeDrive("-1"))

        assertFalse(routeAlternativesCacheManager.areAlternatives(alternatives))
    }

    private fun nextState(navSessionState: NavigationSessionState) {
        navSessionObserverSlot.captured.onNavigationSessionStateChanged(navSessionState)
    }
}
