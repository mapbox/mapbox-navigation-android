package com.mapbox.navigation.core.routeoptions.builder

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions

@ExperimentalPreviewMapboxNavigationAPI
interface NoWaypointsOptionsBuilder {
    fun fromStartLocation(
        point: Point,
        bearing: Double? = null,
        zLevel: Int?
    ): WaypointsInProgressBuilder

    fun fromCurrentLocation(): WaypointsInProgressBuilder
}

@ExperimentalPreviewMapboxNavigationAPI
interface WaypointsInProgressBuilder {
    fun addIntermediateWaypoint(
        coordinate: Point,
        name: String? = null,
        bearing: Double? = null,
        zLevel: Int? = null,
        targetCoordinate: Point? = null
    ): WaypointsInProgressBuilder

    fun addIntermediateSilentWaypoint(
        coordinate: Point,
        bearing: Double? = null,
        zLevel: Int? = null,
    ): WaypointsInProgressBuilder

    fun toDestination(
        coordinate: Point,
        name: String? = null,
        bearing: Double? = null,
        zLevel: Int? = null,
        targetCoordinate: Point? = null
    ): RouteOptionsBuilderWithWaypoints
}

@ExperimentalPreviewMapboxNavigationAPI
interface RouteOptionsBuilderWithWaypoints {
    fun profileDriving(
        drivingSpecificSetup: DrivingSpecificSetup.() -> Unit = { }
    ): RouteOptionsBuilderWithWaypoints

    fun profileDrivingTraffic(
        drivingSpecificSetup: DrivingSpecificSetup.() -> Unit = { }
    ): RouteOptionsBuilderWithWaypoints

    fun profileWalking(
        walkingSpecificSetup: WalkingSpecificSetup.() -> Unit = { }
    ): RouteOptionsBuilderWithWaypoints

    fun profileCycling(
        cyclingSpecificSetup: CyclingSpecificSetup.() -> Unit = { }
    ): RouteOptionsBuilderWithWaypoints

    fun baseUrl(baseUrl: String): RouteOptionsBuilderWithWaypoints
}

