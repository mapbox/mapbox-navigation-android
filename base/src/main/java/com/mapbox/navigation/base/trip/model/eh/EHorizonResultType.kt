package com.mapbox.navigation.base.trip.model.eh

import androidx.annotation.StringDef

/**
 * EHorizonResultType
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
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

    /**
     * EH is not available due to being in off-road, fallback or uncertain state
     */
    const val NOT_AVAILABLE = "NOT_AVAILABLE"

    /**
     * Retention policy for the EHorizonResultType
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(INITIAL, UPDATE, NOT_AVAILABLE)
    annotation class Type
}
