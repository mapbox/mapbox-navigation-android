package com.mapbox.navigation.core.mapmatching

import androidx.annotation.StringDef
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.LegAnnotation

/**
 * Defines constant related to Map Matching requests.
 */
object MapMatchingExtras {
    /**
     * Use to request [LegAnnotation.distance] annotation.
     */
    const val ANNOTATION_DISTANCE = DirectionsCriteria.ANNOTATION_DISTANCE

    /**
     * Use to request [LegAnnotation.duration] annotation.
     */
    const val ANNOTATION_DURATION = DirectionsCriteria.ANNOTATION_DURATION

    /**
     * Use to request [LegAnnotation.speed] annotation.
     */
    const val ANNOTATION_SPEED = DirectionsCriteria.ANNOTATION_SPEED

    /**
     * Use to request [LegAnnotation.congestion] annotation.
     */
    const val ANNOTATION_CONGESTION = DirectionsCriteria.ANNOTATION_CONGESTION

    /**
     * Use to request [LegAnnotation.congestionNumeric] annotation.
     */
    const val ANNOTATION_CONGESTION_NUMERIC = DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC

    /**
     * Ignore access restrictions related to mode of travel.
     */
    const val IGNORE_ACCESS = "access"

    /**
     * Ignore one-way restrictions.
     */
    const val IGNORE_ONEWAYS = "oneways"

    /**
     * Ignore other restrictions, such as time-based or turn restrictions.
     */
    const val IGNORE_RESTRICTIONS = "restrictions"

    /**
     * Based on TomTom OpenLR location references.
     */
    const val OPENLR_SPEC_TOMTOM = "tomtom"

    /**
     *	Based on HERE/TPEG2 specification.
     */
    const val OPENLR_SPEC_HERE = "here"

    /**
     * 	Based on TomTom OpenLR location references.
     */
    const val OPENLR_FORMAT_TOMTOM = "tomtom"
}

/**
 * Annotations to request.
 * @see [MapMatchingOptions.Builder.annotations]
 */
@Retention(AnnotationRetention.BINARY)
@StringDef(
    MapMatchingExtras.ANNOTATION_DISTANCE,
    MapMatchingExtras.ANNOTATION_DURATION,
    MapMatchingExtras.ANNOTATION_SPEED,
    MapMatchingExtras.ANNOTATION_CONGESTION,
    MapMatchingExtras.ANNOTATION_CONGESTION_NUMERIC,
)
annotation class MapMatchingAnnotations

/**
 * Routing restrictions that could be ignored.
 * @see [MapMatchingOptions.Builder.ignore]
 */
@Retention(AnnotationRetention.BINARY)
@StringDef(
    MapMatchingExtras.IGNORE_ACCESS,
    MapMatchingExtras.IGNORE_ONEWAYS,
    MapMatchingExtras.IGNORE_RESTRICTIONS,
)
annotation class MapMatchingRoutingRestriction

/**
 * Supported OpenLR specs.
 * @see [MapMatchingOptions.Builder.openlrSpec]
 */
@Retention(AnnotationRetention.BINARY)
@StringDef(
    MapMatchingExtras.OPENLR_SPEC_HERE,
    MapMatchingExtras.OPENLR_SPEC_TOMTOM,
)
annotation class MapMatchingOpenLRSpec

/**
 * Supported OpenLR formats.
 * @see [MapMatchingOptions.Builder.openlrFormat]
 */
@Retention(AnnotationRetention.BINARY)
@StringDef(
    MapMatchingExtras.OPENLR_FORMAT_TOMTOM,
)
annotation class MapMatchingOpenLRFormat
