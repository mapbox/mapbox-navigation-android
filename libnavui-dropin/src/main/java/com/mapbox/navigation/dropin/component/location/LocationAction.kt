package com.mapbox.navigation.dropin.component.location

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.model.Action

/**
 * Defines actions responsible for updating the [Location].
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class LocationAction : Action {
    /**
     * The action updates the [LocationMatcherResult] retrieved from location observer
     * @property result
     */
    data class Update(val result: LocationMatcherResult) : LocationAction()
}
