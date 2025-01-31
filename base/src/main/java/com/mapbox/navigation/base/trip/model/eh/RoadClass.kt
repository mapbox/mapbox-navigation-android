package com.mapbox.navigation.base.trip.model.eh

import androidx.annotation.StringDef

/**
 * Holds available [RoadClass] types. See https://wiki.openstreetmap.org/wiki/Key:highway for further details.
 *
 * Available values are:
 * - [RoadClass.MOTORWAY]
 * - [RoadClass.TRUNK]
 * - [RoadClass.PRIMARY]
 * - [RoadClass.SECONDARY]
 * - [RoadClass.TERTIARY]
 * - [RoadClass.UNCLASSIFIED]
 * - [RoadClass.RESIDENTIAL]
 * - [RoadClass.SERVICE_OTHER]
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
 */
object RoadClass {

    /**
     * Describes a motorway FRC. See https://wiki.openstreetmap.org/wiki/Tag:highway%3Dmotorway for further details.
     */
    const val MOTORWAY = "MOTORWAY"

    /**
     * Describes a trunk FRC. See https://wiki.openstreetmap.org/wiki/Tag:highway%3Dtrunk for further details.
     */
    const val TRUNK = "TRUNK"

    /**
     * Describes a primary FRC. See https://wiki.openstreetmap.org/wiki/Tag:highway%3Dprimary for further details.
     */
    const val PRIMARY = "PRIMARY"

    /**
     * Describes a secondary FRC. See https://wiki.openstreetmap.org/wiki/Tag:highway%3Dsecondary for further details.
     */
    const val SECONDARY = "SECONDARY"

    /**
     * Describes a tertiary FRC. See https://wiki.openstreetmap.org/wiki/Tag:highway%3Dtertiary for further details.
     */
    const val TERTIARY = "TERTIARY"

    /**
     * Describes an unclassified FRC. See https://wiki.openstreetmap.org/wiki/Tag:highway%3Dunclassified for further details.
     */
    const val UNCLASSIFIED = "UNCLASSIFIED"

    /**
     * Describes a residential FRC. See https://wiki.openstreetmap.org/wiki/Tag:highway%3Dresidential for further details.
     */
    const val RESIDENTIAL = "RESIDENTIAL"

    /**
     * Describes a service other FRC.
     */
    const val SERVICE_OTHER = "SERVICE_OTHER"

    /**
     * Road class type.
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        MOTORWAY,
        TRUNK,
        PRIMARY,
        SECONDARY,
        TERTIARY,
        UNCLASSIFIED,
        RESIDENTIAL,
        SERVICE_OTHER,
    )
    annotation class Type
}
