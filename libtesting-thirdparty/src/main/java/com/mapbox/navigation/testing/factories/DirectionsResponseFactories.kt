package com.mapbox.navigation.testing.factories

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.MaxSpeed
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.geojson.Point

fun createDirectionsResponse(
    uuid: String? = "testUUID",
    routes: List<DirectionsRoute> = listOf(createDirectionsRoute()),
    unrecognizedProperties: Map<String, JsonElement>? = null,
    responseWaypoints: List<DirectionsWaypoint> = listOf(createWaypoint(), createWaypoint()),
    routeOptions: RouteOptions? = createRouteOptions()
): DirectionsResponse {
    val processedRoutes = routes.map {
        it.toBuilder().requestUuid(uuid).routeOptions(routeOptions).build()
    }
    return DirectionsResponse.builder()
        .uuid(uuid)
        .code("Ok")
        .routes(processedRoutes)
        .unrecognizedJsonProperties(unrecognizedProperties)
        .waypoints(responseWaypoints)
        .build()
}

fun createDirectionsRoute(
    legs: List<RouteLeg>? = listOf(createRouteLeg()),
    routeOptions: RouteOptions = createRouteOptions(),
    distance: Double = 5.0,
    duration: Double = 9.0,
    routeIndex: String = "0",
    requestUuid: String? = "testUUID",
    waypoints: List<DirectionsWaypoint>? = null,
    refreshTtl: Int? = null
): DirectionsRoute = DirectionsRoute.builder()
    .distance(distance)
    .duration(duration)
    .legs(legs)
    .routeOptions(routeOptions)
    .routeIndex(routeIndex)
    .requestUuid(requestUuid)
    .waypoints(waypoints)
    .apply { refreshTtl?.let { unrecognizedJsonProperties(mapOf("refresh_ttl" to JsonPrimitive(it))) } }
    .build()

fun createRouteLeg(
    annotation: LegAnnotation? = createRouteLegAnnotation(),
    incidents: List<Incident>? = null,
    closures: List<Closure>? = null,
    steps: List<LegStep> = listOf(createRouteStep()),
    duration: Double? = null
): RouteLeg {
    return RouteLeg.builder()
        .annotation(annotation)
        .incidents(incidents)
        .closures(closures)
        .duration(duration)
        .steps(steps)
        .build()
}

fun createRouteStep(
    distance: Double = 123.0,
    duration: Double = 333.4,
    mode: String = "driving",
    maneuver: StepManeuver = createManeuver(),
    weight: Double = 111.0,
    bannerInstructions: List<BannerInstructions> = listOf(createBannerInstructions())
): LegStep {
    return LegStep
        .builder()
        .distance(distance)
        .duration(duration)
        .mode(mode)
        .maneuver(maneuver)
        .weight(weight)
        .bannerInstructions(bannerInstructions)
        .build()
}

fun createManeuver(
    rawLocation: DoubleArray = doubleArrayOf(123.3434, 37.2233)
): StepManeuver = StepManeuver.builder().rawLocation(rawLocation).build()

fun createBannerInstructions(
    primary: BannerText = createBannerText(),
    view: BannerView = createBannerView(),
    secondary: BannerText = createBannerText(),
    sub: BannerText = createBannerText(),
    distanceAlongGeometry: Double = 555.0
): BannerInstructions {
    return BannerInstructions
        .builder()
        .primary(primary)
        .view(view)
        .secondary(secondary)
        .sub(sub)
        .distanceAlongGeometry(distanceAlongGeometry)
        .build()
}

fun createBannerText(
    text: String = "testText",
    @StepManeuver.StepManeuverType type: String = StepManeuver.TURN,
    modifier: String = "right",
    degrees: Double = 90.0,
    drivingSide: String = "right"
): BannerText {
    return BannerText.builder()
        .text(text)
        .type(type)
        .modifier(modifier)
        .degrees(degrees)
        .drivingSide(drivingSide)
        .build()
}

fun createBannerView(
    text: String = "testText",
    @StepManeuver.StepManeuverType type: String = StepManeuver.TURN,
    modifier: String = "right",
    components: List<BannerComponents> = emptyList(),
): BannerView {
    return BannerView
        .builder()
        .components(components)
        .text(text)
        .type(type)
        .modifier(modifier)
        .build()

}

