package com.mapbox.navigation.base.trip.model

/**
 * EHorizon type
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 *
 * @see EHorizonObserver
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
