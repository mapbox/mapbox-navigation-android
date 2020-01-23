package com.mapbox.navigation.core.internal.accounts

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
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
        every { MapboxNavigationAccounts.getInstance(appContext) } returns accounts
        routes = mutableListOf()
        navigationSession = NavigationSession(context)
    }

    @Test
    fun drive_only() {
        navigationSession.onSessionStarted()
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
        navigationSession.onSessionStarted()
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        verify(exactly = 1) { accounts.navigationStarted() }
    }

    @Test
    fun route_drive() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStarted()
        verify(exactly = 1) { accounts.navigationStarted() }
    }

    @Test
    fun drive_route_noRoute() {
        navigationSession.onSessionStarted()
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onRoutesChanged(emptyList())
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun drive_route_noDrive() {
        navigationSession.onSessionStarted()
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStopped()
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun route_drive_noRoute() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStarted()
        navigationSession.onRoutesChanged(emptyList())
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun route_drive_noDrive() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStarted()
        navigationSession.onSessionStopped()
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun route_drive_noRoute_noDrive() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStarted()
        navigationSession.onRoutesChanged(emptyList())
        navigationSession.onSessionStopped()
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun route_drive_noDrive_noRoute() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStarted()
        navigationSession.onSessionStopped()
        navigationSession.onRoutesChanged(emptyList())
        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun restart_drive() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStarted()
        navigationSession.onSessionStopped()
        navigationSession.onSessionStarted()
        verify(exactly = 2) { accounts.navigationStarted() }
    }

    @Test
    fun restart_route() {
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStarted()
        navigationSession.onRoutesChanged(emptyList())
        navigationSession.onRoutesChanged(routes)
        verify(exactly = 2) { accounts.navigationStarted() }
    }
}
