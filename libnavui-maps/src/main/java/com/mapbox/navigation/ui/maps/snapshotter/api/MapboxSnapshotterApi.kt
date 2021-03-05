package com.mapbox.navigation.ui.maps.snapshotter.api

import android.content.Context
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInterface
import com.mapbox.maps.MapSnapshotInterface
import com.mapbox.maps.MapSnapshotOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapboxOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size
import com.mapbox.maps.SnapshotCreatedListener
import com.mapbox.maps.SnapshotStyleListener
import com.mapbox.maps.Snapshotter
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.api.snapshotter.SnapshotReadyCallback
import com.mapbox.navigation.ui.base.api.snapshotter.SnapshotterApi
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.snapshotter.SnapshotState
import com.mapbox.navigation.ui.maps.snapshotter.SnapshotterAction
import com.mapbox.navigation.ui.maps.snapshotter.SnapshotterProcessor
import com.mapbox.navigation.ui.maps.snapshotter.SnapshotterResult
import com.mapbox.navigation.ui.maps.snapshotter.model.MapboxSnapshotterOptions
import com.mapbox.navigation.ui.utils.internal.ifNonNull

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

    private val routeLinePoints: MutableList<Point?> = mutableListOf()
    private val snapshotter: Snapshotter

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
                        snapshotter.setStyleListener(object : SnapshotStyleListener {
                            override fun onDidFinishLoadingStyle(style: Style) {
                                style.addSource(
                                    geoJsonSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) {
                                        geometry(LineString.fromLngLats(routeLinePoints))
                                    }
                                )
                                style.addLayer(
                                    (
                                        SnapshotterProcessor
                                            .process(SnapshotterAction.GenerateLineLayer)
                                            as SnapshotterResult.SnapshotLineLayer
                                        ).layer
                                )
                            }
                        })
                        snapshotter.start(object : SnapshotCreatedListener {
                            override fun onSnapshotResult(snapshot: MapSnapshotInterface?) {
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
                                                bitmapResult.error
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
}
