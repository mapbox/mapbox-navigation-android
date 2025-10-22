package com.mapbox.androidauto.feedback.ui

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
import com.mapbox.navigation.utils.internal.android.toBitmap
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

internal class CarFeedbackIconDownloader(private val screen: Screen) {

    // null means the image is currently being downloaded
    private val downloadedImages = hashMapOf<Uri, CarIcon?>()

    fun getOrDownload(icon: CarFeedbackIcon): CarIcon? {
        return when (icon) {
            is CarFeedbackIcon.Local -> icon.icon
            is CarFeedbackIcon.Remote -> {
                screen.lifecycleScope.launch { downloadImage(icon.uri) }
                downloadedImages[icon.uri]
            }
        }
    }

    private suspend fun downloadImage(uri: Uri) {
        if (uri in downloadedImages) return
        downloadedImages[uri] = null
        val resource = withTimeoutOrNull(IMAGE_DOWNLOAD_TIMEOUT) {
            Glide.with(screen.carContext).request(uri)
        }
        downloadedImages[uri] = if (resource != null) {
            CarIcon.Builder(IconCompat.createWithBitmap(resource.toBitmap())).build()
        } else {
            CarIcon.ERROR
        }
        screen.invalidate()
    }

    private suspend inline fun RequestManager.request(uri: Uri): Drawable? {
        return suspendCancellableCoroutine { continuation ->
            val target = object : CustomTarget<Drawable>() {

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    continuation.resume(value = null)
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?,
                ) {
                    continuation.resume(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Intentionally empty
                }
            }
            continuation.invokeOnCancellation { clear(target) }
            load(uri).into(target)
        }
    }

    private companion object {
        private const val IMAGE_DOWNLOAD_TIMEOUT = 3000L
    }
}