@ExperimentalPreviewMapboxNavigationAPI
internal class NavRouteOptionsBuilder internal constructor(
    private val locationProvider: LocationProvider
) : NoWaypointsOptionsBuilder, WaypointsInProgressBuilder, RouteOptionsBuilderWithWaypoints {

    private lateinit var destination: Waypoint
    private val builder = RouteOptions.builder()
    private var locationFrom: CurrentLocation? = null
    private val intermediateWaypoints = mutableListOf<Waypoint>()
    private var profile: String? = null

    override fun fromCurrentLocation(): WaypointsInProgressBuilder {
        return this
    }

    override fun toDestination(
        coordinate: Point,
        name: String?,
        bearing: Double?,
        zLevel: Int?,
        targetCoordinate: Point?,
    ): RouteOptionsBuilderWithWaypoints {
        destination = Waypoint(
            coordinate = coordinate,
            name = name,
            bearing = bearing,
            zLevel = zLevel,
            targetCoordinate = targetCoordinate,
        )
        return this
    }

    internal fun applyLanguageAndVoiceUnitOptions(context: Context): NavRouteOptionsBuilder {
        builder.applyLanguageAndVoiceUnitOptions(context)
        return this
    }

    internal suspend fun build(): RouteOptions {
        val locationFrom = (locationFrom ?: locationProvider.getCurrentLocation()).let {
            Waypoint(
                coordinate = it.point,
                name = null,
                bearing = it.bearing,
                zLevel = it.zLevel,
                isSilent = false,
                targetCoordinate = null,
            )
        }
        val allWaypoints = listOf(locationFrom) + intermediateWaypoints + listOf(destination)
        val notSilentWaypoints = listOf(locationFrom) +
            intermediateWaypoints.filter { !it.isSilent } +
            listOf(destination)
        return builder
            .applyDefaultNavigationOptions(profile ?: DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .bearingsList(
                allWaypoints.map {
                    if (it.bearing != null) {
                        Bearing.builder()
                            .angle(it.bearing)
                            .build()
                    } else null
                }
            )
            .layersList(allWaypoints.map { it.zLevel })
            .coordinatesList(allWaypoints.map { it.coordinate })
            .waypointNamesList(notSilentWaypoints.map { it.name })
            .waypointIndicesList(
                allWaypoints.mapIndexedNotNull { index, waypoint ->
                    if (waypoint.isSilent) {
                        null
                    } else index
                }
            )
            .waypointTargetsList(notSilentWaypoints.map { it.targetCoordinate })
            .build()
    }

    override fun fromStartLocation(
        point: Point,
        bearing: Double?,
        zLevel: Int?
    ): WaypointsInProgressBuilder {
        locationFrom = CurrentLocation(
            point,
            bearing,
            zLevel
        )
        return this
    }

    override fun addIntermediateWaypoint(
        coordinate: Point,
        name: String?,
        bearing: Double?,
        zLevel: Int?,
        targetCoordinate: Point?,
    ): WaypointsInProgressBuilder {
        intermediateWaypoints.add(
            Waypoint(
                coordinate = coordinate,
                name = name,
                bearing = bearing,
                zLevel = zLevel,
                targetCoordinate = targetCoordinate
            )
        )
        return this
    }

    override fun addIntermediateSilentWaypoint(
        coordinate: Point,
        bearing: Double?,
        zLevel: Int?,
    ): WaypointsInProgressBuilder {
        intermediateWaypoints.add(
            Waypoint(
                coordinate = coordinate,
                bearing = bearing,
                zLevel = zLevel,
                name = null,
                isSilent = true,
                targetCoordinate = null
            )
        )
        return this
    }

    override fun profileDriving(
        drivingSpecificSetup: DrivingSpecificSetup.() -> Unit
    ): RouteOptionsBuilderWithWaypoints {
        profile = DirectionsCriteria.PROFILE_DRIVING
        DrivingSpecificSetup(builder).drivingSpecificSetup()
        return this
    }

    override fun profileDrivingTraffic(
        drivingSpecificSetup: DrivingSpecificSetup.() -> Unit
    ): RouteOptionsBuilderWithWaypoints {
        profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
        DrivingSpecificSetup(builder).drivingSpecificSetup()
        return this
    }

    override fun profileWalking(
        walkingSpecificSetup: WalkingSpecificSetup.() -> Unit
    ): RouteOptionsBuilderWithWaypoints {
        profile = DirectionsCriteria.PROFILE_WALKING
        WalkingSpecificSetup(builder).walkingSpecificSetup()
        return this
    }

    override fun profileCycling(
        cyclingSpecificSetup: CyclingSpecificSetup.() -> Unit
    ): RouteOptionsBuilderWithWaypoints {
        profile = DirectionsCriteria.PROFILE_CYCLING
        CyclingSpecificSetup(builder).cyclingSpecificSetup()
        return this
    }

    override fun baseUrl(baseUrl: String): RouteOptionsBuilderWithWaypoints {
        builder.baseUrl(baseUrl)
        return this
    }
}

@ExperimentalPreviewMapboxNavigationAPI
class CyclingSpecificSetup internal constructor(
    private val routeOptionsBuilder: RouteOptions.Builder
) {
    fun exclude(cyclingSpecificExcludeSetup: CyclingSpecificExclude.() -> Unit) {
        val excludes = CyclingSpecificExclude().apply(cyclingSpecificExcludeSetup).excludeList
        routeOptionsBuilder.excludeList(excludes)
    }

    class CyclingSpecificExclude {

        internal val excludeList = mutableListOf<String>()

        fun ferry() {
            excludeList.add(DirectionsCriteria.EXCLUDE_FERRY)
        }

        fun cashOnlyTolls() {
            excludeList.add(DirectionsCriteria.EXCLUDE_CASH_ONLY_TOLLS)
        }
    }
}

@ExperimentalPreviewMapboxNavigationAPI
class WalkingSpecificSetup internal constructor(
    private val routeOptionsBuilder: RouteOptions.Builder
) {

    fun exclude(walkingSpecificExcludeSetup: WalingSpecificExclude.() -> Unit) {
        val excludes = WalingSpecificExclude().apply(walkingSpecificExcludeSetup).excludeList()
        routeOptionsBuilder.excludeList(excludes)
    }

    fun walkwayBias(bias: DirectionBias) {
        routeOptionsBuilder.walkwayBias(bias.rawValue)
    }

    class WalingSpecificExclude {

        private val excludeList = mutableListOf<String>()

        fun cashOnlyTolls() {
            excludeList.add(DirectionsCriteria.EXCLUDE_CASH_ONLY_TOLLS)
        }

        fun excludeList() = excludeList
    }
}

@JvmInline
@ExperimentalPreviewMapboxNavigationAPI
value class DirectionBias(val rawValue: Double) {
    companion object {
        val low = DirectionBias(-1.0)
        val medium = DirectionBias(0.0)
        val high = DirectionBias(1.0)
    }
}

@ExperimentalPreviewMapboxNavigationAPI
class DrivingSpecificSetup internal constructor(
    private val routeOptionsBuilder: RouteOptions.Builder
) {
    fun exclude(block: DrivingSpecificExclude.() -> Unit) {
        val excludeList = DrivingSpecificExclude().apply(block).excludeList()
        routeOptionsBuilder.excludeList(excludeList)
    }

    fun include(block: DrivingSpecificInclude.() -> Unit) {
        val includeList = DrivingSpecificInclude().apply(block).includeList
        routeOptionsBuilder.includeList(includeList)
    }

    fun maxHeight(maxVehicleHeight: Double) {
        routeOptionsBuilder.maxHeight(maxVehicleHeight)
    }

    fun maxWidth(maxVehicleWidth: Double) {
        routeOptionsBuilder.maxWidth(maxVehicleWidth)
    }

    class DrivingSpecificInclude {

        internal val includeList = mutableListOf<String>()

        fun hov3() {
            includeList.add(DirectionsCriteria.INCLUDE_HOV3)
        }

        fun hov2() {
            includeList.add(DirectionsCriteria.INCLUDE_HOV2)
        }

        fun hot() {
            includeList.add(DirectionsCriteria.INCLUDE_HOT)
        }
    }

    class DrivingSpecificExclude {

        private val excludeList = mutableListOf<String>()

        fun toll() {
            excludeList.add(DirectionsCriteria.EXCLUDE_TOLL)
        }

        fun unpaved() {
            excludeList.add(DirectionsCriteria.EXCLUDE_UNPAVED)
        }

        fun ferry() {
            excludeList.add(DirectionsCriteria.EXCLUDE_FERRY)
        }

        fun motorway() {
            excludeList.add(DirectionsCriteria.EXCLUDE_MOTORWAY)
        }

        fun cashOnlyTolls() {
            excludeList.add(DirectionsCriteria.EXCLUDE_CASH_ONLY_TOLLS)
        }

        internal fun excludeList(): List<String>? {
            return excludeList
        }
    }
}

private data class Waypoint(
    val coordinate: Point,
    val name: String?,
    val bearing: Double?,
    val zLevel: Int?,
    val isSilent: Boolean = false,
    val targetCoordinate: Point?
)
