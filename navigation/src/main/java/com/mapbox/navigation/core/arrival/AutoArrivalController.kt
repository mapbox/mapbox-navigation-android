package com.mapbox.navigation.core.arrival

import com.mapbox.navigation.base.trip.model.RouteLegProgress

/**
 * The default controller for arrival. This will move onto the next leg automatically
 * if there is one.
 */
open class AutoArrivalController : ArrivalController {

    /**
     * Moves onto the next immediately.
     */
    override fun navigateNextRouteLeg(routeLegProgress: RouteLegProgress): Boolean {
        return true
    }
}
