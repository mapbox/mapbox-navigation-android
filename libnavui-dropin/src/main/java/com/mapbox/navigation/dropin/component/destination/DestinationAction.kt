package com.mapbox.navigation.dropin.component.destination

import com.mapbox.navigation.dropin.model.Destination

sealed class DestinationAction {
    data class SetDestination(val destination: Destination?) : DestinationAction()
}
