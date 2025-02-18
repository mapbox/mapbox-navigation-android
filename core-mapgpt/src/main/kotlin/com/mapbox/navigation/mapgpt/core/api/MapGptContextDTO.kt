package com.mapbox.navigation.mapgpt.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Context required for MapGPT service to process the request.
 *
 * @property userContext required app context
 * @property appContext optional user context
 * @property vehicleContext optional vehicle context
 * @property routeContext optional route context
 * @property evContext optional ev context
 */
@Serializable
data class MapGptContextDTO constructor(
    @SerialName("user_context")
    val userContext: MapGptUserContextDTO,
    @SerialName("app_context")
    val appContext: MapGptAppContextDTO? = null,
    @SerialName("vehicle_context")
    val vehicleContext: MapGptVehicleContextDTO? = null,
    @SerialName("route_context")
    val routeContext: MapGptRouteContextDTO? = null,
    @SerialName("ev_context")
    val evContext: MapGptEVContextDTO? = null,
)

/**
 * @property locale IETF language tag (based on ISO 639), for example "en-US".
 * This locale will be used to influence the language the AI replies in.
 * @property temperatureUnits "Fahrenheit" or "Celsius".
 * @property distanceUnits "mi" or "km"
 * @property clientTime The current time in 'yyyy-MM-dd'T'HH:mm:ss' format.
 * @property media Current playing media description.
 */
@Serializable
data class MapGptAppContextDTO constructor(
    @SerialName("locale")
    val locale: String? = null,
    @SerialName("temp_units")
    val temperatureUnits: String? = null,
    @SerialName("distance_units")
    val distanceUnits: String? = null,
    @SerialName("client_time")
    val clientTime: String? = null,
    @SerialName("media")
    val media: MapGptMusicContextDTO? = null,
)

/**
 * @property name The name of the currently playing track.
 * @property state State of the player, either "Playing" or "Paused".
 */
@Serializable
data class MapGptMusicContextDTO constructor(
    @SerialName("name")
    val name: String? = null,
    @SerialName("artist")
    val artist: String? = null,
    @SerialName("state")
    val state: String? = null,
)

/**
 * @property lat Latitude of the current location.
 * @property lon Longitude of the current location.
 * @property heading Current user heading.
 * @property placeName The name of the place where the user currently is. For example:
 * - Neighborhood, a colloquial sub-city features often referred to in local parlance.
 * - Place, a cities, villages, municipalities, etc.
 * - Locality, a sub-city features present in countries where such an additional administrative layer is used in postal addressing.
 * - District, smaller than top-level administrative features but typically larger than cities.
 * - Region, a top-level sub-national administrative features, such as states in the United States or provinces in Canada or China.
 * - Country, Generally recognized countries or, in some cases like Hong Kong, an area of quasi-national administrative status that has been given a designated country code under ISO 3166-1.
 *
 * The provided name should be the most granular available to be determined (for example, a Neighborhood should be preferred over Place, if available).
 */
@Serializable
data class MapGptUserContextDTO(
    val lat: String,
    val lon: String,
    val heading: String? = null,
    @SerialName("place_name")
    val placeName: String,
)

/**
 * @property fuel Fuel type, one of:
 * - Gas
 * - Petrol
 * - Diesel
 * - BioDiesel
 * - Electric
 * - Hydrogen
 * - Hybrid
 * @property batteryLevel The current battery charge, in percentages.
 * @property acTemperature The temperature set on the HVAC system in the vehicle.
 * @property acStatus The status of HVAC system, either `"on"` or `"off"`.
 */
@Serializable
data class MapGptVehicleContextDTO constructor(
    val fuel: String? = null,
    @SerialName("battery_level")
    val batteryLevel: Int? = null,
    @SerialName("ac_temperature")
    val acTemperature: Int? = null,
    @SerialName("ac_status")
    val acStatus: String? = null,
    @SerialName("autopilot_available")
    val autopilotAvailable: Boolean = false,
)

