package com.mapbox.navigation.core.trip

/**
 * Specifies a trip type for the [MapboxTripStarter].
 */
internal sealed class MapboxTripStarterType {

    /**
     * The [MapboxTripStarter] will use the best device location for a trip session.
     */
    object MapMatching : MapboxTripStarterType()

    /**
     * The [MapboxTripStarter] will replay navigation routes with an artificial driver.
     */
    object ReplayRoute : MapboxTripStarterType()

    /**
     * The [MapboxTripStarter] will use history files to replay navigation experiences.
     */
    object ReplayHistory : MapboxTripStarterType()
}
