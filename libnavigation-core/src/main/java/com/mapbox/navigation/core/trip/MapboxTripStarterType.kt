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
     * The [MapboxTripStarter] will enable replay for the navigation routes.
     */
    object ReplayRoute : MapboxTripStarterType()
}
