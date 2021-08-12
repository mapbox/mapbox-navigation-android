package com.mapbox.navigation.ui.maps.camera.view

import android.content.Context
import android.content.res.TypedArray
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.databinding.MapboxRouteOverviewLayoutBinding
import com.mapbox.navigation.ui.utils.internal.extensions.afterMeasured
import com.mapbox.navigation.ui.utils.internal.extensions.measureTextWidth
import com.mapbox.navigation.ui.utils.internal.extensions.play
import com.mapbox.navigation.ui.utils.internal.extensions.slideWidth

/**
 * Default view to allow user to switch to route overview mode.
 */
class MapboxRouteOverviewButton : ConstraintLayout {

    private var shrunkWidth = 0
    private var isAnimationRunning = false
    private val binding = MapboxRouteOverviewLayoutBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    private val mainHandler = Handler(Looper.getMainLooper())

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
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initAttributes(attrs)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding.routeOverviewText.afterMeasured {
            shrunkWidth = width
        }
    }

    /**
     * Allows you to change the style of [MapboxRouteOverviewButton].
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        val typedArray = context.obtainStyledAttributes(
            style,
            R.styleable.MapboxRouteOverviewButton
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
        text: String = context.getString(R.string.mapbox_route_overview),
    ) {
        if (!isAnimationRunning) {
            isAnimationRunning = true
            val extendedWidth = (binding.routeOverviewText.measureTextWidth(text) + shrunkWidth)
                .coerceAtLeast(MIN_EXTENDED_WIDTH * context.resources.displayMetrics.density)
            getAnimator(shrunkWidth, extendedWidth.toInt()).play(
                doOnStart = {
                    binding.routeOverviewText.text = text
                    binding.routeOverviewText.visibility = View.VISIBLE
                    mainHandler.postDelayed(
                        {
                            getAnimator(extendedWidth.toInt(), shrunkWidth).play(
                                doOnStart = {
                                    binding.routeOverviewText.text = null
                                },
                                doOnEnd = {
                                    binding.routeOverviewText.visibility = View.INVISIBLE
                                    isAnimationRunning = false
                                }
                            )
                        },
                        duration
                    )
                }
            )
        }
    }

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxRouteOverviewButton,
            0,
            R.style.MapboxStyleRouteOverview
        )
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    private fun applyAttributes(typedArray: TypedArray) {
        typedArray.getDrawable(
            R.styleable.MapboxRouteOverviewButton_overviewButtonDrawable
        ).also { binding.routeOverviewIcon.setImageDrawable(it) }

        typedArray.getDrawable(
            R.styleable.MapboxRouteOverviewButton_overviewButtonBackground,
        )?.let { background ->
            binding.routeOverviewIcon.background = background
            binding.routeOverviewText.background = background
        }

        typedArray.getColorStateList(
            R.styleable.MapboxRouteOverviewButton_overviewButtonTextColor,
        )?.let { binding.routeOverviewText.setTextColor(it) }
    }

    private fun getAnimator(from: Int, to: Int) =
        binding.routeOverviewText.slideWidth(from, to, SLIDE_DURATION)

    private companion object {
        private const val SLIDE_DURATION = 300L
        private const val MIN_EXTENDED_WIDTH = 165
    }
}
