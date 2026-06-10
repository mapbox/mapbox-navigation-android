package com.mapbox.navigation.ui.voicefeedback.internal

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.voicefeedback.view.MapboxVoiceFeedbackButton
import com.mapbox.navigation.ui.voicefeedback.view.MapboxVoiceFeedbackDialog
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.voicefeedback.ASRState
import com.mapbox.navigation.voicefeedback.FeedbackAgentSession
import com.mapbox.navigation.voicefeedback.postVoiceFeedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

fun interface ScreenshotCapturer {
    fun capture(callback: (Bitmap) -> Unit)
}

/**
 * UI component that orchestrates the voice feedback flow: button interaction,
 * [FeedbackAgentSession] lifecycle, ASR dialog, screenshot capture, and feedback posting.
 *
 * @param button the voice feedback button view that triggers the flow
 */
@ExperimentalPreviewMapboxNavigationAPI
class VoiceFeedbackComponent(
    private val button: MapboxVoiceFeedbackButton,
    private val screenshotCapturer: ScreenshotCapturer? = null,
) : UIComponent() {
    private var locationMatcherResult: LocationMatcherResult? = null

    private val session: FeedbackAgentSession =
        FeedbackAgentSession
            .Builder()
            .build()

    private val isDialogVisible = MutableStateFlow(false)

    private var mapboxNavigation: MapboxNavigation? = null
    private var dialog: MapboxVoiceFeedbackDialog? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation = mapboxNavigation
        session.onAttached(mapboxNavigation)
        session.connect()

        coroutineScope.launch {
            mapboxNavigation
                .flowLocationMatcherResult()
                .collect { locationMatcherResult = it }
        }

        coroutineScope.launch {
            session.asrState.collect { state ->
                if (state is ASRState.Result) {
                    postFeedback(state)
                }
            }
        }

        coroutineScope.launch {
            isDialogVisible.collectLatest { visible ->
                if (visible) {
                    showDialog()
                } else {
                    dismissDialog()
                }
            }
        }

        button.setOnClickListener { onButtonClicked() }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        button.setOnClickListener(null)
        dismissDialog()
        session.onDetached(mapboxNavigation)
        this.mapboxNavigation = null
        super.onDetached(mapboxNavigation)
    }

    private fun onButtonClicked() {
        session.startListening()
        isDialogVisible.value = true
    }

    private fun showDialog() {
        val fragmentManager =
            button.context.findFragmentManager() ?: run {
                logE(TAG) { "Cannot show dialog: host Activity is not a FragmentActivity" }
                return
            }

        dialog = MapboxVoiceFeedbackDialog().apply {
            asrState = session.asrState
            onCancel = { session.interruptListening() }
            onDismiss = { isDialogVisible.value = false }
            show(fragmentManager, MapboxVoiceFeedbackDialog.TAG)
        }
    }

    private fun dismissDialog() {
        dialog?.dismissAllowingStateLoss()
        dialog = null
    }

    private suspend fun captureScreenshot(): Bitmap? = suspendCancellableCoroutine { cont ->
        screenshotCapturer?.capture { cont.resume(it) } ?: cont.resume(null)
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
