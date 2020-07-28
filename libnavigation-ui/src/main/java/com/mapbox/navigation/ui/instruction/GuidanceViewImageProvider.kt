package com.mapbox.navigation.ui.instruction

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.utils.internal.ifNonNull
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.lang.Exception
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * The class serves as a medium to emit bitmaps for the respective guidance view URL embedded in
 * [BannerInstructions]
 * @constructor
 */
class GuidanceViewImageProvider() {

    companion object {
        private val USER_AGENT_KEY = "User-Agent"
        private val USER_AGENT_VALUE = "MapboxJava/"
    }

    /**
     * This is added a solution to bypass the issue with Picasso where [com.squareup.picasso.Target]
     * get's garbage collected and guidance view is hence not rendered. Found the solution here
     * https://stackoverflow.com/questions/24180805/onbitmaploaded-of-target-object-not-called-on-first-load#answers
     */
    private val targets: MutableList<Target> = mutableListOf()
    private val okHttpClient = OkHttpClient.Builder().addInterceptor { chain: Interceptor.Chain ->
        chain.proceed(
            chain.request().newBuilder().addHeader(USER_AGENT_KEY, USER_AGENT_VALUE).build()
        )
    }.build()

    /**
     * The API reads the bannerInstruction and returns a guidance view bitmap if one is available
     * @param bannerInstructions [BannerInstructions]
     * @param context [Context]
     * @param callback [OnGuidanceImageDownload] Callback that is triggered based on appropriate state of image downloading
     */
    fun renderGuidanceView(
        bannerInstructions: BannerInstructions,
        context: Context,
        callback: OnGuidanceImageDownload
    ) {
        val target = object : Target {
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                targets.remove(this)
                callback.onFailure(e?.message)
            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                targets.remove(this)
                ifNonNull(bitmap) { b ->
                    callback.onGuidanceImageReady(b)
                } ?: callback.onFailure("Something went wrong. Bitmap not received")
            }
        }
        val bannerView = bannerInstructions.view()
        ifNonNull(bannerView) { view ->
            val bannerComponents = view.components()
            ifNonNull(bannerComponents) { components ->
                components.forEachIndexed { _, component ->
                    component.takeIf { c -> c.type() == BannerComponents.GUIDANCE_VIEW }?.let {
                        targets.add(target)
                        Picasso.Builder(context).downloader(OkHttp3Downloader(okHttpClient))
                            .build()
                            .load(it.imageUrl())
                            .into(target)
                    }
                }
            } ?: callback.onNoGuidanceImageUrl()
        } ?: callback.onNoGuidanceImageUrl()
    }

    /**
     * The API allows you to cancel the rendering of guidance view.
     * @param context Context
     */
    fun cancelRender(context: Context) {
        if (targets.isNotEmpty()) {
            for (target in targets) {
                Picasso.Builder(context).build().cancelRequest(target)
            }
            targets.clear()
        }
    }

    /**
     * Callback that is triggered based on appropriate state of image downloading
     */
    interface OnGuidanceImageDownload {
        /**
         * Triggered when the image has been downloaded and is ready to be used.
         * @param bitmap Bitmap
         */
        fun onGuidanceImageReady(bitmap: Bitmap)

        /**
         * Triggered when their is no URL to render
         */
        fun onNoGuidanceImageUrl()

        /**
         * Triggered when there is a failure to download the image
         * @param message String?
         */
        fun onFailure(message: String?)
    }
}
