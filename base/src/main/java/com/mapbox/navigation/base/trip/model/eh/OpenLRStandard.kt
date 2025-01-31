package com.mapbox.navigation.base.trip.model.eh

import androidx.annotation.StringDef

/**
 * OpenLRStandard
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
 */
object OpenLRStandard {
    /**
     * TomTom OpenLR (http://www.openlr.org/)
     * Supported references: line location, point along line, polygon.
     */
    const val TOM_TOM = "TOM_TOM"

    /**
     * TPEG OpenLR.
     * Only line locations are supported.
     */
    const val TPEG = "TPEG"

    /**
     * Retention policy for the OpenLRStandard
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(TOM_TOM, TPEG)
    annotation class Type
}
