package com.mapbox.navigation.dropin.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxCameraModeButtonLayoutBinding
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.utils.internal.ExtendableButtonHelper
import com.mapbox.navigation.ui.utils.internal.extensions.measureTextWidth

/**
 * Default button that allows user to toggle between Camera Following and Overview mode.
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapboxCameraModeButton : FrameLayout {

    private val binding =
        MapboxCameraModeButtonLayoutBinding.inflate(LayoutInflater.from(context), this)

    private val helper = ExtendableButtonHelper(
        binding.buttonText,
        { 0 },
        { text ->
            binding.buttonText.measureTextWidth(text).toInt() +
                resources.getDimensionPixelSize(R.dimen.mapbox_cameraModeButton_paddingStart)
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
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxCameraModeButton,
            defStyleAttr,
            defStyleRes
        ).apply {
            try {
                followingIconResId = getResourceId(
                    R.styleable.MapboxCameraModeButton_cameraModeButtonFollowIcon,
                    R.drawable.mapbox_ic_camera_follow
                )
                overviewIconResId = getResourceId(
                    R.styleable.MapboxCameraModeButton_cameraModeButtonOverviewIcon,
                    R.drawable.mapbox_ic_camera_overview
                )
                getColorStateList(
                    R.styleable.MapboxCameraModeButton_cameraModeButtonIconTint
                )?.also {
                    iconImage.imageTintList = it
                }
                getResourceId(
                    R.styleable.MapboxCameraModeButton_cameraModeButtonTextAppearance,
                    R.style.MapboxCameraModeButtonTextAppearance
                ).also {
                    // setTextAppearance is not deprecated in AppCompatTextView
                    textView.setTextAppearance(context, it)
                }
                followingText =
                    getString(R.styleable.MapboxCameraModeButton_cameraModeButtonFollowText)
                overviewText =
                    getString(R.styleable.MapboxCameraModeButton_cameraModeButtonOverviewText)
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
    @UiThread
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
    @UiThread
    @JvmOverloads
    fun setStateAndExtend(state: NavigationCameraState, duration: Long = EXTEND_DURATION) {
        updateIconDrawable(state)

        val text = getText(state)
        if (text != null && !helper.isAnimationRunning) {
            helper.showTextAndExtend(text, duration)
        }
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