fun createRouteLegAnnotation(
    congestion: List<String> = listOf("severe", "moderate"),
    congestionNumeric: List<Int> = listOf(90, 50),
    distance: List<Double> = listOf(10.0, 10.0),
    duration: List<Double> = listOf(2.0, 2.0),
    maxSpeed: List<MaxSpeed> = listOf(createMaxSpeed(40), createMaxSpeed(60)),
    speed: List<Double> = listOf(40.4, 60.7),
    stateOfCharge: List<Int> = listOf(80, 79)
): LegAnnotation {
    return LegAnnotation.builder()
        .distance(distance)
        .duration(duration)
        .congestion(congestion)
        .congestionNumeric(congestionNumeric)
        .maxspeed(maxSpeed)
        .speed(speed)
        .unrecognizedJsonProperties(
            mapOf(
                "state_of_charge" to JsonArray().apply {
                    stateOfCharge.forEach {
                        add(JsonPrimitive(it))
                    }
                }
            )
        )
        .build()
}

fun createMaxSpeed(
    speed: Int = 60,
    unit: String = "km/h"
): MaxSpeed = MaxSpeed.builder().speed(speed).unit(unit).build()

fun createIncident(
    id: String = "1",
    @Incident.IncidentType type: String = Incident.INCIDENT_CONSTRUCTION,
    endTime: String? = null,
    startGeometryIndex: Int? = null,
    endGeometryIndex: Int? = null,
): Incident = Incident.builder()
    .id(id)
    .type(type)
    .endTime(endTime)
    .geometryIndexStart(startGeometryIndex)
    .geometryIndexEnd(endGeometryIndex)
    .build()

fun createClosure(
    geometryIndexStart: Int = 2,
    geometryIndexEnd: Int = 11,
): Closure = Closure.builder()
    .geometryIndexStart(geometryIndexStart)
    .geometryIndexEnd(geometryIndexEnd)
    .build()

fun createJsonWaypoint(
    name: String = "name",
    distance: Double? = null,
    location: DoubleArray = doubleArrayOf(1.3, 5.7),
    metadata: JsonObject? = null
): JsonElement = JsonObject().apply {
    add("name", JsonPrimitive(name))
    distance?.let { add("distance", JsonPrimitive(it)) }
    add("location", JsonArray().apply {
        location.forEach { coord -> add(JsonPrimitive(coord)) }
    })
    metadata?.let { add("metadata", it) }
}

fun createWaypoint(
    name: String = "name",
    distance: Double? = null,
    location: DoubleArray = doubleArrayOf(1.3, 5.7),
    unrecognizedProperties: Map<String, JsonElement>? = null
): DirectionsWaypoint = DirectionsWaypoint.builder()
    .name(name)
    .distance(distance)
    .rawLocation(location)
    .unrecognizedJsonProperties(unrecognizedProperties)
    .build()

fun createRouteOptions(
    // the majority of tests needs 2 waypoints
    coordinatesList: List<Point> = createCoordinatesList(2),
    profile: String = DirectionsCriteria.PROFILE_DRIVING,
    unrecognizedProperties: Map<String, JsonElement>? = null,
    enableRefresh: Boolean? = false,
    waypointsPerRoute: Boolean? = null,
    bearingList: List<Bearing?>? = null,
    avoidManeuverRadius: Double? = null,
    @DirectionsCriteria.GeometriesCriteria geometries: String = DirectionsCriteria.GEOMETRY_POLYLINE6
): RouteOptions {
    return RouteOptions
        .builder()
        .coordinatesList(coordinatesList)
        .profile(profile)
        .enableRefresh(enableRefresh)
        .waypointsPerRoute(waypointsPerRoute)
        .unrecognizedJsonProperties(unrecognizedProperties)
        .bearingsList(bearingList)
        .avoidManeuverRadius(avoidManeuverRadius)
        .geometries(geometries)
        .build()
}

fun createCoordinatesList(waypointCount: Int): List<Point> = MutableList(waypointCount) { index ->
    Point.fromLngLat(index.toDouble(), index.toDouble())
}
