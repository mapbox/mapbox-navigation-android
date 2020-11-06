package com.mapbox.navigation.core.trip.model.eh

/**
 * Holds available [FunctionalRoadClass] types. See https://wiki.openstreetmap.org/wiki/Key:highway for further details.
 *
 * Available values are:
 * - [FunctionalRoadClass.MOTORWAY]
 * - [FunctionalRoadClass.TRUNK]
 * - [FunctionalRoadClass.PRIMARY]
 * - [FunctionalRoadClass.SECONDARY]
 * - [FunctionalRoadClass.TERTIARY]
 * - [FunctionalRoadClass.UNCLASSIFIED]
 * - [FunctionalRoadClass.RESIDENTIAL]
 * - [FunctionalRoadClass.SERVICE_OTHER]
 */
object FunctionalRoadClass {

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
}
