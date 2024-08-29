package com.mapbox.navigation.ui.components.maps.view

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
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.databinding.MapboxCameraModeButtonLayoutBinding
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.utils.internal.ExtendableButtonHelper
import com.mapbox.navigation.ui.utils.internal.extensions.measureTextWidth

/**
 * Default button that allows user to toggle between Camera Following and Overview mode.
 */
@UiThread
class MapboxCameraModeButton : FrameLayout {

    private val binding =
        MapboxCameraModeButtonLayoutBinding.inflate(LayoutInflater.from(context), this)

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
     * Icon Resource Id for FOLLOWING camera state.
     */
    @DrawableRes
    var followingIconResId: Int = 0

    /**
     * Icon  Drawable Resource Id for OVERVIEW camera state.
     */
    @DrawableRes
    var overviewIconResId: Int = 0

    /**
     * Extended mode Text for FOLLOWING camera state.
     */
    var followingText: String? = null

    /**
     * Extended mode Text for OVERVIEW camera state.
     */
    var overviewText: String? = null

    /**
     * Default button that allows user to toggle between Camera Following and Overview mode.
     */
    constructor(context: Context) : this(context, null)

    /**
     * Default button that allows user to toggle between Camera Following and Overview mode.
     */
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    /**
     * Default button that allows user to toggle between Camera Following and Overview mode.
     */
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        this(context, attrs, defStyleAttr, R.style.MapboxStyleCameraModeButton)

    /**
     * Default button that allows user to toggle between Camera Following and Overview mode.
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxCameraModeButton,
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
     * Update this button to represent new camera state.
     * This method does nothing if button is already in given state.
     *
     * @param state new camera state
     */
    fun setState(state: NavigationCameraState) {
        updateIconDrawable(state)
    }

    /**
     * Update this button to represent new camera state.
     * Extend button for the [duration] and show [followingText] or [overviewText].
     * This method does nothing if button is already in given state.
     *
     * @param state new camera state.
     * @param duration duration in milliseconds. Defaults to [EXTEND_DURATION].
     */
    @JvmOverloads
    fun setStateAndExtend(state: NavigationCameraState, duration: Long = EXTEND_DURATION) {
        updateIconDrawable(state)

        val text = getText(state)
        if (text != null && !helper.isAnimationRunning) {
            helper.showTextAndExtend(text, duration)
        }
    }

    /**
     * Allows you to change the style of [MapboxCameraModeButton].
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        context.obtainStyledAttributes(style, R.styleable.MapboxCameraModeButton).apply {
            try {
                applyAttributes(this)
            } finally {
                recycle()
            }
        }
    }

    private fun applyAttributes(typedArray: TypedArray) {
        followingIconResId = typedArray.getResourceId(
            R.styleable.MapboxCameraModeButton_cameraModeButtonFollowIcon,
            R.drawable.mapbox_ic_camera_follow,
        )
        overviewIconResId = typedArray.getResourceId(
            R.styleable.MapboxCameraModeButton_cameraModeButtonOverviewIcon,
            R.drawable.mapbox_ic_camera_overview,
        )
        typedArray.getColorStateList(
            R.styleable.MapboxCameraModeButton_cameraModeButtonIconTint,
        )?.also {
            iconImage.imageTintList = it
        }
        typedArray.getResourceId(
            R.styleable.MapboxCameraModeButton_cameraModeButtonBackground,
            R.drawable.mapbox_bg_button,
        ).also {
            background = ContextCompat.getDrawable(context, it)
        }
        typedArray.getResourceId(
            R.styleable.MapboxCameraModeButton_cameraModeButtonTextAppearance,
            R.style.MapboxCameraModeButtonTextAppearance,
        ).also {
            // setTextAppearance is not deprecated in AppCompatTextView
            textView.setTextAppearance(context, it)
        }
        followingText =
            typedArray.getString(R.styleable.MapboxCameraModeButton_cameraModeButtonFollowText)
        overviewText =
            typedArray.getString(R.styleable.MapboxCameraModeButton_cameraModeButtonOverviewText)
    }

    private fun updateIconDrawable(state: NavigationCameraState) {
        if (isInFollowMode(state)) {
            iconImage.setImageResource(overviewIconResId)
        } else {
            iconImage.setImageResource(followingIconResId)
        }
    }

    private fun getText(state: NavigationCameraState): String? =
        if (isInFollowMode(state)) overviewText else followingText

    private fun isInFollowMode(state: NavigationCameraState) =
        state == NavigationCameraState.TRANSITION_TO_FOLLOWING ||
            state == NavigationCameraState.FOLLOWING

    private companion object {
        /**
         * Default extended mode duration in milliseconds (2000).
         */
        private const val EXTEND_DURATION: Long = 2000L
    }
}
