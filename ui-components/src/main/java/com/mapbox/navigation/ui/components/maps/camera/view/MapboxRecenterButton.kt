package com.mapbox.navigation.ui.components.maps.camera.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.ConstraintLayout
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.databinding.ExtendableButtonLayoutBinding
import com.mapbox.navigation.ui.utils.internal.ExtendableButtonHelper

/**
 * Default view to allow user to switch to route overview mode.
 */
@UiThread
class MapboxRecenterButton : ConstraintLayout {

    private val binding = ExtendableButtonLayoutBinding.inflate(LayoutInflater.from(context), this)
    private val helper = ExtendableButtonHelper(
        binding.buttonText,
        context.resources.getDimensionPixelSize(R.dimen.mapbox_button_size),
        context.resources.getDimension(R.dimen.mapbox_recenterButton_minExtendedWidth),
    )

    /**
     *
     * @param context Context
     * @constructor
     */
    constructor(context: Context) : super(context)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @constructor
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttributes(attrs)
    }

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @param defStyleAttr Int
     * @constructor
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr) {
        initAttributes(attrs)
    }

    /**
     * Allows you to change the style of [MapboxRecenterButton].
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        val typedArray = context.obtainStyledAttributes(
            style,
            R.styleable.MapboxRecenterButton,
        )
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    /**
     * Invoke the function to show optional text associated with the view.
     * @param duration for the view to be in the extended mode before it starts to shrink.
     * @param text for the view to show in the extended mode.
     */
    @JvmOverloads
    fun showTextAndExtend(
        duration: Long,
        text: String = context.getString(R.string.mapbox_recenter),
    ) {
        if (!helper.isAnimationRunning) {
            helper.showTextAndExtend(text, duration)
        }
    }

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxRecenterButton,
            0,
            R.style.MapboxStyleRecenter,
        )
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    private fun applyAttributes(typedArray: TypedArray) {
        typedArray.getDrawable(
            R.styleable.MapboxRecenterButton_recenterButtonDrawable,
        ).also { binding.buttonIcon.setImageDrawable(it) }

        typedArray.getDrawable(
            R.styleable.MapboxRecenterButton_recenterButtonBackground,
        )?.let { background ->
            binding.buttonIcon.background = background
            binding.buttonText.background = background
        }

        typedArray.getColorStateList(
            R.styleable.MapboxRecenterButton_recenterButtonTextColor,
        )?.let { binding.buttonText.setTextColor(it) }
    }
}
