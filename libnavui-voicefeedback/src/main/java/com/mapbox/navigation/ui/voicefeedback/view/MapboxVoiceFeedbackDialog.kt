package com.mapbox.navigation.ui.voicefeedback.view

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.voicefeedback.R
import com.mapbox.navigation.ui.voicefeedback.databinding.MapboxVoiceFeedbackDialogLayoutBinding
import com.mapbox.navigation.voicefeedback.ASRState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toDrawable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest

@ExperimentalPreviewMapboxNavigationAPI
class MapboxVoiceFeedbackDialog : DialogFragment() {

    internal var asrState: StateFlow<ASRState?>? = null
    internal var onCancel: (() -> Unit)? = null
    internal var onDismiss: (() -> Unit)? = null

    private lateinit var binding: MapboxVoiceFeedbackDialogLayoutBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), theme).apply {
            window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setGravity(Gravity.BOTTOM)
                setFlags(FLAG_NOT_TOUCH_MODAL, FLAG_NOT_TOUCH_MODAL)
                clearFlags(FLAG_DIM_BEHIND)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return MapboxVoiceFeedbackDialogLayoutBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.closeButton.setOnClickListener {
            onCancel?.invoke()
            dismissAllowingStateLoss()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                asrState?.filterNotNull()?.collectLatest { state -> renderState(state) }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss?.invoke()
    }

    private suspend fun renderState(state: ASRState) {
        val showContentBlock = state is ASRState.Listening || state is ASRState.SpeechFinishedWaitingForResult
        binding.contentBlock.isVisible = showContentBlock
        binding.infoBlock.isVisible = !showContentBlock

        when (state) {
            is ASRState.Listening -> showContentBlock(
                icon = R.drawable.mapbox_ic_mic_pulsing,
                header = R.string.mapbox_voice_feedback__share_your_feedback,
                description = R.string.mapbox_voice_feedback__listening,
            )
            is ASRState.SpeechFinishedWaitingForResult -> showContentBlock(
                icon = R.drawable.mapbox_ic_spinner_anim,
                header = R.string.mapbox_voice_feedback__share_your_feedback,
                description = R.string.mapbox_voice_feedback__processing,
            )

            is ASRState.Idle,
            is ASRState.Error,
            is ASRState.Interrupted,
            is ASRState.InterruptedByTimeout -> {
                showInfoBlock(
                    icon = R.drawable.mapbox_ic_error,
                    header = R.string.mapbox_voice_feedback__error_title,
                    description = R.string.mapbox_voice_feedback__error_description,
                )
                dismissWithDelay()
            }
            is ASRState.NoResult -> {
                showInfoBlock(
                    icon = R.drawable.mapbox_ic_error,
                    header = R.string.mapbox_voice_feedback__speech_not_recognized_title,
                    description = R.string.mapbox_voice_feedback__error_description,
                )
                dismissWithDelay()
            }
            is ASRState.Result -> {
                showInfoBlock(
                    icon = R.drawable.mapbox_ic_ok_sign,
                    header = R.string.mapbox_voice_feedback__success_title,
                    description = R.string.mapbox_voice_feedback__success_description,
                )
                dismissWithDelay()
            }
        }
    }

    private fun showContentBlock(
        icon: Int,
        header: Int,
        description: Int,
    ) = with(binding) {
        contentIconImage.setImageResource(icon)
        (contentIconImage.drawable as? AnimatedVectorDrawable)?.start()
        contentHeader.setText(header)
        contentDescription.setText(description)
    }

    private fun showInfoBlock(
        icon: Int,
        header: Int,
        description: Int,
    ) = with(binding) {
        infoIcon.setImageResource(icon)
        infoHeader.setText(header)
        infoDescription.setText(description)
    }

    private suspend fun dismissWithDelay(delay: Long = AUTO_DISMISS_DELAY_MS) = coroutineScope {
        delay(delay)
        dismissAllowingStateLoss()
    }

    companion object Companion {
        internal const val TAG = "VoiceFeedbackDialog"
        private const val AUTO_DISMISS_DELAY_MS = 3_000L
    }
}
