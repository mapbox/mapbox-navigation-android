package com.mapbox.navigation.ui.tripprogress.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.tripprogress.R
import com.mapbox.navigation.ui.tripprogress.databinding.MapboxTripProgressLayoutBinding
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateError
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateValue

/**
 * A view that can be added to activity layouts which displays trip progress.
 */
class MapboxTripProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding =
        MapboxTripProgressLayoutBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )

    init {
        initAttributes(attrs)
    }

    /**
     * Allows you to change the style of [MapboxTripProgressView].
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        val typedArray = context.obtainStyledAttributes(style, R.styleable.MapboxTripProgressView)
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MapboxTripProgressView)
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    private fun applyAttributes(typedArray: TypedArray) {
        val textColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxTripProgressView_tripProgressTextColor,
                R.color.mapbox_trip_progress_text_color
            )
        )
        binding.timeRemainingText.setTextColor(textColor)
        binding.distanceRemainingText.setTextColor(textColor)
        binding.estimatedTimeToArriveText.setTextColor(textColor)

        val dividerColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxTripProgressView_tripProgressDividerColor,
                R.color.mapbox_trip_progress_divider_color
            )
        )
        binding.tripProgressDivider?.setBackgroundColor(dividerColor)
        binding.tripProgressDividerLeft?.setBackgroundColor(dividerColor)
        binding.tripProgressDividerRight?.setBackgroundColor(dividerColor)

        setBackgroundColor(
            ContextCompat.getColor(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxTripProgressView_tripProgressViewBackgroundColor,
                    R.color.mapbox_trip_progress_view_background_color
                )
            )
        )
    }

    /**
     * Applies the necessary view side effects based on the input.
     *
     * @param result a [Expected<TripProgressUpdateValue, TripProgressUpdateError>]
     * containing the data that should be rendered.
     */
    fun render(result: Expected<TripProgressUpdateValue, TripProgressUpdateError>) {
        when (result) {
            is Expected.Success<TripProgressUpdateValue> -> {
                binding.distanceRemainingText.setText(
                    result.value.formatter.getDistanceRemaining(result.value.distanceRemaining),
                    TextView.BufferType.SPANNABLE
                )

                binding.estimatedTimeToArriveText.setText(
                    result.value.formatter.getEstimatedTimeToArrival(
                        result.value.estimatedTimeToArrival
                    ),
                    TextView.BufferType.SPANNABLE
                )

                binding.timeRemainingText.setText(
                    result.value.formatter.getTimeRemaining(result.value.currentLegTimeRemaining),
                    TextView.BufferType.SPANNABLE
                )
            }
            is Expected.Failure<TripProgressUpdateError> -> { }
        }
    }
}
