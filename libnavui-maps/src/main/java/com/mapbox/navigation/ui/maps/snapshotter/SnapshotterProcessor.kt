package com.mapbox.navigation.ui.maps.snapshotter

import android.graphics.Bitmap
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapSnapshotInterface
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.generated.SkyLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.navigation.ui.base.internal.route.RouteConstants
import com.mapbox.navigation.ui.maps.snapshotter.model.CameraPosition
import com.mapbox.navigation.ui.maps.snapshotter.model.MapboxSnapshotterOptions
import com.mapbox.navigation.ui.utils.internal.extensions.getBannerComponents
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import java.nio.ByteBuffer

internal object SnapshotterProcessor {

    private const val FRAME_POINT_DISTANCE_BEFORE_MANEUVER = 70.0
    private const val FRAME_POINT_DISTANCE_AFTER_MANEUVER = 10.0
    private const val SKY_LAYER_ID = "sky_snapshotter"
    private const val CAMERA_PITCH = 72.0
    private val snapshotterFramePoints = mutableListOf<Point>()

    /**
     * The function takes [SnapshotterAction] performs business logic and returns [SnapshotterResult]
     * @param action SnapshotterAction user specific commands
     * @return SnapshotterResult
     */
    fun process(action: SnapshotterAction): SnapshotterResult {
        return when (action) {
            is SnapshotterAction.GenerateSnapshot -> {
                isSnapshotAvailable(action.bannerInstruction)
            }
            is SnapshotterAction.GenerateCameraPosition -> {
                generateCameraPosition(
                    action.currentStepGeometry,
                    action.nextStepGeometry,
                    action.options
                )
            }
            is SnapshotterAction.GenerateBitmap -> {
                generateBitmap(action.options, action.snapshot)
            }
            is SnapshotterAction.GenerateSkyLayer -> {
                generateSkyLayer()
            }
            is SnapshotterAction.GenerateLineLayer -> {
                generateLineLayer()
            }
        }
    }

    private fun isSnapshotAvailable(instruction: BannerInstructions): SnapshotterResult {
        return ifNonNull(getComponentContainingSnapshot(instruction)) {
            SnapshotterResult.SnapshotAvailable
        } ?: SnapshotterResult.SnapshotUnavailable
    }

    private fun getComponentContainingSnapshot(
        bannerInstructions: BannerInstructions
    ): BannerComponents? {
        val bannerComponents = bannerInstructions.getBannerComponents()
        return when {
            bannerComponents != null -> {
                findSnapshotComponent(bannerComponents)
            }
            else -> {
                null
            }
        }
    }

    private fun findSnapshotComponent(
        componentList: MutableList<BannerComponents>
    ): BannerComponents? {
        return componentList.find {
            it.type() == BannerComponents.GUIDANCE_VIEW &&
                it.subType() == BannerComponents.SIGNBOARD
        }
    }

    /**
     * pointListFromDistanceToManeuver defines the list of points starting at some distance to
     * maneuver point including the maneuver point
     * pointListFromDistanceToManeuver.last() holds the point some distance before maneuver point
     * pointListFromDistanceToManeuver.first() holds the maneuver point itself
     *
     * @param currentGeometry String?
     * @param upcomingGeometry String?
     * @param options MapboxSnapshotterOptions
     * @return SnapshotterResult
     */
    private fun generateCameraPosition(
        currentGeometry: String?,
        upcomingGeometry: String?,
        options: MapboxSnapshotterOptions
    ): SnapshotterResult {
        return ifNonNull(currentGeometry, upcomingGeometry) { currGeometry, nextGeometry ->
            val pointListFromDistanceToManeuver =
                getPointList(currGeometry, FRAME_POINT_DISTANCE_BEFORE_MANEUVER, true)
            val pointAtDistanceBeforeManeuver =
                pointListFromDistanceToManeuver.last()
            val nextManeuverPoint = pointListFromDistanceToManeuver.first()
            val pointListFromManeuverToDistance = getPointList(
                nextGeometry,
                FRAME_POINT_DISTANCE_AFTER_MANEUVER,
                false
            )
            snapshotterFramePoints.clear()
            snapshotterFramePoints.addAll(
                pointListFromDistanceToManeuver.asReversed()
                    .plus(pointListFromManeuverToDistance)
            )
            val bearing = getBearing(pointAtDistanceBeforeManeuver, nextManeuverPoint)

            SnapshotterResult.SnapshotterCameraPosition(
                getCameraPosition(snapshotterFramePoints, options.edgeInsets, bearing, CAMERA_PITCH)
            )
        } ?: SnapshotterResult.SnapshotterCameraPosition(null)
    }

