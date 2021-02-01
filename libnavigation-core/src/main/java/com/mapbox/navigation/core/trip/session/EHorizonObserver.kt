package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.trip.model.eh.EHorizon
import com.mapbox.navigation.core.trip.model.eh.EHorizonPosition

/**
 * The EHorizonObserver consists of two parts: onElectronicHorizonUpdated provides the
 * Electronic Horizon and onPositionUpdated the position thereon.
 */
@Deprecated("Temporarily no-op. Functionality will be reintroduced in future releases.")
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
