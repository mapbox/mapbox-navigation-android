package com.mapbox.navigation.ui.maps.snapshotter.api

import android.content.Context
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapInterface
import com.mapbox.maps.MapSnapshotOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size
import com.mapbox.maps.SnapshotStyleListener
import com.mapbox.maps.Snapshotter
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.snapshotter.SnapshotterAction
import com.mapbox.navigation.ui.maps.snapshotter.SnapshotterProcessor
import com.mapbox.navigation.ui.maps.snapshotter.SnapshotterResult
import com.mapbox.navigation.ui.maps.snapshotter.model.CameraPosition
import com.mapbox.navigation.ui.maps.snapshotter.model.MapboxSnapshotterOptions
import com.mapbox.navigation.ui.maps.snapshotter.model.SnapshotError
import com.mapbox.navigation.ui.maps.snapshotter.model.SnapshotValue
import com.mapbox.navigation.ui.utils.internal.ifNonNull

/**
 * Mapbox Snapshotter Api allows you to generate snapshot showing a junction for select maneuvers.
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
) {

    private val routeLinePoints: MutableList<Point?> = mutableListOf()
    private val snapshotter: Snapshotter

    init {
        val resourceOptions = MapInitOptions.getDefaultResourceOptions(context)
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
    fun generateSnapshot(
        progress: RouteProgress,
        consumer: MapboxNavigationConsumer<Expected<SnapshotError, SnapshotValue>>
    ) {
        val bannerInstructions = progress.bannerInstructions
        ifNonNull(bannerInstructions) { instruction ->
            val action = SnapshotterAction.GenerateSnapshot(instruction)
            val result = SnapshotterProcessor.process(action)
            when (result) {
                is SnapshotterResult.SnapshotAvailable -> {
                    val currentGeometry =
                        progress.currentLegProgress?.currentStepProgress?.step?.geometry()
                    val upcomingGeometry = progress.currentLegProgress?.upcomingStep?.geometry()
                    val cameraPosition = generateCameraPosition(currentGeometry, upcomingGeometry)
                    ifNonNull(cameraPosition, progress.upcomingStepPoints) { position, stepPoints ->
                        routeLinePoints.addAll(position.points.plus(stepPoints))
                        val mapInterface = getMapInterface()
                        val oldSize = mapInterface.size
                        val cameraOptions = getCameraOptions(mapInterface, position)
                        snapshotter.setCamera(cameraOptions)
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
                        snapshotter.start { snapshot ->
                            val bitmapAction =
                                SnapshotterAction.GenerateBitmap(options, snapshot)
                            val bitmapResult = SnapshotterProcessor.process(bitmapAction)
                            when (bitmapResult) {
                                is SnapshotterResult.Snapshot.Success -> {
                                    consumer.accept(
                                        ExpectedFactory.createValue(
                                            SnapshotValue(bitmapResult.bitmap)
                                        )
                                    )
                                }
                                is SnapshotterResult.Snapshot.Failure -> {
                                    consumer.accept(
                                        ExpectedFactory.createError(
                                            SnapshotError(bitmapResult.error, null)
                                        )
                                    )
                                }
                                else -> {
                                    consumer.accept(
                                        ExpectedFactory.createError(
                                            SnapshotError(
                                                "Inappropriate $result emitted for $action.",
                                                null
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    } ?: consumer.accept(
                        ExpectedFactory.createError(
                            SnapshotError(
                                "Camera position or upcoming step points cannot be null",
                                null
                            )
                        )
                    )
                }
                is SnapshotterResult.SnapshotUnavailable -> {
                    consumer.accept(
                        ExpectedFactory.createError(
                            SnapshotError(
                                "No snapshot available for the current maneuver",
                                null
                            )
                        )
                    )
                }
                is SnapshotterResult.SnapshotterCameraPosition,
                is SnapshotterResult.Snapshot.Success,
                is SnapshotterResult.Snapshot.Failure,
                is SnapshotterResult.SnapshotLineLayer -> {
                    consumer.accept(
                        ExpectedFactory.createError(
                            SnapshotError(
                                "Inappropriate $result emitted for $action.",
                                null
                            )
                        )
                    )
                }
            }
        }
    }

    /**
     * The method stops the process of taking snapshot and destroys any related callback.
     */
    fun cancel() {
        snapshotter.cancel()
    }

    private fun generateCameraPosition(
        currentGeometry: String?,
        upcomingGeometry: String?
    ): CameraPosition? {
        val action = SnapshotterAction.GenerateCameraPosition(
            currentGeometry,
            upcomingGeometry,
            options
        )
        val result = SnapshotterProcessor.process(action)
            as SnapshotterResult.SnapshotterCameraPosition
        return result.cameraPosition
    }

    private fun getCameraOptions(
        mapInterface: MapInterface,
        cameraPosition: CameraPosition
    ): CameraOptions {
        mapInterface.size = Size(options.size.width, options.size.height)
        val cameraOptions = mapboxMap.cameraForCoordinates(
            cameraPosition.points,
            cameraPosition.insets,
            cameraPosition.bearing,
            cameraPosition.pitch
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
        val padding = getEdgeInsets(
            Size(
                mapInterface.size.width * options.density,
                mapInterface.size.height * options.density
            ),
            ScreenCoordinate(
                centerLeft,
                centerTop
            )
        )
        return cameraOptions
            .toBuilder()
            .padding(padding)
            .build()
    }

    private fun getEdgeInsets(
        mapSize: Size,
        centerOffset: ScreenCoordinate = ScreenCoordinate(0.0, 0.0)
    ): EdgeInsets {
        val top = centerOffset.y
        val left = centerOffset.x
        return EdgeInsets(top, left, mapSize.height - top, mapSize.width - left)
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
}
