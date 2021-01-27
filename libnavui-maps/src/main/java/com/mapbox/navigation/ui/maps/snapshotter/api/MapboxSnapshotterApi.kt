package com.mapbox.navigation.ui.maps.snapshotter.api

import android.content.Context
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInterface
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapboxOptions
import com.mapbox.maps.MapSnapshotInterface
import com.mapbox.maps.MapSnapshotOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size
import com.mapbox.maps.Snapshotter
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.api.snapshotter.SnapshotReadyCallback
import com.mapbox.navigation.ui.base.api.snapshotter.SnapshotterApi
import com.mapbox.navigation.ui.base.internal.route.RouteConstants
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.MAX_DEGREES
import com.mapbox.navigation.ui.base.model.snapshotter.SnapshotState
import com.mapbox.navigation.ui.maps.internal.route.arrow.RouteArrowUtils
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowState
import com.mapbox.navigation.ui.maps.snapshotter.internal.SnapshotterAction
import com.mapbox.navigation.ui.maps.snapshotter.internal.SnapshotterProcessor
import com.mapbox.navigation.ui.maps.snapshotter.internal.SnapshotterResult
import com.mapbox.navigation.ui.maps.snapshotter.model.MapboxSnapshotterOptions
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.turf.TurfMeasurement
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Implementation of [SnapshotterApi] allowing you to generate snapshot showing a junction for select maneuvers.
 * @property context Context
 * @property mapboxMap reference to maps
 * @property options defining properties of snapshot
 * @property mapView MapView
 */
