package com.mapbox.navigation.base.formatter

/**
 * Unit type for voice and visual information. Note that this won't change other results such as
 * raw distance measurements which will always be returned in meters.
 *
 * @param value String representation of the unit type
 */
enum class UnitType(val value: String) {

    /**
     * Imperial unit type.
     */
    IMPERIAL("imperial"),

    /**
     * Metric unit type.
     */
    METRIC("metric"),
}
