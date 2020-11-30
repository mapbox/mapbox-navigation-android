package com.mapbox.navigation.ui.maps.guidance.api

import android.content.Context
import android.graphics.Bitmap
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.skyRadialProgress
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.linear
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.SkyLayer
import com.mapbox.maps.extension.style.layers.properties.generated.SkyType
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.api.guidanceimage.GuidanceImageApi
import com.mapbox.navigation.ui.maps.guidance.model.GuidanceImageOptions
import com.mapbox.navigation.ui.base.model.guidanceimage.GuidanceImageState
import com.mapbox.navigation.ui.maps.guidance.internal.GuidanceImageAction
import com.mapbox.navigation.ui.maps.guidance.internal.GuidanceImageProcessor
import com.mapbox.navigation.ui.maps.guidance.internal.GuidanceImageResult
import com.mapbox.navigation.util.internal.ifNonNull
import com.mapbox.turf.TurfMeasurement.destination
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
    private val options: GuidanceImageOptions,
    private val callback: OnGuidanceImageReady
) : GuidanceImageApi {

    companion object {
        private const val REFERENCE_BEARING = 180.0
    }
    private val snapshotter: Snapshotter
    private val snapshotterCallback = object : Snapshotter.SnapshotReadyCallback {
        override fun onSnapshotCreated(snapshot: Expected<MapSnapshotInterface?, String?>) {
            when {
                snapshot.isValue -> {
                    snapshot.value?.let { snapshotInterface ->
                        val image = snapshotInterface.image()
                        val bitmap: Bitmap = Bitmap.createBitmap(image.width, image.height, options.bitmapConfig)
                        val buffer: ByteBuffer = ByteBuffer.wrap(image.data)
                        bitmap.copyPixelsFromBuffer(buffer)
                        callback.onGuidanceImagePrepared(GuidanceImageState.GuidanceImagePrepared(bitmap))
                    }
                        ?: callback.onFailure(GuidanceImageState.GuidanceImageFailure.GuidanceImageEmpty(snapshot.error))
                }
                snapshot.isError -> {
                    callback.onFailure(GuidanceImageState.GuidanceImageFailure.GuidanceImageError(snapshot.error))
                }
            }
        }

        override fun onStyleLoaded(style: Style) {
            val skyLayer = SkyLayer("sky_snapshotter")
            skyLayer.skyType(SkyType.ATMOSPHERE)
            skyLayer.skyGradient(interpolate {
                linear()
                skyRadialProgress()
                literal(0.0)
                literal("yellow")
                literal(1.0)
                literal("pink")
            })
            skyLayer.skyGradientCenter(listOf(-34.0, 90.0))
            skyLayer.skyGradientRadius(8.0)
            skyLayer.skyAtmosphereSun(listOf(0.0, 90.0))
            style.addLayer(skyLayer)
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
        snapshotter.setUri(options.styleUri)
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
                    (showUrlBased as GuidanceImageResult.ShouldShowUrlBasedGuidance).isUrlBased -> {
                        Timber.d("Url based guidance views to be shown")
                    }
                    (showSnapshotBased as GuidanceImageResult.ShouldShowSnapshotBasedGuidance).isSnapshotBased -> {
                        snapshotter.setCameraOptions(
                            getCameraOptions(
                                progress.upcomingStepPoints,
                                progress.currentLegProgress?.currentStepProgress?.step?.maneuver()
                            )
                        )
                        snapshotter.start(snapshotterCallback)
                    }
                    else -> {
                    }
                }
            } ?: callback.onFailure(GuidanceImageState.GuidanceImageFailure.GuidanceImageUnavailable)
        } ?: callback.onFailure(GuidanceImageState.GuidanceImageFailure.GuidanceImageUnavailable)
    }

    private fun getCameraOptions(upcomingPoints: List<Point>?, stepManeuver: StepManeuver?): CameraOptions {
        return ifNonNull(upcomingPoints, stepManeuver) { points, maneuver ->
            val bearing = maneuver.bearingAfter()
            bearing?.let { b ->
                val zoom = 17.0
                val pitch = 75.0
                val center = getCameraCenter(
                    points[0], b,
                    options.cameraCenterDistanceFromJunction,
                    options.cameraCenterDistanceUnit
                )
                CameraOptions.Builder()
                    .pitch(pitch)
                    .zoom(zoom)
                    .bearing(b)
                    .padding(options.edgeInsets)
                    .center(center)
                    .build()
            } ?: CameraOptions.Builder().build()
        } ?: CameraOptions.Builder().build()
    }

    private fun getCameraCenter(point: Point, bearing: Double, distance: Double, unit: String): Point {
        return when {
            bearing <= REFERENCE_BEARING -> {
                destination(point, distance, bearing + REFERENCE_BEARING, unit)
            }
            else -> {
                destination(point, distance, bearing - REFERENCE_BEARING, unit)
            }
        }
    }
}
