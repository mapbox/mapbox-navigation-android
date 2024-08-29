package com.mapbox.navigation.core.trip.session

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.testutil.createRoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.NavigationSessionState.ActiveGuidance
import com.mapbox.navigation.core.trip.session.NavigationSessionState.FreeDrive
import com.mapbox.navigation.core.trip.session.NavigationSessionState.Idle
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NavigationSessionTest {

    private val route: NavigationRoute = mockk {
        every { directionsRoute } returns mockk()
    }
    private val stateObserver: NavigationSessionStateObserver = mockk()
    private val navigationSessionStateSlot = slot<NavigationSessionState>()

    @Before
    fun setup() {
        every {
            stateObserver.onNavigationSessionStateChanged(capture(navigationSessionStateSlot))
        } just runs
    }

    @Test
    fun stateObserverImmediateIdle() {
        val navigationSession = NavigationSession()

        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                Idle,
            )
        }
    }

    @Test
    fun stateObserverImmediateActiveGuidance() {
        val routes = mutableListOf<NavigationRoute>()
        val navigationSession = NavigationSession()
        routes.add(route)
        navigationSession.onRoutesChanged(
            createRoutesUpdatedResult(routes, RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                ActiveGuidance(navigationSessionStateSlot.captured.sessionId),
            )
        }
    }

    @Test
    fun stateObserverImmediateActiveFreeDrive() {
        val navigationSession = NavigationSession()
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                FreeDrive(navigationSessionStateSlot.captured.sessionId),
            )
        }
    }

    @Test
    fun stateObserverIdle() {
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)

        verify(exactly = 2) {
            stateObserver.onNavigationSessionStateChanged(
                Idle,
            )
        }
    }

    @Test
    fun stateObserverActiveGuidance() {
        val routes = mutableListOf<NavigationRoute>()
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        routes.add(route)
        navigationSession.onRoutesChanged(
            createRoutesUpdatedResult(routes, RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                ActiveGuidance(navigationSessionStateSlot.captured.sessionId),
            )
        }
    }

    @Test
    fun stateObserverFreeDrive() {
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                FreeDrive(navigationSessionStateSlot.captured.sessionId),
            )
        }
    }

    @Test
    fun stateObserverUnregisterIdle() {
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        clearMocks(stateObserver)
        navigationSession.unregisterNavigationSessionStateObserver(stateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)

        verify(exactly = 0) {
            stateObserver.onNavigationSessionStateChanged(
                Idle,
            )
        }
    }

    @Test
    fun stateObserverUnregisterActiveGuidance() {
        val routes = mutableListOf<NavigationRoute>()
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        clearMocks(stateObserver)
        navigationSession.unregisterNavigationSessionStateObserver(stateObserver)

        routes.add(route)
        navigationSession.onRoutesChanged(
            createRoutesUpdatedResult(routes, RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            stateObserver.onNavigationSessionStateChanged(
                ActiveGuidance(navigationSessionStateSlot.captured.sessionId),
            )
        }
    }

    @Test
    fun stateObserverUnregisterFreeDrive() {
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        clearMocks(stateObserver)
        navigationSession.unregisterNavigationSessionStateObserver(stateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            stateObserver.onNavigationSessionStateChanged(
                FreeDrive(navigationSessionStateSlot.captured.sessionId),
            )
        }
    }

    @Test
    fun unregisterAllStateObservers() {
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        clearMocks(stateObserver)
        navigationSession.unregisterNavigationSessionStateObserver(stateObserver)

        navigationSession.unregisterAllNavigationSessionStateObservers()

        verify(exactly = 0) { stateObserver.onNavigationSessionStateChanged(any()) }
    }

    @Test
    fun emptySessionIdWhenIdle() {
        val mockedStateObserver: NavigationSessionStateObserver = mockk()
        val navigationSessionStateSlot = slot<NavigationSessionState>()
        every {
            mockedStateObserver.onNavigationSessionStateChanged(
                capture(
                    navigationSessionStateSlot,
                ),
            )
        } just runs
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(mockedStateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)

        assertTrue(navigationSessionStateSlot.captured.sessionId.isEmpty())
    }

    @Test
    fun nonEmptySessionIdWhenFreeDrive() {
        val mockedStateObserver: NavigationSessionStateObserver = mockk()
        val navigationSessionStateSlot = slot<NavigationSessionState>()
        every {
            mockedStateObserver.onNavigationSessionStateChanged(
                capture(
                    navigationSessionStateSlot,
                ),
            )
        } just runs
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(mockedStateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        assertTrue(navigationSessionStateSlot.captured.sessionId.isNotEmpty())
    }

    @Test
    fun nonEmptySessionIdWhenActiveGuidance() {
        val mockedStateObserver: NavigationSessionStateObserver = mockk()
        val navigationSessionStateSlot = slot<NavigationSessionState>()
        every {
            mockedStateObserver.onNavigationSessionStateChanged(
                capture(
                    navigationSessionStateSlot,
                ),
            )
        } just runs
        val routes = mutableListOf<NavigationRoute>()
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(mockedStateObserver)

        routes.add(route)
        navigationSession.onRoutesChanged(
            createRoutesUpdatedResult(routes, RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        assertTrue(navigationSessionStateSlot.captured.sessionId.isNotEmpty())
    }

    @Test
    fun navigationSessionStateHoldsSessionId() {
        val mockedStateObserver: NavigationSessionStateObserver = mockk()
        val navigationSessionStateSlots = mutableListOf<NavigationSessionState>()

        every {
            mockedStateObserver.onNavigationSessionStateChanged(
                capture(
                    navigationSessionStateSlots,
                ),
            )
        } just runs
        val routes = mutableListOf<NavigationRoute>()
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(mockedStateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        val one = navigationSessionStateSlots[1].sessionId
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)
        routes.add(
            mockk {
                every { directionsRoute } returns mockk()
            },
        )
        navigationSession.onRoutesChanged(
            createRoutesUpdatedResult(routes, RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        routes.clear()
        navigationSession.onRoutesChanged(
            createRoutesUpdatedResult(emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP),
        )
        val two = navigationSessionStateSlots[1].sessionId

        assertEquals(one, two)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        listOf(
            FreeDrive::class.java,
            ActiveGuidance::class.java,
        ).forEach {
            EqualsVerifier.forClass(it).verify()
            ToStringVerifier.forClass(it).verify()
        }
    }
}
