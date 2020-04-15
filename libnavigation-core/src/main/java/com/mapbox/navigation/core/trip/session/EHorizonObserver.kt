package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.base.trip.model.ElectronicHorizon

/**
 * An interface which enables listening to Electronic Horizon updates
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 *
 * For now, Electronic Horizon only works in Free Drive.
 */
interface EHorizonObserver {
    /**
     * Invoked every time the [ElectronicHorizon] is updated
     * @param eHorizon [ElectronicHorizon]
     */
    fun onElectronicHorizonUpdated(eHorizon: ElectronicHorizon)
}
