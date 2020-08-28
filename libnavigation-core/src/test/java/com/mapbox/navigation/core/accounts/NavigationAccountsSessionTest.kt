package com.mapbox.navigation.core.accounts

import android.content.Context
import com.mapbox.navigation.core.NavigationSession
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class NavigationAccountsSessionTest {

    private val context: Context = mockk()
    private val appContext: Context = mockk()
    private val accounts: MapboxNavigationAccounts = mockk(relaxUnitFun = true)

    @Before
    fun setUp() {
        every { context.applicationContext } returns appContext
        mockkObject(MapboxNavigationAccounts)
        every {
            MapboxNavigationAccounts.getInstance(
                appContext
            )
        } returns accounts
    }

    @Test
    fun previousIdleCurrentIdleNeitherStartedNorStoppedAreCalled() {
        val navigationAccountsSession = NavigationAccountsSession(context)

        navigationAccountsSession.onNavigationSessionStateChanged(NavigationSession.State.IDLE)

        verify(exactly = 0) { accounts.navigationStarted() }
        verify(exactly = 0) { accounts.navigationStopped() }
    }

    @Test
    fun previousIdleCurrentFreeDriveNeitherStartedNorStoppedAreCalled() {
        val navigationAccountsSession = NavigationAccountsSession(context)

        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.FREE_DRIVE)

        verify(exactly = 0) { accounts.navigationStarted() }
        verify(exactly = 0) { accounts.navigationStopped() }
    }

    @Test
    fun previousIdleCurrentActiveGuidanceStartedIsCalled() {
        val navigationAccountsSession = NavigationAccountsSession(context)

        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.ACTIVE_GUIDANCE)

        verify(exactly = 1) { accounts.navigationStarted() }
    }

    @Test
    fun previousIdleCurrentActiveGuidanceStoppedIsNotCalled() {
        val navigationAccountsSession = NavigationAccountsSession(context)

        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.ACTIVE_GUIDANCE)

        verify(exactly = 0) { accounts.navigationStopped() }
    }

    @Test
    fun previousActiveGuidanceCurrentIdleStoppedIsCalled() {
        val navigationAccountsSession = NavigationAccountsSession(context)
        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.ACTIVE_GUIDANCE)

        navigationAccountsSession.onNavigationSessionStateChanged(NavigationSession.State.IDLE)

        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun previousActiveGuidanceCurrentFreeDriveStoppedIsCalled() {
        val navigationAccountsSession = NavigationAccountsSession(context)
        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.ACTIVE_GUIDANCE)

        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.FREE_DRIVE)

        verify(exactly = 1) { accounts.navigationStopped() }
    }

    @Test
    fun previousActiveGuidanceCurrentActiveGuidanceStartedOnce() {
        val navigationAccountsSession = NavigationAccountsSession(context)
        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.ACTIVE_GUIDANCE)

        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.ACTIVE_GUIDANCE)

        verify(exactly = 1) { accounts.navigationStarted() }
        verify(exactly = 0) { accounts.navigationStopped() }
    }

    @Test
    fun previousFreeDriveCurrentIdleNeitherStartedNorStoppedAreCalled() {
        val navigationAccountsSession = NavigationAccountsSession(context)
        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.FREE_DRIVE)

        navigationAccountsSession.onNavigationSessionStateChanged(NavigationSession.State.IDLE)

        verify(exactly = 0) { accounts.navigationStarted() }
        verify(exactly = 0) { accounts.navigationStopped() }
    }

    @Test
    fun previousFreeDriveCurrentFreeDriveNeitherStartedNorStoppedAreCalled() {
        val navigationAccountsSession = NavigationAccountsSession(context)
        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.FREE_DRIVE)

        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.FREE_DRIVE)

        verify(exactly = 0) { accounts.navigationStarted() }
        verify(exactly = 0) { accounts.navigationStopped() }
    }

    @Test
    fun previousFreeDriveCurrentActiveGuidanceNeitherStartedNorStoppedAreCalled() {
        val navigationAccountsSession = NavigationAccountsSession(context)
        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.FREE_DRIVE)

        navigationAccountsSession
            .onNavigationSessionStateChanged(NavigationSession.State.ACTIVE_GUIDANCE)

        verify(exactly = 1) { accounts.navigationStarted() }
        verify(exactly = 0) { accounts.navigationStopped() }
    }
}
