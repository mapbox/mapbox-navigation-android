package com.mapbox.navigation.ui.voicefeedback.internal

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.voicefeedback.view.MapboxVoiceFeedbackButton
import com.mapbox.navigation.ui.voicefeedback.view.MapboxVoiceFeedbackDialog
import com.mapbox.navigation.ui.voicefeedback.view.VoiceFeedbackViewModel
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.voicefeedback.ASRState
import com.mapbox.navigation.voicefeedback.postVoiceFeedback
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Interface for capturing map screenshots to be included with feedback.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface ScreenshotCapturer {
    /**
     * @param callback Callback to be invoked with the captured screenshot as a [Bitmap].
     */
    fun capture(callback: (Bitmap) -> Unit)
}

/**
 * UI component that orchestrates the voice feedback flow: button interaction,
 * [VoiceFeedbackViewModel] lifecycle, ASR dialog, screenshot capture, and feedback posting.
 *
 * @param button the voice feedback button view that triggers the flow
 * @param screenshotCapturer optional screenshot capturer that can be used to capture screenshots
 *   before posting feedback.
 */
@ExperimentalPreviewMapboxNavigationAPI
class VoiceFeedbackComponent(
    private val button: MapboxVoiceFeedbackButton,
    private val screenshotCapturer: ScreenshotCapturer? = null,
) : UIComponent() {

    private var mapboxNavigation: MapboxNavigation? = null
    private var viewModel: VoiceFeedbackViewModel? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation = mapboxNavigation

        val activity = button.context.findFragmentActivity() ?: run {
            logE(TAG) { "Cannot attach: host Activity is not a FragmentActivity" }
            return
        }
        val viewModel = ViewModelProvider(activity)[VoiceFeedbackViewModel::class.java]
        this.viewModel = viewModel

        coroutineScope.launch {
            viewModel.state.collect { state ->
                if (state is ASRState.Result) {
                    postFeedback(state)
                }
            }
        }

        button.setOnClickListener {
            showDialog()
            viewModel.onVoiceFeedbackButtonClicked()
        }
    }

    private fun showDialog() {
        val fragmentManager =
            button.context.findFragmentManager() ?: run {
                logE(TAG) { "Cannot update dialog: host Activity is not a FragmentActivity" }
                return
            }

        MapboxVoiceFeedbackDialog().show(fragmentManager, MapboxVoiceFeedbackDialog.TAG)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        button.setOnClickListener(null)
        this.viewModel = null
        this.mapboxNavigation = null
        super.onDetached(mapboxNavigation)
    }

    private suspend fun captureScreenshot(): Bitmap? = suspendCancellableCoroutine { cont ->
        if (screenshotCapturer != null) {
            screenshotCapturer.capture {
                if (cont.isActive) cont.resume(it)
            }
        } else {
            cont.resume(null)
        }
    }

    private suspend fun postFeedback(result: ASRState.Result) {
        val nav = mapboxNavigation ?: return
        val screenshot = captureScreenshot()?.let {
            FeedbackHelper.encodeScreenshot(it)
        } ?: ""
        nav.postVoiceFeedback(
            feedbackSubType = result.feedbackType,
            description = result.text,
            screenshot = screenshot,
            userFeedbackCallback = UserFeedbackCallback {/*no-op*/},
        )
    }

    private companion object {
        private const val TAG = "VoiceFeedbackComponent"
    }
}

private fun Context.findFragmentManager(): FragmentManager? {
    var ctx = this
    while (true) {
        if (ctx is FragmentActivity) return ctx.supportFragmentManager
        if (ctx is ContextWrapper) ctx = ctx.baseContext else return null
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (true) {
        if (ctx is FragmentActivity) return ctx
        if (ctx is ContextWrapper) ctx = ctx.baseContext else return null
    }
}
