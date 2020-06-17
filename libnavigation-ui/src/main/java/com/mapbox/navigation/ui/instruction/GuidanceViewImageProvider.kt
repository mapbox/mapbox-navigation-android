package com.mapbox.navigation.ui.instruction

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

/**
 * The class serves as a medium to emit bitmaps for the respective guidance view URL embedded in
 * [BannerInstructions]
 * @constructor
 */
class GuidanceViewImageProvider {

    private companion object {
        private const val USER_AGENT_KEY = "User-Agent"
        private const val USER_AGENT_VALUE = "MapboxJava/"
        private const val ERROR_VIEW_IMAGE_URL_NULL = "Guidance View Image URL is null"
    }

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private val okHttpClient = OkHttpClient.Builder().addInterceptor { chain: Interceptor.Chain ->
        chain.proceed(
            chain.request().newBuilder().addHeader(USER_AGENT_KEY, USER_AGENT_VALUE).build()
        )
    }.build()

    /**
     * The API reads the bannerInstruction and returns a guidance view bitmap if one is available
     * @param bannerInstructions [BannerInstructions]
     * @param callback [OnGuidanceImageDownload] Callback that is triggered based on appropriate state of image downloading
     */
    fun renderGuidanceView(
        bannerInstructions: BannerInstructions,
        callback: OnGuidanceImageDownload
    ) {
        val bannerView = bannerInstructions.view()
        ifNonNull(bannerView) { view ->
            val bannerComponents = view.components()
            ifNonNull(bannerComponents) { components ->
                components.forEach { component ->
                    component.takeIf { it.type() == BannerComponents.GUIDANCE_VIEW }?.let {
                        ifNonNull(it.imageUrl()) { url ->
                            mainJobController.scope.launch {
                                val response = getBitmap(url)
                                response.bitmap?.let { b ->
                                    callback.onGuidanceImageReady(b)
                                } ?: callback.onFailure(response.error)
                            }
                        } ?: callback.onFailure(ERROR_VIEW_IMAGE_URL_NULL)
                    }
                }
            } ?: callback.onNoGuidanceImageUrl()
        } ?: callback.onNoGuidanceImageUrl()
    }

    /**
     * The API allows you to cancel the rendering of guidance view.
     */
    fun cancelRender() {
        mainJobController.job.cancelChildren()
    }

    private suspend fun getBitmap(url: String): GuidanceViewImageResponse =
        withContext(ThreadController.IODispatcher) {
            suspendCoroutine<GuidanceViewImageResponse> {
                val req = Request.Builder().url(url).build()
                okHttpClient.newCall(req).enqueue(
                    object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            resumeCoroutine(GuidanceViewImageResponse(error = e.message))
                        }

                        override fun onResponse(call: Call, response: Response) {
                            resumeCoroutine(
                                GuidanceViewImageResponse(
                                    BitmapFactory.decodeStream(response.body()?.byteStream()),
                                    response.message()
                                )
                            )
                        }

                        private fun resumeCoroutine(result: GuidanceViewImageResponse) {
                            it.resumeWith(Result.success(result))
                        }
                    }
                )
            }
        }

    internal data class GuidanceViewImageResponse(
        val bitmap: Bitmap? = null,
        val error: String? = null
    )

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
