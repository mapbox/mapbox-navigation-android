package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver

internal class NavigationSession(private val context: Context) : RoutesObserver,
    TripSessionStateObserver {

    private var state = State.IDLE
        set(value) {
            if (field == value) {
                return
            }
            val previousValue = state
            field = value

            // todo expose state observers for the rest of the lib to hook into?

            when {
                previousValue == State.ACTIVE_GUIDANCE -> MapboxNavigationAccounts.getInstance(
                    context.applicationContext
                ).navigationStopped()
                value == State.ACTIVE_GUIDANCE -> MapboxNavigationAccounts.getInstance(
                    context.applicationContext
                ).navigationStarted()
            }
        }

    private var hasRoutes = false
        set(value) {
            if (field != value) {
                field = value
                updateState()
            }
        }

    private var isDriving = false
        set(value) {
            if (field != value) {
                field = value
                updateState()
            }
        }

    private fun updateState() {
        state = when {
            hasRoutes && isDriving -> State.ACTIVE_GUIDANCE
            isDriving -> State.FREE_DRIVE
            else -> State.IDLE
        }
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {
        hasRoutes = routes.isNotEmpty()
    }

    override fun onSessionStarted() {
        isDriving = true
    }

    override fun onSessionStopped() {
        isDriving = false
    }

    private enum class State {
        IDLE,
        FREE_DRIVE,
        ACTIVE_GUIDANCE
    }
}