class MapboxSnapshotterApi(
    val context: Context,
    private val mapboxMap: MapboxMap,
    private var options: MapboxSnapshotterOptions,
    private val mapView: MapView
) : SnapshotterApi {

    companion object {
        const val ARROW_BEARING_ADVANCED = "mapbox-navigation-arrow-bearing-advanced"
        const val ARROW_SHAFT_SOURCE_ID_ADVANCED = "mapbox-navigation-arrow-shaft-source-advanced"
        const val ARROW_HEAD_SOURCE_ID_ADVANCED = "mapbox-navigation-arrow-head-source-advanced"
    }
    private val routeLinePoints: MutableList<Point?> = mutableListOf()
    private val arrows: CopyOnWriteArrayList<List<Point>> = CopyOnWriteArrayList()
    private val snapshotter: Snapshotter
    private lateinit var routeArrowState: RouteArrowState.UpdateManeuverArrowState
    private val mapboxRouteArrowView = MapboxRouteArrowView(
        RouteArrowOptions
            .Builder(context)
            .withAboveLayerId(RouteConstants.PRIMARY_ROUTE_LAYER_ID)
            .build()
    )

    init {
        val resourceOptions = MapboxOptions.getDefaultResourceOptions(context)
        val mapSnapshotOptions = MapSnapshotOptions.Builder()
            .resourceOptions(resourceOptions)
            .size(options.size)
            .pixelRatio(options.density)
            .build()
        snapshotter = Snapshotter(context, mapSnapshotOptions)
    }

    /**
     * The method takes in [RouteProgress] and generates a snapshot based on the presence of
     * [BannerComponents] of type [BannerComponents.GUIDANCE_VIEW] and subType [BannerComponents.SIGNBOARD]
     * @param progress object representing [RouteProgress]
     * @param callback informs about the state of the snapshot
     */
    override fun generateSnapshot(progress: RouteProgress, callback: SnapshotReadyCallback) {
        val bannerInstructions = progress.bannerInstructions
        ifNonNull(bannerInstructions) { instruction ->
            val action = SnapshotterAction.GenerateSnapshot(instruction)
            val result = SnapshotterProcessor.process(action)

            when (result) {
                is SnapshotterResult.SnapshotUnavailable -> {
                    callback.onFailure(
                        SnapshotState.SnapshotFailure.SnapshotUnavailable
                    )
                }
                is SnapshotterResult.SnapshotAvailable -> {
                    val currentGeometry =
                        progress.currentLegProgress?.currentStepProgress?.step?.geometry()
                    val upcomingGeometry = progress.currentLegProgress?.upcomingStep?.geometry()
                    val camera = SnapshotterProcessor.process(
                        SnapshotterAction.GenerateCameraPosition(
                            currentGeometry,
                            upcomingGeometry,
                            options
                        )
                    ) as SnapshotterResult.SnapshotterCameraPosition
                    ifNonNull(camera.cameraPosition) {
                        routeLinePoints.addAll(it.points.plus(progress.upcomingStepPoints!!))
                        val head = getFeatureForArrowHead(routeLinePoints.mapNotNull { it })
                        routeArrowState = RouteArrowState.UpdateManeuverArrowState(
                            listOf(),
                            Feature.fromGeometry(LineString.fromLngLats(routeLinePoints)),
                            head
                        )
                        val mapInterface = getMapInterface()
                        val oldSize = mapInterface.size
                        mapInterface.size = Size(options.size.width, options.size.height)
                        val cameraOptions = mapboxMap.cameraForCoordinates(
                            it.points,
                            it.insets,
                            it.bearing,
                            it.pitch
                        )
                        val centerTop =
                            (
                                (mapInterface.size.height * options.density) -
                                    (options.edgeInsets.top + options.edgeInsets.bottom)
                                ) / 2 + (options.edgeInsets.top)
                        val centerLeft =
                            (
                                (mapInterface.size.width * options.density) -
                                    (options.edgeInsets.left + options.edgeInsets.right)
                                ) / 2 + (options.edgeInsets.left)
                        cameraOptions.padding = getEdgeInsets(
                            Size(
                                mapInterface.size.width * options.density,
                                mapInterface.size.height * options.density
                            ),
                            ScreenCoordinate(
                                centerLeft,
                                centerTop
                            )
                        )
                        snapshotter.setCameraOptions(cameraOptions)
                        snapshotter.setUri(options.styleUri)
                        mapInterface.size = oldSize
                        snapshotter.start(object : Snapshotter.SnapshotReadyCallback {
                            override fun onSnapshotCreated(
                                snapshot: Expected<MapSnapshotInterface?, String?>
                            ) {
                                val bitmapAction = SnapshotterAction.GenerateBitmap(
                                    options,
                                    snapshot
                                )
                                val bitmapResult = SnapshotterProcessor.process(bitmapAction)

                                when (bitmapResult) {
                                    is SnapshotterResult.Snapshot.Success -> {
                                        callback.onSnapshotReady(
                                            SnapshotState.SnapshotReady(bitmapResult.bitmap)
                                        )
                                    }
                                    is SnapshotterResult.Snapshot.Failure -> {
                                        callback.onFailure(
                                            SnapshotState.SnapshotFailure.SnapshotError(
                                                snapshot.error
                                            )
                                        )
                                    }
                                    is SnapshotterResult.Snapshot.Empty -> {
                                        callback.onFailure(
                                            SnapshotState.SnapshotFailure.SnapshotEmpty(
                                                snapshot.error
                                            )
                                        )
                                    }
                                    else -> {
                                        throw RuntimeException(
                                            "Inappropriate $bitmapResult emitted for " +
                                                "$bitmapAction processed."
                                        )
                                    }
                                }
                            }

                            override fun onStyleLoaded(style: Style) {
                                style.addSource(
                                    geoJsonSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) {
                                        geometry(LineString.fromLngLats(listOf()))
                                    }
                                )
                                style.addLayer(
                                    (
                                        SnapshotterProcessor
                                            .process(SnapshotterAction.GenerateLineLayer)
                                            as SnapshotterResult.SnapshotLineLayer
                                        ).layer
                                )
                                mapboxRouteArrowView.render(style, routeArrowState)

                                /*style.addSource(
                                    geoJsonSource(ARROW_SHAFT_SOURCE_ID_ADVANCED) {
                                        featureCollection(getArrowShaftFeatureCollection())
                                    }
                                )
                                style.addSource(
                                    geoJsonSource(ARROW_HEAD_SOURCE_ID_ADVANCED) {
                                        featureCollection(getArrowHeadFeatureCollection())
                                    }
                                )
                                RouteArrowUtils.initializeLayers(style,
                                    RouteArrowOptions
                                        .Builder(context)
                                        .withAboveLayerId(RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
                                        .build()
                                )*/
                            }
                        })
                    } ?: callback.onFailure(
                        SnapshotState.SnapshotFailure.SnapshotError(
                            "Camera position cannot be null"
                        )
                    )
                }
                else -> {
                    throw RuntimeException("Inappropriate $result emitted for $action processed.")
                }
            }
        }
    }

    /**
     * The method stops the process of taking snapshot and destroys any related callback.
     */
    override fun cancel() {
        snapshotter.cancel()
    }

    private fun getMapInterface(): MapInterface {
        val privateMapView =
            Class.forName("com.mapbox.maps.MapView").getDeclaredField("mapController")
        privateMapView.isAccessible = true
        val controller = privateMapView.get(mapView)

        val privateMapController =
            Class.forName("com.mapbox.maps.MapController").getDeclaredField("renderer")
        privateMapController.isAccessible = true
        val renderer = privateMapController.get(controller)

        val privateMapRenderer =
            Class.forName("com.mapbox.maps.renderer.MapboxRenderer").getDeclaredField("map")
        privateMapRenderer.isAccessible = true
        val mapInterface = privateMapRenderer.get(renderer) as MapInterface

        return mapInterface
    }

    private fun getEdgeInsets(
        mapSize: Size,
        centerOffset: ScreenCoordinate = ScreenCoordinate(0.0, 0.0)
    ): EdgeInsets {
        val top = centerOffset.y
        val left = centerOffset.x
        return EdgeInsets(top, left, mapSize.height - top, mapSize.width - left)
    }

    private fun getArrowShaftFeatureCollection(): FeatureCollection {
        val shaftFeatures = arrows.map { pointList ->
            LineString.fromLngLats(pointList)
        }.map { lineString ->
            Feature.fromGeometry(lineString)
        }
        return FeatureCollection.fromFeatures(shaftFeatures)
    }

    private fun getArrowHeadFeatureCollection(): FeatureCollection {
        val arrowHeadFeatures = arrows.map { pointList ->
            val azimuth = TurfMeasurement.bearing(pointList[pointList.size - 2], pointList[pointList.size - 1])
            val delta: Double = MAX_DEGREES - 0.0

            val firstMod: Double = (azimuth - 0.0) % delta
            val secondMod = (firstMod + delta) % delta

            val value = secondMod + 0.0
            Feature.fromGeometry(pointList[pointList.size - 1]).also { feature ->
                feature.addNumberProperty(
                    ARROW_BEARING_ADVANCED,
                    value
                )
            }
        }
        return FeatureCollection.fromFeatures(arrowHeadFeatures)
    }

    private fun getShowArrowModifications(): List<Pair<String, Visibility>> {
        return listOf(
            Pair(RouteConstants.ARROW_SHAFT_LINE_LAYER_ID, Visibility.VISIBLE),
            Pair(RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID, Visibility.VISIBLE),
            Pair(RouteConstants.ARROW_HEAD_CASING_LAYER_ID, Visibility.VISIBLE),
            Pair(RouteConstants.ARROW_HEAD_LAYER_ID, Visibility.VISIBLE)
        )
    }

    // This came from MathUtils in the Maps SDK which may have been removed.
    private fun wrap(value: Double, min: Double, max: Double): Double {
        val delta = max - min
        val firstMod = (value - min) % delta
        val secondMod = (firstMod + delta) % delta
        return secondMod + min
    }

    private fun getFeatureForArrowHead(points: List<Point>): Feature {
        val azimuth = TurfMeasurement.bearing(points[points.size - 2], points[points.size - 1])
        return Feature.fromGeometry(points[points.size - 1]).also {
            it.addNumberProperty(
                RouteConstants.ARROW_BEARING,
                wrap(azimuth, 0.0, RouteConstants.MAX_DEGREES)
            )
        }
    }
}
