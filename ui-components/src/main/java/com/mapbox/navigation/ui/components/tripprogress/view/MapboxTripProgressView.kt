package com.mapbox.navigation.ui.components.tripprogress.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.tripdata.progress.model.TripOverviewError
import com.mapbox.navigation.tripdata.progress.model.TripOverviewValue
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateValue
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.databinding.MapboxTripProgressLayoutBinding
import com.mapbox.navigation.ui.components.tripprogress.model.TripProgressViewOptions
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * A view that can be added to activity layouts which displays trip progress.
 */
@UiThread
class MapboxTripProgressView : FrameLayout {

    /**
     *
     * @param context Context
     * @constructor
     */
    @JvmOverloads
    constructor(
        context: Context,
        options: TripProgressViewOptions = TripProgressViewOptions.Builder().build(),
    ) : super(context) {
        updateOptions(options)
    }

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @constructor
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
    ) : this(context, attrs, 0)

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
        updateStyle(defStyleAttr)
    }

    private val binding =
        MapboxTripProgressLayoutBinding.inflate(
            LayoutInflater.from(context),
            this,
        )

    /**
     * Invoke the method to change the styling of various trip progress components at runtime.
     *
     * @param options TripProgressViewOptions
     */
    fun updateOptions(options: TripProgressViewOptions) {
        TextViewCompat.setTextAppearance(
            binding.timeRemainingText,
            options.timeRemainingTextAppearance,
        )
        TextViewCompat.setTextAppearance(
            binding.distanceRemainingText,
            options.distanceRemainingTextAppearance,
        )
        TextViewCompat.setTextAppearance(
            binding.estimatedTimeToArriveText,
            options.estimatedArrivalTimeTextAppearance,
        )
        binding.distanceRemainingIcon.setImageResource(options.distanceRemainingIcon)
        binding.estimatedTimeToArriveIcon.setImageResource(options.estimatedArrivalTimeIcon)
        options.distanceRemainingIconTint?.let { tint ->
            binding.distanceRemainingIcon.imageTintList = tint
        }
        options.estimatedArrivalTimeIconTint?.let { tint ->
            binding.estimatedTimeToArriveIcon.imageTintList = tint
        }
        setBackgroundColor(ContextCompat.getColor(context, options.backgroundColor))
    }

    /**
     * Applies the necessary view side effects based on the input.
     *
     * @param result a [TripProgressUpdateValue] containing the data that should be rendered.
     */
    fun render(result: TripProgressUpdateValue) {
        binding.timeRemainingText.renderTimeRemaining(
            result.formatter.getTimeRemaining(result.currentLegTimeRemaining),
            TextView.BufferType.SPANNABLE,
        )

        binding.distanceRemainingText.renderDistanceRemaining(
            result.formatter.getDistanceRemaining(result.distanceRemaining),
            TextView.BufferType.SPANNABLE,
        )

        binding.estimatedTimeToArriveText.renderEstimatedArrivalTime(
            result.formatter.getEstimatedTimeToArrival(
                result.estimatedTimeToArrival,
            ),
            TextView.BufferType.SPANNABLE,
        )
    }

    /**
     * Draws `totalTime`, totalDistance` and `totalEstimatedTimeToArrival` based on the total time
     * of the route (including all upcoming legs).
     *
     * @param result a [TripOverviewValue] containing the data that should be rendered.
     */
    fun renderTripOverview(result: Expected<TripOverviewError, TripOverviewValue>) {
        result.onValue { tripOverview ->
            binding.timeRemainingText.renderTimeRemaining(
                tripOverview.formatter.getTimeRemaining(tripOverview.totalTime),
                TextView.BufferType.SPANNABLE,
            )

            binding.distanceRemainingText.renderDistanceRemaining(
                tripOverview.formatter.getDistanceRemaining(tripOverview.totalDistance),
                TextView.BufferType.SPANNABLE,
            )

            binding.estimatedTimeToArriveText.renderEstimatedArrivalTime(
                tripOverview.formatter.getEstimatedTimeToArrival(
                    tripOverview.totalEstimatedTimeToArrival,
                ),
                TextView.BufferType.SPANNABLE,
            )
        }
    }

    /**
     * Draws `legTime`, legDistance` and `estimatedTimeToArrival` based on the [RouteLeg] passed
     * to the API.
     *
     * @param legIndex index of the [RouteLeg] for which to display the trip overview
     * @param result a [TripOverviewValue] containing the data that should be rendered.
     */
    fun renderLegOverview(legIndex: Int, result: Expected<TripOverviewError, TripOverviewValue>) {
        result.onValue { tripOverview ->
            val legOverview = tripOverview.routeLegTripDetail.find { it.legIndex == legIndex }
            ifNonNull(legOverview) { overview ->
                binding.timeRemainingText.renderTimeRemaining(
                    tripOverview.formatter.getTimeRemaining(overview.legTime),
                    TextView.BufferType.SPANNABLE,
                )

                binding.distanceRemainingText.renderDistanceRemaining(
                    tripOverview.formatter.getDistanceRemaining(overview.legDistance),
                    TextView.BufferType.SPANNABLE,
                )

                binding.estimatedTimeToArriveText.renderEstimatedArrivalTime(
                    tripOverview.formatter.getEstimatedTimeToArrival(
                        overview.estimatedTimeToArrival,
                    ),
                    TextView.BufferType.SPANNABLE,
                )
            }
        }
    }

    /**
     * Allows you to change the style of [MapboxTripProgressView].
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        context.obtainStyledAttributes(style, R.styleable.MapboxTripProgressView).apply {
            try {
                val options = TripProgressViewOptions.Builder()
                getResourceId(
                    R.styleable.MapboxTripProgressView_timeRemainingTextAppearance,
                    R.style.MapboxStyleTimeRemaining,
                ).also {
                    options.timeRemainingTextAppearance(it)
                }
                getResourceId(
                    R.styleable.MapboxTripProgressView_distanceRemainingTextAppearance,
                    R.style.MapboxStyleDistanceRemaining,
                ).also {
                    options.distanceRemainingTextAppearance(it)
                }
                getResourceId(
                    R.styleable.MapboxTripProgressView_estimatedArrivalTimeTextAppearance,
                    R.style.MapboxStyleEstimatedArrivalTime,
                ).also {
                    options.estimatedArrivalTimeTextAppearance(it)
                }
                getResourceId(
                    R.styleable.MapboxTripProgressView_distanceRemainingIcon,
                    R.drawable.mapbox_ic_pin,
                ).also {
                    options.distanceRemainingIcon(it)
                }
                getResourceId(
                    R.styleable.MapboxTripProgressView_estimatedArrivalTimeIcon,
                    R.drawable.mapbox_ic_time,
                ).also {
                    options.estimatedArrivalTimeIcon(it)
                }
                getColorStateList(
                    R.styleable.MapboxTripProgressView_distanceRemainingIconTint,
                )?.also {
                    options.distanceRemainingIconTint(it)
                }
                getColorStateList(
                    R.styleable.MapboxTripProgressView_estimatedArrivalTimeIconTint,
                )?.also {
                    options.estimatedArrivalTimeIconTint(it)
                }
                getResourceId(
                    R.styleable.MapboxTripProgressView_tripProgressViewBackgroundColor,
                    R.color.mapbox_trip_progress_view_background_color,
                ).also {
                    options.backgroundColor(it)
                }
                updateOptions(options.build())
            } finally {
                recycle()
            }
        }
    }
}