/**
 * @property active `true` when there's an active navigation session, `false` otherwise.
 * @property eta The date/time of arrival in 'yyyy-MM-dd'T'HH:mm:ss' format.
 * @property timeLeft Duration in minutes until the destination.
 * @property distanceLeft The distance remaining in the route, in kilometers or miles.
 * @property stepDistanceRemaining The distance remaining in the current step of the route, in kilometers or miles.
 * @property maxCongestion The maximum congestion on the remaining portion on the route, in a range from 0 to 100.
 * @property typicalDuration The typical travel time from this route's origin to destination.
 * @property alternativeRoutes Additional information on each alternative route when available.
 * @property speed Current speed that the user is traveling at, in meters/second over ground.
 * @property speedLimit Current speed limit, in meters/second over ground.
 * @property currentLaneIndex Index into `lanes` indicating the current lane the driver is on, where `0` is the leftmost lane and `null` is unknown.
 * @property maneuver Object detailing the type and instruction for the upcoming maneuver.
 * @property lanes Array of all available lanes.
 * @property origin The location of route's origin.
 * @property destination The location of route's destination.
 */
@Serializable
data class MapGptRouteContextDTO constructor(
    val active: Boolean? = null,
    @SerialName("route_json")
    val routeJson: String? = null,
    val eta: String? = null,
    @SerialName("time_left")
    val timeLeft: Int? = null,
    @SerialName("distance_left")
    val distanceLeft: Double? = null,
    @SerialName("step_distance_remaining")
    val stepDistanceRemaining: Double? = null,
    @SerialName("max_congestion")
    val maxCongestion: Int? = null,
    @SerialName("duration_typical")
    val typicalDuration: Int? = null,
    @SerialName("alternative_routes")
    val alternativeRoutes: List<MapGptAlternateRouteDTO> = emptyList(),
    val speed: Double? = null,
    @SerialName("speed_limit")
    val speedLimit: Double? = null,
    @SerialName("current_lane_index")
    val currentLaneIndex: Int? = null,
    val maneuver: MapGptStepManeuver? = null,
    val lanes: List<MapGptLanesDTO>? = null,
    val origin: MapGptLocationDTO? = null,
    val destination: MapGptLocationDTO? = null,
)

/**
 * Holds information about alternative route in comparison to primary route.
 * @property deltaEta Optional. Difference in eta in minutes from the primary route. Eg: 12, -10.
 * @property deltaDistance Optional. Difference in distance to destination in distance_units(km/mi)
 * from the primary route . Eg: 0.5, -1.2
 * @property deltaCost Optional. Difference in costs in local currency from the primary route. Eg: "$10"
 */
@Serializable
data class MapGptAlternateRouteDTO(
    @SerialName("delta_eta")
    val deltaEta: Int? = null,
    @SerialName("delta_distance")
    val deltaDistance: Double? = null,
    @SerialName("delta_cost")
    val deltaCost: String? = null
)

/**
 * @property valid indicates the recommended lanes for the driver. Value `true` represents recommended
 * lane and `false` otherwise.
 */
@Serializable
data class MapGptLanesDTO(
    val valid: Boolean? = null,
)

/**
 * @property instruction sentence representation of the maneuver instruction
 * @property type type of maneuver. For ex: turn, fork, on-ramp, off-ramp, depart, arrive, merge, roundabout etc
 */
@Serializable
data class MapGptStepManeuver(
    val instruction: String? = null,
    val type: String? = null
)

/**
 * @property lat Latitude of the waypoint location.
 * @property lon Longitude of the waypoint location.
 * @property placeName The name of the waypoint.
 */
@Serializable
data class MapGptLocationDTO constructor(
    val lat: String? = null,
    val lon: String? = null,
    @SerialName("place_name")
    val placeName: String? = null,
)

/**
 * @property name The name of the charging station.
 * @property distance The distance to the charging station, in kilometers or miles.
 */
@Serializable
data class MapGptEVStationDTO constructor(
    val name: String? = null,
    val distance: Double? = null,
)

/**
 * @property nearbyStations The collection of nearby charging stations.
 */
@Serializable
data class MapGptEVContextDTO constructor(
    @SerialName("nearby_stations")
    val nearbyStations: List<MapGptEVStationDTO>? = null,
)
