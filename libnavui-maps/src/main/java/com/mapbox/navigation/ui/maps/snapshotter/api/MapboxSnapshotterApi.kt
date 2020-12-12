package com.mapbox.navigation.ui.maps.snapshotter.api

import android.content.Context
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
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
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.api.snapshotter.SnapshotterApi
import com.mapbox.navigation.ui.base.internal.route.RouteConstants
import com.mapbox.navigation.ui.base.model.snapshotter.SnapshotState
import com.mapbox.navigation.ui.maps.snapshotter.internal.SnapshotterAction
import com.mapbox.navigation.ui.maps.snapshotter.internal.SnapshotterProcessor
import com.mapbox.navigation.ui.maps.snapshotter.internal.SnapshotterResult
import com.mapbox.navigation.ui.maps.snapshotter.model.SnapshotOptions
import com.mapbox.navigation.ui.utils.internal.ifNonNull

/**
 * Implementation of [SnapshotterApi] allowing you to generate snapshot showing a junction for select maneuvers.
 * @property context Context
 * @property mapboxMap reference to maps
 * @property options defining properties of snapshot
 * @property mapInterface MapInterface
 * @property onSnapshotReadyCallback callback that informs about the state of the snapshot
 */
class MapboxSnapshotterApi(
    val context: Context,
    private val mapboxMap: MapboxMap,
    private val options: SnapshotOptions,
    private val mapInterface: MapInterface,
    val onSnapshotReadyCallback: SnapshotReadyCallback
) : SnapshotterApi {

    private val routeLinePoints: MutableList<Point?> = mutableListOf()
    private val snapshotter: Snapshotter
    private val snapshotterCallback = object : Snapshotter.SnapshotReadyCallback {
        override fun onSnapshotCreated(snapshot: Expected<MapSnapshotInterface?, String?>) {
            val result =
                SnapshotterProcessor.process(SnapshotterAction.GenerateBitmap(options, snapshot))
            when (result) {
                is SnapshotterResult.Snapshot.Success -> {
                    onSnapshotReadyCallback.onSnapshotReady(
                        SnapshotState.SnapshotReady(result.bitmap)
                    )
                }
                is SnapshotterResult.Snapshot.Failure -> {
                    onSnapshotReadyCallback.onFailure(
                        SnapshotState.SnapshotFailure.SnapshotError(
                            snapshot.error
                        )
                    )
                }
                is SnapshotterResult.Snapshot.Empty -> {
                    onSnapshotReadyCallback.onFailure(
                        SnapshotState.SnapshotFailure.SnapshotEmpty(
                            snapshot.error
                        )
                    )
                }
            }
        }

        override fun onStyleLoaded(style: Style) {
            style.addLayer(
                (
                    SnapshotterProcessor
                        .process(SnapshotterAction.GenerateSkyLayer)
                        as SnapshotterResult.SnapshotSkyLayer
                    ).layer
            )
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

    /**
     * The method takes in [RouteProgress] and generates a snapshot based on the presence of
     * [BannerComponents] of type [BannerComponents.GUIDANCE_VIEW] and subType [BannerComponents.SIGNBOARD]
     * @param progress object representing [RouteProgress]
     */
    override fun generateSnapshot(progress: RouteProgress) {
        val bannerInstructions = progress.bannerInstructions
        ifNonNull(bannerInstructions) { instruction ->
            val result =
                SnapshotterProcessor.process(SnapshotterAction.GenerateSnapshot(instruction))
            when (result) {
                is SnapshotterResult.SnapshotUnavailable -> {
                    onSnapshotReadyCallback.onFailure(
                        SnapshotState.SnapshotFailure.SnapshotUnavailable
                    )
                }
                is SnapshotterResult.SnapshotAvailable -> {
                    val currentGeometry =
                        progress.currentLegProgress?.currentStepProgress?.step?.geometry()
                    val upcomingGeometry = progress.currentLegProgress?.upcomingStep?.geometry()
                    val cameraPosition = SnapshotterProcessor.process(
                        SnapshotterAction.GenerateCameraPosition(
                            currentGeometry,
                            upcomingGeometry,
                            options
                        )
                    )
                    ifNonNull(
                        (
                            cameraPosition
                                as SnapshotterResult.SnapshotterCameraPosition
                            ).cameraPosition
                    ) {
                        routeLinePoints.addAll(it.points.plus(progress.upcomingStepPoints!!))
                        val oldSize = mapInterface.size
                        mapInterface.size = Size(1024f, 512f)
                        val cameraOptions = mapboxMap.cameraForCoordinates(
                            it.points,
                            it.insets,
                            it.bearing,
                            it.pitch
                        )
                        cameraOptions.padding = getEdgeInsets(
                            mapInterface.size,
                            ScreenCoordinate(-70.0 * options.density, 30.0 * options.density)
                        )
                        snapshotter.setUri(options.styleUri)
                        snapshotter.setCameraOptions(cameraOptions)
                        mapInterface.size = oldSize
                        snapshotter.start(snapshotterCallback)
                    } ?: onSnapshotReadyCallback.onFailure(
                        SnapshotState.SnapshotFailure.SnapshotError(
                            "Camera position cannot be null"
                        )
                    )
                }
            }
        }
    }

    private fun getEdgeInsets(
        mapSize: Size,
        centerOffset: ScreenCoordinate = ScreenCoordinate(0.0, 0.0)
    ): EdgeInsets {
        val mapCenterScreenCoordinate =
            ScreenCoordinate((mapSize.width / 2).toDouble(), (mapSize.height / 2).toDouble())
        val top = mapCenterScreenCoordinate.y + centerOffset.y
        val left = mapCenterScreenCoordinate.x + centerOffset.x
        return EdgeInsets(top, left, mapSize.height - top, mapSize.width - left)
    }
}
