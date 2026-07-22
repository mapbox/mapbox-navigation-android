package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.util.isEmpty
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.BEARING_NORTH
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.EMPTY_EDGE_INSETS
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.NULL_ISLAND_POINT
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.ZERO_PITCH
import com.mapbox.navigation.ui.maps.camera.data.OverviewFrameOptions
import com.mapbox.navigation.ui.maps.camera.data.ViewportProperty
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.utils.internal.logW
import kotlin.math.min

/**
 * Common base for the overview camera data sources.
 *
 * It owns all the framing logic that is agnostic to *what* is being framed: the camera property
 * holders and overrides, padding, the [additionalPointsToFrame], the [active] flag, and the
 * [evaluate] routine that turns a list of points into a [CameraOptions] overview frame.
 *
 * Subclasses only need to supply the feature-specific points via [getPointsToFrame]:
 * - [RouteOverviewViewportDataSource] frames the remaining route geometry (plus the puck).
 * - [PointsOverviewViewportDataSource] frames only the configured [additionalPointsToFrame].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
abstract class BaseOverviewViewportDataSource(
    protected val mapboxMap: MapboxMap,
) {

    private companion object {
        private const val LOG_CATEGORY = "BaseOverviewViewportDataSource"
    }

    val options = OverviewFrameOptions()

    var debugger: MapboxNavigationViewportDataSourceDebugger? = null

    private val centerProperty = ViewportProperty.CenterProperty(null, NULL_ISLAND_POINT)
    private val zoomProperty =
        ViewportProperty.ZoomProperty(null, options.maxZoom)
    private val bearingProperty = ViewportProperty.BearingProperty(null, BEARING_NORTH)
    private val pitchProperty = ViewportProperty.PitchProperty(null, ZERO_PITCH)

    var padding: EdgeInsets = EMPTY_EDGE_INSETS
    private var additionalPointsToFrame: List<Point> = emptyList()

    private var active = true

    var cameraOptions: CameraOptions =
        CameraOptions.Builder()
            .center(centerProperty.get())
            .zoom(zoomProperty.get())
            .bearing(bearingProperty.get())
            .pitch(pitchProperty.get())
            .padding(padding)
            .build()
        private set

    fun setActive(active: Boolean) {
        this.active = active
        if (active) {
            reevaluate()
        }
    }

    /**
     * Recomputes any cached state and produces a fresh [cameraOptions]. The base only re-runs
     * [evaluate]; subclasses that cache route data override this to refresh it first.
     */
    protected open fun reevaluate() {
        evaluate()
    }

    fun additionalPointsToFrame(points: List<Point>) {
        additionalPointsToFrame = ArrayList(points)
    }

    fun centerPropertyOverride(value: Point?) {
        centerProperty.override = value
    }

    fun zoomPropertyOverride(value: Double?) {
        zoomProperty.override = value
    }

    fun bearingPropertyOverride(value: Double?) {
        bearingProperty.override = value
    }

    fun pitchPropertyOverride(value: Double?) {
        pitchProperty.override = value
    }

    fun clearOverrides() {
        centerProperty.override = null
        zoomProperty.override = null
        bearingProperty.override = null
        pitchProperty.override = null
    }

    /**
     * Feature-specific points that should be framed by the overview camera.
     * The shared [additionalPointsToFrame] are always appended on top of these by [evaluate].
     */
    protected abstract fun getPointsToFrame(): List<Point>

    fun evaluate() {
        val cameraState = mapboxMap.cameraState
        runIfActive {
            PerformanceTracker.trackPerformanceSync("BaseOverviewViewportDataSource#evaluate") {
                val pointsForOverview = getPointsToFrame().toMutableList()
                pointsForOverview.addAll(additionalPointsToFrame)

                if (pointsForOverview.isEmpty()) {
                    // nothing to frame
                    options.run {
                        bearingProperty.fallback = cameraState.bearing
                        pitchProperty.fallback = cameraState.pitch
                        centerProperty.fallback = cameraState.center
                        zoomProperty.fallback = min(cameraState.zoom, maxZoom)
                    }
                } else {
                    pitchProperty.fallback = ZERO_PITCH
                    bearingProperty.fallback = normalizeBearing(
                        cameraState.bearing,
                        BEARING_NORTH,
                    )

                    val cameraFrame = mapboxMap.cameraForCoordinates(
                        pointsForOverview,
                        CameraOptions.Builder()
                            .padding(padding)
                            .bearing(bearingProperty.get())
                            .pitch(pitchProperty.get())
                            .build(),
                        null,
                        null,
                        null,
                    )

                    if (cameraFrame.isEmpty) {
                        logW(LOG_CATEGORY) { "CameraOptions is empty" }
                    } else {
                        // TODO should be non-null (reproducible with Camera test)
                        centerProperty.fallback = cameraFrame.center!!
                        zoomProperty.fallback = min(
                            cameraFrame.zoom!!,
                            options.maxZoom,
                        )
                    }
                }

                updateDebugger(pointsForOverview)

                options.run {
                    cameraOptions =
                        CameraOptions.Builder().apply {
                            if (centerUpdatesAllowed) {
                                center(centerProperty.get())
                            }
                            if (zoomUpdatesAllowed) {
                                zoom(zoomProperty.get())
                            }
                            if (bearingUpdatesAllowed) {
                                bearing(bearingProperty.get())
                            }
                            if (pitchUpdatesAllowed) {
                                pitch(pitchProperty.get())
                            }
                            if (paddingUpdatesAllowed) {
                                padding(padding)
                            }
                        }.build()
                }
            }
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun updateDebugger(pointsForOverview: List<Point>) {
        runIfActive {
            debugger?.let { updateDebuggerPoints(it, pointsForOverview) }
            debugger?.overviewUserPadding = padding
        }
    }

    /**
     * Publishes the framed points to the debugger field that matches this source, so that route
     * geometry and standalone points overview points don't overwrite each other.
     */
    protected abstract fun updateDebuggerPoints(
        debugger: MapboxNavigationViewportDataSourceDebugger,
        pointsForOverview: List<Point>,
    )

    protected fun runIfActive(action: () -> Unit) {
        if (active) {
            action()
        }
    }
}
