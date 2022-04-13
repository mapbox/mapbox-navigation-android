package com.mapbox.navigation.dropin.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxExtendableButtonLayoutBinding
import com.mapbox.navigation.ui.utils.internal.ExtendableButtonHelper
import com.mapbox.navigation.ui.utils.internal.extensions.measureTextWidth

/**
 * Button with an icon and expand behaviour.
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapboxExtendableButton : FrameLayout {

    private val binding =
        MapboxExtendableButtonLayoutBinding.inflate(LayoutInflater.from(context), this)

    private val helper = ExtendableButtonHelper(
        binding.buttonText,
        { 0 },
        { text ->
            binding.buttonText.measureTextWidth(text).toInt() +
                resources.getDimensionPixelSize(R.dimen.mapbox_extendableButton_paddingStart)
        },
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
     * Button with an icon and expand behaviour.
     */
    constructor(context: Context) : this(context, null)

    /**
     * Button with an icon and expand behaviour.
     */
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    /**
     * Button with an icon and expand behaviour.
     */
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        this(context, attrs, defStyleAttr, R.style.MapboxStyleExtendableButton)

    /**
     * Button with an icon and expand behaviour.
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxExtendableButton,
            defStyleAttr,
            defStyleRes
        ).apply {
            try {
                getResourceId(
                    R.styleable.MapboxExtendableButton_extendableButtonIcon,
                    0
                ).also {
                    iconImage.setImageResource(it)
                }
                getColorStateList(
                    R.styleable.MapboxExtendableButton_extendableButtonIconTint
                )?.also {
                    iconImage.imageTintList = it
                }
                getResourceId(
                    R.styleable.MapboxExtendableButton_extendableButtonTextAppearance,
                    R.style.MapboxExtendableButtonTextAppearance
                ).also {
                    // setTextAppearance is not deprecated in AppCompatTextView
                    textView.setTextAppearance(context, it)
                }
            } finally {
                recycle()
            }
        }
    }

    /**
     * Update this button [iconImage] drawable to [State.icon].
     *
     * Passing a state with non-empty [State.text] and [State.duration] > 0 will
     * extend the button with animation, show [State.text] message and shrink
     * after [State.duration].
     *
     * @param state [State] with new icon and text.
     */
    fun setState(state: State) {
        iconImage.setImageResource(state.icon)

        if (0 < state.duration && !state.text.isNullOrEmpty() && !helper.isAnimationRunning) {
            helper.showTextAndExtend(state.text, state.duration)
        }
    }

    /**
     * Value object for updating button state.
     *
     * @property icon Drawable Resource Id.
     * @property text Message text for extended button.
     * @property duration Duration in milliseconds for button extend animation.
     */
    data class State(
        @DrawableRes val icon: Int,
        val text: String? = null,
        val duration: Long = 0
    )
}
