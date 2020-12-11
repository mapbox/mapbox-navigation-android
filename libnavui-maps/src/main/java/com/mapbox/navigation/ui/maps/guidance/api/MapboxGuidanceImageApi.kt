package com.mapbox.navigation.ui.maps.guidance.api

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.mapbox.bindgen.Expected
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInterface
import com.mapbox.maps.MapSnapshotInterface
import com.mapbox.maps.MapSnapshotOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapboxOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size
import com.mapbox.maps.Snapshotter
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.SkyLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.api.guidanceimage.GuidanceImageApi
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.PRIMARY_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.base.model.guidanceimage.GuidanceImageState
import com.mapbox.navigation.ui.maps.guidance.internal.GuidanceImageAction
import com.mapbox.navigation.ui.maps.guidance.internal.GuidanceImageProcessor
import com.mapbox.navigation.ui.maps.guidance.internal.GuidanceImageResult
import com.mapbox.navigation.ui.maps.guidance.model.GuidanceImageOptions
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.turf.TurfConstants.UNIT_METERS
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import timber.log.Timber
import java.nio.ByteBuffer

/**
 * Mapbox implementation of [GuidanceImageApi]
 * @property context Context
 * @property options GuidanceImageOptions options allowing customization of [Bitmap] for snapshot based image
 * @property callback OnGuidanceImageReady callback resulting in appropriate [GuidanceImageState] to render on the view
 */
