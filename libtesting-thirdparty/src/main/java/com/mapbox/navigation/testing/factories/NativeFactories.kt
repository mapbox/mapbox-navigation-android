package com.mapbox.navigation.testing.factories

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.DataRef
import com.mapbox.geojson.Point
import com.mapbox.navigator.ActiveGuidanceInfo
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.BannerSection
import com.mapbox.navigator.CorrectedLocationData
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.HdMatchingResult
import com.mapbox.navigator.MapMatcherOutput
import com.mapbox.navigator.MapboxAPI
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.OffRoadStateProvider
import com.mapbox.navigator.RerouteError
import com.mapbox.navigator.RerouteErrorType
import com.mapbox.navigator.RoadName
import com.mapbox.navigator.RouteIndices
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterOrigin
import com.mapbox.navigator.SpeedLimit
import com.mapbox.navigator.SpeedLimitSign
import com.mapbox.navigator.SpeedLimitUnit
import com.mapbox.navigator.TimeZone
import com.mapbox.navigator.TurnLane
import com.mapbox.navigator.UpcomingRouteAlertUpdate
import com.mapbox.navigator.VoiceInstruction
import com.mapbox.navigator.Waypoint
import com.mapbox.navigator.WaypointType
import java.nio.ByteBuffer
import java.time.Instant
import java.util.Date

fun createNavigationStatus(
    routeState: RouteState = RouteState.TRACKING,
    locatedAlternativeId: String? = null,
    primaryRouteId: String? = null,
    stale: Boolean = false,
    location: FixLocation = createFixedLocation(),
    routeIndex: Int = 0,
    legIndex: Int = 0,
    stepIndex: Int = 0,
    isFallback: Boolean = false,
    isTunnel: Boolean = false,
    isParkingAisle: Boolean = false,
    isRoundabout: Boolean = false,
    predicted: Long = 0,
    geometryIndex: Int = 0,
    shapeIndex: Int = 0,
    intersectionIndex: Int = 0,
    turnLanes: List<TurnLane> = emptyList(),
    roads: List<RoadName> = emptyList(),
    voiceInstruction: VoiceInstruction? = null,
    // default banner instruction workarounds the direct usage of the MapboxNativeNavigatorImpl
    bannerInstruction: BannerInstruction? = createBannerInstruction(),
    speedLimit: SpeedLimit = SpeedLimit(null, SpeedLimitUnit.KILOMETRES_PER_HOUR, SpeedLimitSign.VIENNA),
    keyPoints: List<FixLocation> = emptyList(),
    mapMatcherOutput: MapMatcherOutput = createMapMatcherOutput(),
    offRoadProba: Float = 0f,
    offRoadStateProvider: OffRoadStateProvider = OffRoadStateProvider.UNKNOWN,
    activeGuidanceInfo: ActiveGuidanceInfo? = null,
    upcomingRouteAlertUpdates: List<UpcomingRouteAlertUpdate> = emptyList(),
    nextWaypointIndex: Int = 0,
    layer: Int = 0,
    alternativeRouteIndices: List<RouteIndices> = emptyList(),
    isSyntheticLocation: Boolean = false,
    correctedLocationData: CorrectedLocationData? = null,
    hdMatchingResult: HdMatchingResult? = null,
    mapMatchedSystemTime: Date = Date(),
    isAdasDataAvailable: Boolean? = null,
): NavigationStatus {
    return NavigationStatus(
        routeState,
        locatedAlternativeId,
        primaryRouteId,
        stale,
        location,
        routeIndex,
        legIndex,
        stepIndex,
        isFallback,
        isTunnel,
        isParkingAisle,
        isRoundabout,
        predicted,
        geometryIndex,
        shapeIndex,
        intersectionIndex,
        turnLanes,
        alternativeRouteIndices,
        roads,
        voiceInstruction,
        bannerInstruction,
        speedLimit,
        keyPoints,
        mapMatcherOutput,
        offRoadProba,
        offRoadStateProvider,
        activeGuidanceInfo,
        upcomingRouteAlertUpdates,
        nextWaypointIndex,
        layer,
        isSyntheticLocation,
        correctedLocationData,
        hdMatchingResult,
        mapMatchedSystemTime,
        isAdasDataAvailable,
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
        index,
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

fun createNativeWaypoint(
    name: String = "testWaypoint",
    location: Point = Point.fromLngLat(2.1, 2.2),
    distance: Double? = null,
    metadata: String? = null,
    target: Point? = null,
    type: WaypointType = WaypointType.REGULAR,
    timeZone: TimeZone? = null,
) = Waypoint(
    name,
    location,
    distance,
    metadata,
    target,
    type,
    timeZone,
)

// Add default parameters if you define properties
fun createMapMatcherOutput() = MapMatcherOutput(emptyList(), false, 0)

fun createRouteInterface(
    responseUUID: String = "testResponseUUID",
    routeIndex: Int = 0,
    responseJson: String = createDirectionsResponse().toJson(),
    requestURI: String = createRouteOptions().toUrl("***").toString(),
    routerOrigin: RouterOrigin = RouterOrigin.ONLINE,
    routeInfo: RouteInfo = RouteInfo(emptyList()),
    waypoints: List<Waypoint> = emptyList(),
    expirationTimeMs: Long? = null,
    lastRefreshTimestamp: Date? = null,
    routeGeometry: List<Point> = emptyList(),
): RouteInterface = object : RouteInterface {

    override fun getRouteId() = "$responseUuid#$routeIndex"

    override fun getResponseUuid() = responseUUID

    override fun getRouteIndex() = routeIndex

    override fun getResponseJsonRef(): DataRef {
        return responseJson.toDataRef()
    }

    override fun getRequestUri() = requestURI

    override fun getRouterOrigin() = routerOrigin

    override fun getMapboxAPI() = MapboxAPI.DIRECTIONS

    override fun getRouteInfo() = routeInfo

    override fun getWaypoints() = waypoints

    override fun getExpirationTimeMs(): Long? = expirationTimeMs

    override fun getLastRefreshTimestamp(): Date? = lastRefreshTimestamp

    override fun getRouteGeometry() = routeGeometry
}

fun String.toDataRef(): DataRef {
    val bytes = encodeToByteArray()
    val buffer = ByteBuffer.allocateDirect(bytes.size)
    buffer.put(bytes)
    return DataRef(buffer)
}

fun createRouterError(
    message: String = "test error",
    type: RouterErrorType = RouterErrorType.UNKNOWN,
    requestId: Long = 0L,
    refreshTtl: Int? = null,
    routerOrigin: RouterOrigin = RouterOrigin.ONLINE,
    stringUrl: String = createRouteOptions().toUrl("***").toString(),
    isRetryable: Boolean = false
) = RouterError(
    message,
    type,
    requestId,
    refreshTtl,
    routerOrigin,
    stringUrl,
    isRetryable,
)

fun createRerouteError(
    message: String = "test error",
    errorType: RerouteErrorType = RerouteErrorType.ROUTER_ERROR,
    routerErrors: List<RouterError> = listOf(createRouterError())
) = RerouteError(
    message,
    errorType,
    routerErrors,
)
