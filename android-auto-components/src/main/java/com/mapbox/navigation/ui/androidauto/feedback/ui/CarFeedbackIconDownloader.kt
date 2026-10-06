package com.mapbox.navigation.ui.androidauto.feedback.ui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.car.app.Screen
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.roundToInt

internal class CarFeedbackIconDownloader(private val screen: Screen) {

    private val icons = hashMapOf<Uri, IconState>()

    /**
     * @return the icon, `null` while it is being downloaded, or [CarIcon.ERROR] once every
     * download attempt has failed
     */
    fun getOrDownload(icon: CarFeedbackIcon): CarIcon? {
        return when (icon) {
            is CarFeedbackIcon.Local -> icon.icon
            is CarFeedbackIcon.Remote -> when (val state = icons[icon.uri]) {
                null -> {
                    download(icon.uri, attempt = 1)
                    null
                }
                IconState.Loading -> null
                is IconState.Loaded -> state.icon
                is IconState.Failed -> if (state.attempts < MAX_ATTEMPTS) {
                    download(icon.uri, attempt = state.attempts + 1)
                    null
                } else {
                    CarIcon.ERROR
                }
            }
        }
    }

    private fun download(uri: Uri, attempt: Int) {
        icons[uri] = IconState.Loading
        screen.lifecycleScope.launch {
            // Glide decodes and downscales to the grid icon size on its own executors.
            val bitmap = withTimeoutOrNull(IMAGE_DOWNLOAD_TIMEOUT) {
                Glide.with(screen.carContext).requestBitmap(uri, iconSizePx())
            }
            icons[uri] = if (bitmap != null) {
                IconState.Loaded(CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build())
            } else {
                // Retried on the next template build, up to MAX_ATTEMPTS.
                IconState.Failed(attempt)
            }
            screen.invalidate()
        }
    }

    private fun iconSizePx(): Int {
        val density = screen.carContext.resources.displayMetrics.density
        return (GRID_ICON_SIZE_DP * density).roundToInt()
    }

    private suspend fun RequestManager.requestBitmap(uri: Uri, sizePx: Int): Bitmap? {
        return suspendCancellableCoroutine { continuation ->
            val target = object : CustomTarget<Bitmap>() {

                // Glide restarts failed requests when connectivity returns, so a target can be
                // called again after the continuation has already resumed.
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    if (continuation.isActive) continuation.resume(value = null)
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?,
                ) {
                    if (continuation.isActive) continuation.resume(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Intentionally empty
                }
            }
            continuation.invokeOnCancellation { clear(target) }
            asBitmap().load(uri).override(sizePx).into(target)
        }
    }

    private sealed class IconState {
        object Loading : IconState()
        class Loaded(val icon: CarIcon) : IconState()
        class Failed(val attempts: Int) : IconState()
    }

    private companion object {
        private const val IMAGE_DOWNLOAD_TIMEOUT = 3000L
        private const val MAX_ATTEMPTS = 3

        // The size of a grid item icon in the car app templates.
        private const val GRID_ICON_SIZE_DP = 64
    }
}
