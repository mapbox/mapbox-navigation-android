package com.mapbox.navigation.ui.app.internal.location

import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.ui.app.internal.Action

/**
 * Defines actions responsible for updating the [Location].
 */
sealed class LocationAction : Action {
    /**
     * The action updates the [LocationMatcherResult] retrieved from location observer
     * @property result
     */
    data class Update(val result: LocationMatcherResult) : LocationAction()
}
