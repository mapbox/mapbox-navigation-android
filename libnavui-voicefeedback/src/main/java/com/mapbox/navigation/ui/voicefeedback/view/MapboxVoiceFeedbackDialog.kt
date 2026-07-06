package com.mapbox.navigation.ui.voicefeedback.view

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.voicefeedback.R
import com.mapbox.navigation.ui.voicefeedback.databinding.MapboxVoiceFeedbackDialogLayoutBinding
import com.mapbox.navigation.voicefeedback.ASRState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
class MapboxVoiceFeedbackDialog : DialogFragment() {

    private lateinit var binding: MapboxVoiceFeedbackDialogLayoutBinding
    private lateinit var viewModel: VoiceFeedbackViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), theme)
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

        viewModel = ViewModelProvider(requireActivity())[VoiceFeedbackViewModel::class.java]

        dialog?.window?.apply {
            val width = if (resources.isTablet() || resources.configuration.isLandscape()) {
                ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setGravity(Gravity.BOTTOM)
            setFlags(FLAG_NOT_TOUCH_MODAL, FLAG_NOT_TOUCH_MODAL)
            clearFlags(FLAG_DIM_BEHIND)
        }

        binding.closeButton.setOnClickListener {
            dismissAllowingStateLoss()
            viewModel.onCloseDialogButtonClicked()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                    viewModel.state.collectLatest { state -> renderState(state) }
                }
            }
        }

        viewModel.onDialogVisible()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onDialogDismissed()
    }

    private suspend fun renderState(state: ASRState?) {
        when (state) {
            ASRState.Idle, null -> showContentBlock(
                icon = R.drawable.mapbox_ic_spinner_anim,
                header = R.string.mapbox_voice_feedback__share_your_feedback,
                description = R.string.mapbox_voice_feedback__connecting,
            )
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
        binding.contentBlock.isVisible = true
        binding.infoBlock.isVisible = false
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
        binding.contentBlock.isVisible = false
        binding.infoBlock.isVisible = true
        infoIcon.setImageResource(icon)
        infoHeader.setText(header)
        infoDescription.setText(description)
    }

    private suspend fun dismissWithDelay(delay: Long = AUTO_DISMISS_DELAY_MS) {
        delay(delay)
        dismissAllowingStateLoss()
    }

    companion object Companion {
        internal const val TAG = "VoiceFeedbackDialog"
        private const val AUTO_DISMISS_DELAY_MS = 3_000L
    }
}

private fun Resources.isTablet(): Boolean = getBoolean(R.bool.is_tablet)

private fun Configuration.isLandscape(): Boolean = orientation ==
    Configuration.ORIENTATION_LANDSCAPE
