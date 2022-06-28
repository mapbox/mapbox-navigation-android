package com.mapbox.navigation.ui.voice.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.utils.internal.ExtendableButtonHelper
import com.mapbox.navigation.ui.utils.internal.extensions.measureTextWidth
import com.mapbox.navigation.ui.voice.R
import com.mapbox.navigation.ui.voice.databinding.MapboxAudioGuidanceButtonLayoutBinding

/**
 * Default button that allows user to mute and un-mute audio guidance.
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapboxAudioGuidanceButton : FrameLayout {

    private val binding =
        MapboxAudioGuidanceButtonLayoutBinding.inflate(LayoutInflater.from(context), this)

    private val helper = ExtendableButtonHelper(
        binding.buttonText,
        { binding.iconImage.left },
        { text -> binding.buttonText.measureTextWidth(text).toInt() + binding.iconImage.left }
    )

    /**
     * Container view that hosts [iconImage] and [textView].
     */
    val containerView: ConstraintLayout = binding.container

    /**
     * Icon Image.
     */
    val iconImage: AppCompatImageView = binding.iconImage

    /**
     * TextView used to display text when expanded.
     */
    val textView: AppCompatTextView = binding.buttonText

    /**
     * Icon Resource Id for MUTE audio guidance.
     */
    @DrawableRes
    var muteIconResId: Int = 0

    /**
     * Icon  Drawable Resource Id for UNMUTE audio guidance.
     */
    @DrawableRes
    var unMuteIconResId: Int = 0

    /**
     * Extended mode Text for MUTE audio guidance.
     */
    var muteText: String? = null

    /**
     * Extended mode Text for UNMUTE audio guidance.
     */
    var unMuteText: String? = null

    /**
     * Default button that allows user to mute and un-mute audio guidance.
     */
    constructor(context: Context) : this(context, null)

    /**
     * Default button that allows user to mute and un-mute audio guidance.
     */
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    /**
     * Default button that allows user to mute and un-mute audio guidance.
     */
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        this(context, attrs, defStyleAttr, R.style.MapboxStyleAudioGuidanceButton)

    /**
     * Default button that allows user to mute and un-mute audio guidance.
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxAudioGuidanceButton,
            defStyleAttr,
            defStyleRes
        ).apply {
            try {
                applyAttributes(this)
            } finally {
                recycle()
            }
        }
    }

    /**
     * Update this button to represent audio guidance state in muted state.
     * This method does nothing if button is already in given state.
     */
    @UiThread
    fun mute() {
        iconImage.setImageResource(muteIconResId)
    }

    /**
     * Update this button to represent audio guidance state in un-muted state.
     * This method does nothing if button is already in given state.
     *
     * @param state new camera state
     */
    @UiThread
    fun unMute() {
        iconImage.setImageResource(unMuteIconResId)
    }

    /**
     * Update this button to represent audio guidance state in muted state.
     * Extend button for the [duration] and show [muteText].
     * This method does nothing if button is already in given state.
     *
     * @param duration duration in milliseconds. Defaults to [EXTEND_DURATION].
     */
    @UiThread
    @JvmOverloads
    fun muteAndExtend(duration: Long = EXTEND_DURATION) {
        iconImage.setImageResource(muteIconResId)

        if (muteText != null && !helper.isAnimationRunning) {
            helper.showTextAndExtend(muteText!!, duration)
        }
    }

    /**
     * Update this button to represent audio guidance state in un-muted state.
     * Extend button for the [duration] and show [unMuteText].
     * This method does nothing if button is already in given state.
     *
     * @param duration duration in milliseconds. Defaults to [EXTEND_DURATION].
     */
    @UiThread
    @JvmOverloads
    fun unMuteAndExtend(duration: Long = EXTEND_DURATION) {
        iconImage.setImageResource(unMuteIconResId)

        if (unMuteText != null && !helper.isAnimationRunning) {
            helper.showTextAndExtend(unMuteText!!, duration)
        }
    }

    /**
     * Allows you to change the style of [MapboxAudioGuidanceButton].
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        context.obtainStyledAttributes(style, R.styleable.MapboxAudioGuidanceButton).apply {
            try {
                applyAttributes(this)
            } finally {
                recycle()
            }
        }
    }

    private fun applyAttributes(typedArray: TypedArray) {
        muteIconResId = typedArray.getResourceId(
            R.styleable.MapboxAudioGuidanceButton_audioGuidanceButtonMuteIcon,
            R.drawable.mapbox_ic_sound_off
        )
        unMuteIconResId = typedArray.getResourceId(
            R.styleable.MapboxAudioGuidanceButton_audioGuidanceButtonUnmuteIcon,
            R.drawable.mapbox_ic_sound_on
        )
        typedArray.getColorStateList(
            R.styleable.MapboxAudioGuidanceButton_audioGuidanceButtonIconTint
        )?.also {
            iconImage.imageTintList = it
        }
        typedArray.getResourceId(
            R.styleable.MapboxAudioGuidanceButton_audioGuidanceButtonBackground,
            R.drawable.mapbox_bg_button
        ).also {
            background = ContextCompat.getDrawable(context, it)
        }
        typedArray.getResourceId(
            R.styleable.MapboxAudioGuidanceButton_audioGuidanceButtonTextAppearance,
            R.style.MapboxAudioGuidanceButtonTextAppearance
        ).also {
            // setTextAppearance is not deprecated in AppCompatTextView
            textView.setTextAppearance(context, it)
        }
        muteText =
            typedArray.getString(R.styleable.MapboxAudioGuidanceButton_audioGuidanceButtonMuteText)
        unMuteText =
            typedArray.getString(
                R.styleable.MapboxAudioGuidanceButton_audioGuidanceButtonUnmuteText
            )
    }

    private companion object {
        /**
         * Default extended mode duration in milliseconds (2000).
         */
        private const val EXTEND_DURATION: Long = 2000L
    }
}
