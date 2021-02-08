package com.mapbox.navigation.core.trip.model.eh

/**
 * EHorizonResultType
 */
object EHorizonResultType {

    /**
     * State will be INITIAL for the first EHorizon and after a Horizon reset.
     * This represents a new MPP.
     *
     * These are possible scenarios:
     * - The very first Electronic Horizon generation
     * - Localization error which leads to a completely separate MPP from the previous
     * - Deviate from the previous MPP, i.e. driving to the side path of the previous MPP
     */
    const val INITIAL = "INITIAL"

    /**
     * State will be UPDATE for continuation of the EHorizon.
     * The EHorizon is an update of the previous state.
     */
    const val UPDATE = "UPDATE"
}
