package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.trip.session.TripSessionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class NavigationSessionTest {

    private val context: Context = mockk()
    private val appContext: Context = mockk()
    private val accounts: MapboxNavigationAccounts = mockk(relaxUnitFun = true)
    private lateinit var routes: MutableList<DirectionsRoute>
    private val route: DirectionsRoute = mockk()
    private lateinit var navigationSession: NavigationSession

    @Before
    fun setUp() {
        every { context.applicationContext } returns appContext
        mockkObject(MapboxNavigationAccounts)
        every {
            MapboxNavigationAccounts.getInstance(
                appContext
            )
        } returns accounts
        routes = mutableListOf()
        navigationSession = NavigationSession(context)
    }

    @Test
    fun drive_only() {
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        verify(exactly = 0) { accounts.navigationStarted() }
        verify(exactly = 0) { accounts.navigationStopped() }
    }

    @Test
    fun route_only() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        verify(exactly = 0) { accounts.navigationStarted() }
        verify(exactly = 0) { accounts.navigationStopped() }
    }

    @Test
    fun drive_route() {
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        verify(exactly = 1) { accounts.navigationStarted() }
    }

    @Test
    fun route_drive() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        verify(exactly = 1) { accounts.navigationStarted() }
    }

    @Test
    fun drive_route_noRoute() {
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onRoutesChanged(emptyList())
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun drive_route_noDrive() {
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun route_drive_noRoute() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onRoutesChanged(emptyList())
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun route_drive_noDrive() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun route_drive_noRoute_noDrive() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onRoutesChanged(emptyList())
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun route_drive_noDrive_noRoute() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)
        navigationSession.onRoutesChanged(emptyList())
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun restart_drive() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        verify(exactly = 2) { accounts.navigationStarted() }
    }

    @Test
    fun restart_route() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onRoutesChanged(emptyList())
        navigationSession.onRoutesChanged(routes)
        verify(exactly = 2) { accounts.navigationStarted() }
    }
}
