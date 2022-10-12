package com.mapbox.navigation.core

import com.mapbox.navigation.core.trip.session.NavigationSessionState

internal fun interface CopilotSessionObserver {

    fun onCopilotSessionChanged(session: NavigationSessionState)
}
