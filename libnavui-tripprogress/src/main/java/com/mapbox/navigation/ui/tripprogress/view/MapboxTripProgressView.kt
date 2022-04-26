package com.mapbox.navigation.ui.tripprogress.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.tripprogress.R
import com.mapbox.navigation.ui.tripprogress.databinding.MapboxTripProgressLayoutBinding
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateValue

/**
 * A view that can be added to activity layouts which displays trip progress.
 */
class MapboxTripProgressView : FrameLayout {

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

    private val binding =
        MapboxTripProgressLayoutBinding.inflate(
            LayoutInflater.from(context),
            this
        )

    /**
     * [TimeRemainingView] to render time remaining to reach the destination
     */
    val timeRemainingView: TimeRemainingView = binding.timeRemainingText
    /**
     * [TimeRemainingView] to render distance remaining to reach the destination
     */
    val distanceRemainingView: DistanceRemainingView = binding.distanceRemainingText
    /**
     * [TimeRemainingView] to render estimated arrival time to reach the destination
     */
    val estimatedArrivalTimeView: EstimatedArrivalTimeView = binding.estimatedTimeToArriveText

    /**
     * Allows you to change the style of [MapboxTripProgressView].
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        context.obtainStyledAttributes(style, R.styleable.MapboxTripProgressView).apply {
            try {
                applyAttributes(this)
            } finally {
                recycle()
            }
        }
    }

    /**
     * Applies the necessary view side effects based on the input.
     *
     * @param result a [TripProgressUpdateValue] containing the data that should be rendered.
     */
    fun render(result: TripProgressUpdateValue) {
        distanceRemainingView.renderDistanceRemaining(
            result.formatter.getDistanceRemaining(result.distanceRemaining),
            TextView.BufferType.SPANNABLE
        )

        estimatedArrivalTimeView.renderEstimatedArrivalTime(
            result.formatter.getEstimatedTimeToArrival(
                result.estimatedTimeToArrival
            ),
            TextView.BufferType.SPANNABLE
        )

        timeRemainingView.renderTimeRemaining(
            result.formatter.getTimeRemaining(result.currentLegTimeRemaining),
            TextView.BufferType.SPANNABLE
        )
    }

    private fun initAttributes(attrs: AttributeSet?) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxTripProgressView,
            0,
            R.style.MapboxStyleTripProgressView
        ).apply {
            try {
                applyAttributes(this)
            } finally {
                recycle()
            }
        }
    }

    private fun applyAttributes(typedArray: TypedArray) {

        typedArray.getResourceId(
            R.styleable.MapboxTripProgressView_timeRemainingTextAppearance,
            R.style.MapboxStyleTimeRemaining
        ).also {
            // setTextAppearance is not deprecated in AppCompatTextView
            timeRemainingView.setTextAppearance(context, it)
        }
        typedArray.getResourceId(
            R.styleable.MapboxTripProgressView_distanceRemainingTextAppearance,
            R.style.MapboxStyleDistanceRemaining
        ).also {
            // setTextAppearance is not deprecated in AppCompatTextView
            distanceRemainingView.setTextAppearance(context, it)
        }
        typedArray.getResourceId(
            R.styleable.MapboxTripProgressView_estimatedArrivalTimeTextAppearance,
            R.style.MapboxStyleEstimatedArrivalTime
        ).also {
            // setTextAppearance is not deprecated in AppCompatTextView
            estimatedArrivalTimeView.setTextAppearance(context, it)
        }
        typedArray.getResourceId(
            R.styleable.MapboxTripProgressView_distanceRemainingIcon,
            R.drawable.mapbox_ic_pin
        ).also {
            binding.distanceRemainingIcon.setImageResource(it)
        }
        typedArray.getResourceId(
            R.styleable.MapboxTripProgressView_estimatedArrivalTimeIcon,
            R.drawable.mapbox_ic_time
        ).also {
            binding.estimatedTimeToArriveIcon.setImageResource(it)
        }
        typedArray.getColorStateList(
            R.styleable.MapboxTripProgressView_distanceRemainingIconTint,
        )?.also {
            binding.distanceRemainingIcon.imageTintList = it
        }
        typedArray.getColorStateList(
            R.styleable.MapboxTripProgressView_estimatedArrivalTimeIconTint
        )?.also {
            binding.estimatedTimeToArriveIcon.imageTintList = it
        }
        typedArray.getColor(
            R.styleable.MapboxTripProgressView_tripProgressViewBackgroundColor,
            ContextCompat.getColor(
                context,
                R.color.mapbox_trip_progress_view_background_color
            )
        ).also {
            setBackgroundColor(it)
        }
    }
}
