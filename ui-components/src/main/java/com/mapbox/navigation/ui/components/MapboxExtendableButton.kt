package com.mapbox.navigation.ui.components

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.components.databinding.MapboxExtendableButtonLayoutBinding
import com.mapbox.navigation.ui.utils.internal.ExtendableButtonHelper
import com.mapbox.navigation.ui.utils.internal.extensions.measureTextWidth

/**
 * Button with an icon and expand behaviour.
 */
@UiThread
class MapboxExtendableButton : FrameLayout {

    private val binding =
        MapboxExtendableButtonLayoutBinding.inflate(
            LayoutInflater.from(context),
            this,
        )

    private val helper = ExtendableButtonHelper(
        binding.buttonText,
        { binding.iconImage.left },
        { text -> binding.buttonText.measureTextWidth(text).toInt() + binding.iconImage.left },
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
        this(
            context,
            attrs,
            defStyleAttr,
            R.style.MapboxStyleExtendableButton,
        )

    /**
     * Button with an icon and expand behaviour.
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxExtendableButton,
            defStyleAttr,
            defStyleRes,
        ).apply {
            try {
                applyAttributes(this)
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

    private fun applyAttributes(typedArray: TypedArray) {
        typedArray.getResourceId(
            R.styleable.MapboxExtendableButton_extendableButtonIcon,
            0,
        ).also {
            iconImage.setImageResource(it)
        }
        typedArray.getColorStateList(
            R.styleable.MapboxExtendableButton_extendableButtonIconTint,
        )?.also {
            iconImage.imageTintList = it
        }
        typedArray.getResourceId(
            R.styleable.MapboxExtendableButton_extendableButtonBackground,
            R.drawable.mapbox_bg_button,
        ).also {
            background = ContextCompat.getDrawable(context, it)
        }
        typedArray.getResourceId(
            R.styleable.MapboxExtendableButton_extendableButtonTextAppearance,
            R.style.MapboxExtendableButtonTextAppearance,
        ).also {
            // setTextAppearance is not deprecated in AppCompatTextView
            textView.setTextAppearance(context, it)
        }
    }

    /**
     * Value object for updating button state.
     *
     * @property icon Drawable Resource Id.
     * @property text Message text for extended button.
     * @property duration Duration in milliseconds for button extend animation.
     */
    class State(
        @DrawableRes val icon: Int,
        val text: String? = null,
        val duration: Long = 0,
    ) {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as State

            if (icon != other.icon) return false
            if (text != other.text) return false
            return duration == other.duration
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            var result = icon
            result = 31 * result + (text?.hashCode() ?: 0)
            result = 31 * result + duration.hashCode()
            return result
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "State(icon=$icon, text=$text, duration=$duration)"
        }
    }
}
