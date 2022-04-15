package com.mapbox.navigation.testing.factories

import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.geojson.Point
import com.mapbox.navigator.ActiveGuidanceInfo
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.BannerSection
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.MapMatcherOutput
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Road
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.RouterOrigin
import com.mapbox.navigator.SpeedLimit
import com.mapbox.navigator.UpcomingRouteAlert
import com.mapbox.navigator.VoiceInstruction
import com.mapbox.navigator.Waypoint
import java.time.Instant
import java.util.Date

fun createNavigationStatus(
    routeState: RouteState = RouteState.TRACKING,
    locatedAlternativeId: String? = null,
    stale: Boolean = false,
    location: FixLocation = createFixedLocation(),
    routeSequenceNumber: Int = 0,
    routeIndex: Int = 0,
    legIndex: Int = 0,
    stepIndex: Int = 0,
    isFallback: Boolean = false,
    isTunnel: Boolean = false,
    predicted: Long = 0,
    shapeIndex: Int = 0,
    intersectionIndex: Int = 0,
    roads: List<Road> = emptyList(),
    voiceInstruction: VoiceInstruction? = null,
    // default banner instruction workarounds the direct usage of the MapboxNativeNavigatorImpl
    bannerInstruction: BannerInstruction? = createBannerInstruction(),
    speedLimit: SpeedLimit? = null,
    keyPoints: List<FixLocation> = emptyList(),
    mapMatcherOutput: MapMatcherOutput = createMapMatcherOutput(),
    offRoadProba: Float = 0f,
    activeGuidanceInfo: ActiveGuidanceInfo? = null,
    upcomingRouteAlerts: List<UpcomingRouteAlert> = emptyList(),
    nextWaypointIndex: Int = 0,
    layer: Int = 0
): NavigationStatus {
    return NavigationStatus(
        routeState,
        locatedAlternativeId,
        stale,
        location,
        routeIndex,
        legIndex,
        stepIndex,
        isFallback,
        isTunnel,
        predicted,
        shapeIndex,
        intersectionIndex,
        roads,
        voiceInstruction,
        bannerInstruction,
        speedLimit,
        keyPoints,
        mapMatcherOutput,
        offRoadProba,
        activeGuidanceInfo,
        upcomingRouteAlerts,
        nextWaypointIndex,
        layer
    )
}

fun createVoiceInstruction(
    announcement: String = "test"
): VoiceInstruction {
    return VoiceInstruction("test", announcement, 0f, 0)
}

fun createBannerInstruction(
    primary: BannerSection = createBannerSection(),
    view: BannerSection = createBannerSection(),
    secondary: BannerSection = createBannerSection(),
    sub: BannerSection = createBannerSection(),
    remainingStepDistance: Float = 50f,
    index: Int = 0
): BannerInstruction {
    return BannerInstruction(
        primary,
        view,
        secondary,
        sub,
        remainingStepDistance,
        index
    )
}

fun createBannerSection(
    text: String = "testText",
    @StepManeuver.StepManeuverType type: String = StepManeuver.TURN,
    modifier: String = "right",
    degrees: Int = 90,
    drivingSide: String = "right"
): BannerSection {
    return BannerSection(
        text,
        type,
        modifier,
        degrees,
        drivingSide,
        null
    )
}

// Add more default parameters if you define properties
fun createFixedLocation(
    longitude: Double = 0.0,
    latitude: Double = 0.0,
) = FixLocation(
    Point.fromLngLat(longitude, latitude),
    0,
    Date.from(Instant.ofEpochMilli(20)),
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    HashMap(),
    true
)

// Add default parameters if you define properties
fun createMapMatcherOutput() = MapMatcherOutput(emptyList(), false)

fun createRouteInterface(
    responseUUID: String = "testResponseUUID",
    routeIndex: Int = 0,
    responseJson: String = "",
    requestURI: String = "",
    routerOrigin: RouterOrigin = RouterOrigin.ONLINE,
    routeInfo: RouteInfo = RouteInfo(emptyList()),
    waypoints: List<Waypoint> = emptyList(),
    waypointsJson: String = "",
): RouteInterface = object : RouteInterface {

    override fun getRouteId() = "$responseUuid#$routeIndex"

    override fun getResponseUuid() = responseUUID

    override fun getRouteIndex() = routeIndex

    override fun getResponseJson() = responseJson

    override fun getRequestUri() = requestURI

    override fun getRouterOrigin() = routerOrigin

    override fun getRouteInfo() = routeInfo

    override fun getWaypoints() = waypoints

    override fun getWaypointsJson() = waypointsJson
}
