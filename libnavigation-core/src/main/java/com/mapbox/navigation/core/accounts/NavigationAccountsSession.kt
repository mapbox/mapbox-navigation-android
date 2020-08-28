package com.mapbox.navigation.core.accounts

import android.content.Context
import com.mapbox.navigation.core.NavigationSession
import com.mapbox.navigation.core.NavigationSessionStateObserver
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts

internal class NavigationAccountsSession(private val context: Context) :
    NavigationSessionStateObserver {

    private var state = NavigationSession.State.IDLE
        set(value) {
            if (field == value) {
                return
            }
            val previousValue = state
            field = value

            when {
                previousValue == NavigationSession.State.ACTIVE_GUIDANCE ->
                    MapboxNavigationAccounts.getInstance(
                        context.applicationContext
                    ).navigationStopped()
                value == NavigationSession.State.ACTIVE_GUIDANCE ->
                    MapboxNavigationAccounts.getInstance(
                        context.applicationContext
                    ).navigationStarted()
            }
        }

    override fun onNavigationSessionStateChanged(navigationSession: NavigationSession.State) {
        state = navigationSession
    }
}
