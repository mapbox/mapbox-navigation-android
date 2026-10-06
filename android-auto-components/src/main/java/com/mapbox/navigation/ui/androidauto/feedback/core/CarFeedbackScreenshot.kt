package com.mapbox.navigation.ui.androidauto.feedback.core

import android.graphics.Bitmap
import com.mapbox.common.dispatchers.SdkDispatchers
import com.mapbox.maps.MapSurface
import com.mapbox.navigation.core.telemetry.events.BitmapEncodeOptions
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Captures the map without blocking the calling thread and encodes it on [encodeDispatcher].
 *
 * The snapshot callback is invoked on the render thread, and it is never invoked when the
 * surface is torn down before the snapshot is rendered, so the wait is bounded by [timeoutMs].
 * A bitmap delivered after the wait has ended is recycled.
 *
 * @return the Base64-encoded screenshot, or `null` when the map could not be captured or encoded
 */
internal suspend fun MapSurface.captureEncodedScreenshot(
    options: BitmapEncodeOptions,
    timeoutMs: Long = SNAPSHOT_TIMEOUT_MS,
    encodeDispatcher: CoroutineDispatcher = SdkDispatchers.Default,
): String? {
    val bitmap = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine<Bitmap?> { continuation ->
            snapshot { bitmap ->
                continuation.resume(bitmap) { bitmap?.recycle() }
            }
        }
    } ?: return null
    return try {
        withContext(encodeDispatcher) {
            try {
                FeedbackHelper.encodeScreenshot(bitmap, options)
            } catch (e: Exception) {
                logAndroidAutoFailure("Car feedback screenshot could not be encoded", e)
                null
            }
        }
    } finally {
        // Also reached when cancelled before the encoding block starts.
        bitmap.recycle()
    }
}

// Above the renderer's own one-second wait for a snapshot.
private const val SNAPSHOT_TIMEOUT_MS = 1500L
