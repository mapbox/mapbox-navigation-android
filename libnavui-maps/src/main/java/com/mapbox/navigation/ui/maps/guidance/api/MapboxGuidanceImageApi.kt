package com.mapbox.navigation.ui.maps.guidance.api

import android.content.Context
import android.graphics.Bitmap
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapSnapshotInterface
import com.mapbox.maps.MapSnapshotOptions
import com.mapbox.maps.MapSnapshotterObserver
import com.mapbox.maps.MapboxOptions
import com.mapbox.maps.snapshotting.Snapshotter
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.api.guidanceimage.GuidanceImageApi
import com.mapbox.navigation.ui.maps.guidance.model.GuidanceImageOptions
import com.mapbox.navigation.ui.base.model.guidanceimage.GuidanceImageState
import com.mapbox.navigation.ui.maps.guidance.internal.GuidanceImageAction
import com.mapbox.navigation.ui.maps.guidance.internal.GuidanceImageProcessor
import com.mapbox.navigation.ui.maps.guidance.internal.GuidanceImageResult
import com.mapbox.navigation.utils.internal.ifNonNull
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

    private val snapshotter: Snapshotter
    private val snapshotterCallback = object : Snapshotter.SnapshotReadyCallback {
        override fun run(snapshot: Expected<MapSnapshotInterface?, String?>) {
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
    }

    init {
        val resourceOptions = MapboxOptions.getDefaultResourceOptions(context)
        val snapshotOptions = MapSnapshotOptions.Builder()
            .resourceOptions(resourceOptions)
            .size(options.size)
            .pixelRatio(options.density)
            .build()
        snapshotter = Snapshotter(context, snapshotOptions, object : MapSnapshotterObserver() {
            override fun onDidFailLoadingStyle(message: String) {

            }

            override fun onDidFinishLoadingStyle() {

            }

            override fun onStyleImageMissing(imageId: String) {

            }
        })
        snapshotter.styleURI = options.styleUri
    }

    override fun generateGuidanceImage(progress: RouteProgress, point: Point?) {
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
                        snapshotter.cameraOptions = getCameraOptions(point, progress.currentLegProgress?.currentStepProgress?.step?.maneuver())
                        snapshotter.start(snapshotterCallback)
                    }
                    else -> {
                    }
                }
            } ?: callback.onFailure(GuidanceImageState.GuidanceImageFailure.GuidanceImageUnavailable)
        } ?: callback.onFailure(GuidanceImageState.GuidanceImageFailure.GuidanceImageUnavailable)
    }

    private fun getCameraOptions(point: Point?, maneuver: StepManeuver?): CameraOptions {
        return ifNonNull(maneuver) {
            CameraOptions.Builder()
                .pitch(70.0)
                .zoom(17.5)
                .bearing(it.bearingAfter())
                .padding(options.edgeInsets)
                .center(point)
                .build()
        } ?: CameraOptions.Builder().build()
    }
}