class MapboxGuidanceImageApi(
    val context: Context,
    val mapboxMap: MapboxMap,
    private val mapInterface: MapInterface,
    private val options: GuidanceImageOptions,
    private val callback: OnGuidanceImageReady
) : GuidanceImageApi {

    private val routeLinePoints = mutableListOf<Point>()
    private val snapshotter: Snapshotter
    private val snapshotterCallback = object : Snapshotter.SnapshotReadyCallback {
        override fun onSnapshotCreated(snapshot: Expected<MapSnapshotInterface?, String?>) {
            when {
                snapshot.isValue -> {
                    snapshot.value?.let { snapshotInterface ->
                        val image = snapshotInterface.image()
                        val bitmap: Bitmap = Bitmap.createBitmap(
                            image.width,
                            image.height,
                            options.bitmapConfig
                        )
                        val buffer: ByteBuffer = ByteBuffer.wrap(image.data)
                        bitmap.copyPixelsFromBuffer(buffer)
                        callback.onGuidanceImagePrepared(
                            GuidanceImageState.GuidanceImagePrepared(bitmap)
                        )
                    }
                        ?: callback.onFailure(
                            GuidanceImageState.GuidanceImageFailure.GuidanceImageEmpty(
                                snapshot.error
                            )
                        )
                }
                snapshot.isError -> {
                    callback.onFailure(
                        GuidanceImageState.GuidanceImageFailure.GuidanceImageError(snapshot.error)
                    )
                }
            }
        }

        override fun onStyleLoaded(style: Style) {
            val skyLayer = SkyLayer("sky_snapshotter")
            //skyLayer.skyType(SkyType.ATMOSPHERE)
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
            //skyLayer.skyGradientCenter(listOf(-34.0, 90.0))
            //skyLayer.skyGradientRadius(8.0)
            //skyLayer.skyAtmosphereSun(listOf(0.0, 90.0))
            style.addLayer(skyLayer)

            style.addSource(geoJsonSource(PRIMARY_ROUTE_SOURCE_ID) {
                geometry(LineString.fromLngLats(routeLinePoints))
            })
            val layer = lineLayer(PRIMARY_ROUTE_LAYER_ID, PRIMARY_ROUTE_SOURCE_ID) {
                lineWidth(16.0)
                lineOpacity(1.0)
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
                lineColor("#56A8FB")
            }
            style.addLayer(layer)
        }
    }

    init {
        val resourceOptions = MapboxOptions.getDefaultResourceOptions(context)
        val snapshotOptions = MapSnapshotOptions.Builder()
            .resourceOptions(resourceOptions)
            .size(options.size)
            .pixelRatio(options.density)
            .build()
        snapshotter = Snapshotter(context, snapshotOptions)
    }

    override fun generateGuidanceImage(progress: RouteProgress) {
        val bannerInstructions = progress.bannerInstructions
        ifNonNull(bannerInstructions) { b ->
            val result = GuidanceImageProcessor.process(
                GuidanceImageAction.GuidanceImageAvailable(b)
            )
            ifNonNull((result as GuidanceImageResult.GuidanceImageAvailable).bannerComponent) {
                val showUrlBased = GuidanceImageProcessor.process(
                    GuidanceImageAction.ShouldShowUrlBasedGuidance(it)
                )
                val showSnapshotBased = GuidanceImageProcessor.process(
                    GuidanceImageAction.ShouldShowSnapshotBasedGuidance(it)
                )
                when {
                    (
                        showUrlBased as
                            GuidanceImageResult.ShouldShowUrlBasedGuidance
                        ).isUrlBased -> {
                        Timber.d("Url based guidance views to be shown")
                    }
                    (
                        showSnapshotBased as
                            GuidanceImageResult.ShouldShowSnapshotBasedGuidance
                        ).isSnapshotBased -> {
                        val oldSize = mapInterface.size
                        mapInterface.size = Size(1024f, 512f)
                        val cameraOptions = getCameraOptions(
                            progress.currentLegProgress?.currentStepProgress?.step?.geometry(),
                            progress.currentLegProgress?.upcomingStep?.geometry()
                        )
                        /*
                        let top = screenCoordinate.y
                            let left = screenCoordinate.x
                            let bottom = view.bounds.size.height - top
                        let right = view.bounds.size.width - left
                        val displayMetrics = Resources.getSystem().getDisplayMetrics()

    */
                        val centerTop = ((mapInterface.size.height*options.density) - (options.edgeInsets.top + options.edgeInsets.bottom))/2 + (options.edgeInsets.top)
                        val centerLeft = ((mapInterface.size.width*options.density) - (options.edgeInsets.left + options.edgeInsets.right))/2 + (options.edgeInsets.left)
                        Log.d("TESTING", "centerTop: $centerTop + centerLeft: $centerLeft")
                        cameraOptions.padding = getEdgeInsets(mapInterface.size, ScreenCoordinate(centerLeft/options.density, centerTop/options.density))
                        snapshotter.setCameraOptions(cameraOptions)
                        mapInterface.size = oldSize
                        snapshotter.setUri(options.styleUri)
                        snapshotter.start(snapshotterCallback)
                    }
                    else -> {
                    }
                }
            } ?: callback.onFailure(
                GuidanceImageState.GuidanceImageFailure.GuidanceImageUnavailable
            )
        }
    }

    private fun getEdgeInsets(mapSize: Size, centerOffset: ScreenCoordinate = ScreenCoordinate(0.0, 0.0)): EdgeInsets {
        val mapCenterScreenCoordinate = ScreenCoordinate((mapSize.width / 2).toDouble(), (mapSize.height / 2).toDouble())
        val top = mapCenterScreenCoordinate.y + centerOffset.y
        val left = mapCenterScreenCoordinate.x + centerOffset.x
        return EdgeInsets(top, left, mapSize.height - top, mapSize.width - left)
    }

    private fun getCameraOptions(
        currentStepGeometry: String?,
        upcomingStepGeometry: String?
    ): CameraOptions {
        return ifNonNull(
            currentStepGeometry,
            upcomingStepGeometry
        ) { currentGeometry, nextGeometry ->
            // retrieve list of points 100m to maneuver including maneuver point
            val pointListFromDistanceToManeuver = getPointsAlongLineStringSlice(
                currentGeometry,
                true,
                70.0
            )
            // retrieve point 100m before maneuver point
            val pointAtDistanceBeforeManeuver = pointListFromDistanceToManeuver.last()
            // retrieve list of points 40m from maneuver including maneuver point
            val pointListFromManeuverToDistance = getPointsAlongLineStringSlice(
                nextGeometry,
                false,
                10.0
            )
            val nextManeuverPoint = pointListFromDistanceToManeuver.first()

            routeLinePoints.clear()
            routeLinePoints.addAll(pointListFromDistanceToManeuver.asReversed())
            routeLinePoints.addAll(pointListFromManeuverToDistance)

            return mapboxMap.cameraForCoordinates(
                routeLinePoints,
                options.edgeInsets,
                TurfMeasurement.bearing(pointAtDistanceBeforeManeuver, nextManeuverPoint),
                72.0
            )
        } ?: CameraOptions.Builder().build()
    }

    private fun getPointsAlongLineStringSlice(
        geometry: String,
        shouldReverse: Boolean,
        distanceToManeuver: Double
    ): MutableList<Point> {
        // calculate the point sequence for the geometry
        val pointSequence: List<Point> = PolylineUtils.decode(geometry, PRECISION_6)
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
            distanceToManeuver,
            UNIT_METERS
        )
        // get all the points in this sliced line string
        return slicedLineString.coordinates()
    }
}
