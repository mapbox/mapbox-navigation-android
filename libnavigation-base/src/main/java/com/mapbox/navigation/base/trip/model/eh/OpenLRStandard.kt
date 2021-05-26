package com.mapbox.navigation.base.trip.model.eh

import androidx.annotation.StringDef

/**
 * OpenLRStandard
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
