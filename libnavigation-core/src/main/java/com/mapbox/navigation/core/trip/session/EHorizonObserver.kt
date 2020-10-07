package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.base.trip.model.EHorizon
import com.mapbox.navigation.base.trip.model.EHorizonPosition

/**
 * The EHorizonObserver consists of two parts: onElectronicHorizonUpdated provides the
 * Electronic Horizon and onPositionUpdated the position thereon.
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 */
interface EHorizonObserver {

    /**
     * This callback is fired whenever the Electronic Horizon itself changes.
     *
     * @see EHorizon
     * @see EHorizonResultType
     */
    fun onElectronicHorizonUpdated(horizon: EHorizon, type: String)

    /**
     * This callback is fired whenever the location on the EHorizon changes.
     * This will always relate to the EHorizon previously conveyed through
     * onElectronicHorizonUpdated. The location contains the edge and the progress along that edge
     * in the EHorizon.
     *
     * @see EHorizonPosition
     */
    fun onPositionUpdated(position: EHorizonPosition)
}