    private fun getBearing(point1: Point, point2: Point): Double =
        TurfMeasurement.bearing(point1, point2)

    private fun getCameraPosition(
        points: List<Point>,
        insets: EdgeInsets,
        bearing: Double,
        pitch: Double
    ): CameraPosition =
        CameraPosition(points, insets, bearing, pitch)

    private fun getPointList(
        geometry: String,
        distance: Double,
        shouldReverseLineString: Boolean
    ): MutableList<Point> =
        getPointsAlongLineStringSlice(geometry, distance, shouldReverseLineString)

    /**
     * The method returns a list of points starting from distance before maneuver point to end of
     * line string containing the maneuver point, or from the line string starting at the maneuver
     * point to distance after the maneuver point
     *
     * The function first decodes the entire geometry to get a list of points representing the route
     * line -> Based on whether you need the stating point before/after maneuver point, you generate
     * the lineString using either the list of points generated above as is or reverse them first ->
     * Now you slice the lineString from 0.0 to the distance you want -> Convert this sliced lineString
     * to list of points and return.
     *
     * Note: Reverse the [LineString], if you need the starting point to be at some distance from the
     * maneuver point.
     * @param geometry String
     * @param shouldReverse Boolean
     * @param distance Double
     * @return MutableList<Point>
     */
    private fun getPointsAlongLineStringSlice(
        geometry: String,
        distance: Double,
        shouldReverse: Boolean
    ): MutableList<Point> {
        val pointSequence: List<Point> = PolylineUtils.decode(geometry, Constants.PRECISION_6)
        val lineString = if (shouldReverse) {
            LineString.fromLngLats(pointSequence.asReversed())
        } else {
            LineString.fromLngLats(pointSequence)
        }
        val slicedLineString = TurfMisc.lineSliceAlong(
            lineString,
            0.0,
            distance,
            TurfConstants.UNIT_METERS
        )
        return slicedLineString.coordinates()
    }

    private fun generateBitmap(
        options: MapboxSnapshotterOptions,
        snapshot: Expected<MapSnapshotInterface?, String?>
    ): SnapshotterResult {
        when {
            snapshot.isValue -> {
                return snapshot.value?.let { snapshotInterface ->
                    val image = snapshotInterface.image()
                    val bitmap: Bitmap = Bitmap.createBitmap(
                        image.width,
                        image.height,
                        options.bitmapConfig
                    )
                    val buffer = ByteBuffer.wrap(image.data)
                    bitmap.copyPixelsFromBuffer(buffer)
                    SnapshotterResult.Snapshot.Success(bitmap)
                } ?: SnapshotterResult.Snapshot.Empty(snapshot.error)
            }
            snapshot.isError -> {
                return SnapshotterResult.Snapshot.Failure(snapshot.error)
            }
            else -> {
                return SnapshotterResult.Snapshot.Failure(snapshot.error)
            }
        }
    }

    private fun generateSkyLayer(): SnapshotterResult {
        val skyLayer = SkyLayer(SKY_LAYER_ID)
        skyLayer.skyGradient(
            interpolate {
                linear()
                skyRadialProgress()
                literal(0.0)
                literal("#B0C7CF")
                literal(1.0)
                literal("#7CA7BE")
            }
        )
        return SnapshotterResult.SnapshotSkyLayer(skyLayer)
    }

    private fun generateLineLayer(): SnapshotterResult {
        val lineLayer = lineLayer(
            RouteConstants.PRIMARY_ROUTE_LAYER_ID,
            RouteConstants.PRIMARY_ROUTE_SOURCE_ID
        ) {
            lineWidth(25.0)
            lineOpacity(1.0)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
            lineColor("#0F77FF")
        }
        return SnapshotterResult.SnapshotLineLayer(lineLayer)
    }
}
