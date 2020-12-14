package com.mapbox.navigation.ui.maps.snapshotter.internal

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
import com.mapbox.navigation.ui.base.MapboxProcessor
import com.mapbox.navigation.ui.base.domain.BannerInstructionsApi
import com.mapbox.navigation.ui.base.internal.route.RouteConstants
import com.mapbox.navigation.ui.maps.snapshotter.model.CameraPosition
import com.mapbox.navigation.ui.maps.snapshotter.model.SnapshotOptions
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import java.nio.ByteBuffer

internal object SnapshotterProcessor :
    MapboxProcessor<SnapshotterAction, SnapshotterResult>,
    BannerInstructionsApi {

    private val snapshotterFramePoints = mutableListOf<Point>()

    override fun process(action: SnapshotterAction): SnapshotterResult {
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
        return ifNonNull(getComponentContainingSnapshot(instruction)) { component ->
            SnapshotterResult.SnapshotAvailable
        } ?: SnapshotterResult.SnapshotUnavailable
    }

    private fun getComponentContainingSnapshot(
        bannerInstructions: BannerInstructions
    ): BannerComponents? {
        val bannerComponents = getBannerComponents(bannerInstructions)
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

    private fun generateCameraPosition(
        currentGeometry: String?,
        upcomingGeometry: String?,
        options: SnapshotOptions
    ): SnapshotterResult {
        return ifNonNull(currentGeometry, upcomingGeometry) { currGeometry, nextGeometry ->
            // retrieve list of points 70m to maneuver including maneuver point
            val pointListFromDistanceToManeuver =
                getPointList(currGeometry, 70.0, true)
            // retrieve point 70m before maneuver point
            val pointAtDistanceBeforeManeuver =
                pointListFromDistanceToManeuver.last()
            val nextManeuverPoint = pointListFromDistanceToManeuver.first()
            // retrieve list of points 10m from maneuver including maneuver point
            val pointListFromManeuverToDistance = getPointList(
                nextGeometry,
                10.0,
                false
            )
            snapshotterFramePoints.clear()
            snapshotterFramePoints.addAll(
                pointListFromDistanceToManeuver.asReversed()
                    .plus(pointListFromManeuverToDistance)
            )
            val bearing = getBearing(pointAtDistanceBeforeManeuver, nextManeuverPoint)

            SnapshotterResult.SnapshotterCameraPosition(
                getCameraPosition(snapshotterFramePoints, options.edgeInsets, bearing, 72.0)
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
     * The method returns a list of points starting from [distanceToManeuver] to end of line string
     * or vice-versa.
     *
     * Note: Reverse the [LineString], if you need the starting point to be [distanceToManeuver]
     * units before the end of point.
     * @param geometry String
     * @param shouldReverse Boolean
     * @param distanceToManeuver Double
     * @return MutableList<Point>
     */
    private fun getPointsAlongLineStringSlice(
        geometry: String,
        distance: Double,
        shouldReverse: Boolean
    ): MutableList<Point> {
        // calculate the point sequence for the geometry
        val pointSequence: List<Point> = PolylineUtils.decode(geometry, Constants.PRECISION_6)
        val lineString = if (shouldReverse) {
            // find the LineString using [pointSequence] and reverse it.
            LineString.fromLngLats(pointSequence.asReversed())
        } else {
            // find the LineString using [pointSequence].
            LineString.fromLngLats(pointSequence)
        }
        // slice the reversed line string to end at 100m from the start
        val slicedLineString = TurfMisc.lineSliceAlong(
            lineString,
            0.0,
            distance,
            TurfConstants.UNIT_METERS
        )
        // get all the points in this sliced line string
        return slicedLineString.coordinates()
    }

    private fun generateBitmap(
        options: SnapshotOptions,
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
        val skyLayer = SkyLayer("sky_snapshotter")
        // skyLayer.skyType(SkyType.ATMOSPHERE)
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
        // skyLayer.skyGradientCenter(listOf(-34.0, 90.0))
        // skyLayer.skyGradientRadius(8.0)
        // skyLayer.skyAtmosphereSun(listOf(0.0, 90.0))
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
            lineColor("#56A8FB")
        }
        return SnapshotterResult.SnapshotLineLayer(lineLayer)
    }
}
