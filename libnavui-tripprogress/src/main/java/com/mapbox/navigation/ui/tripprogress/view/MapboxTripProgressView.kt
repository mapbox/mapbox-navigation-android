package com.mapbox.navigation.ui.tripprogress.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.tripprogress.TripProgressState
import com.mapbox.navigation.ui.tripprogress.R

/**
 * A view that can be added to activity layouts which displays trip progress.
 */
class MapboxTripProgressView : FrameLayout, MapboxView<TripProgressState> {

    private var viewDistanceRemaining: TextView? = null
    private var viewEstimatedTimeToArrive: TextView? = null
    private var viewTimeRemaining: TextView? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initAttributes(attrs)
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.mapbox_trip_progress_layout, this, true)
    }

    private fun initAttributes(attrs: AttributeSet?) {
        viewDistanceRemaining = findViewById(R.id.txtMapboxTripProgressDistanceRemaining)
        viewEstimatedTimeToArrive = findViewById(R.id.txtMapboxTripProgressEstimatedTimeToArrive)
        viewTimeRemaining = findViewById(R.id.txtMapboxTripProgressTimeRemaining)

        val typedArray: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxTripProgressView
        )

        val primaryTextColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxTripProgressView_tripProgressPrimaryTextColor,
                R.color.mapbox_trip_progress_primary_text_color
            )
        )
        viewDistanceRemaining?.setTextColor(primaryTextColor)
        viewEstimatedTimeToArrive?.setTextColor(primaryTextColor)
        viewTimeRemaining?.setTextColor(primaryTextColor)

        val dividerColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxTripProgressView_tripProgressDividerColor,
                R.color.mapbox_trip_progress_divider_color
            )
        )
        findViewById<View>(R.id.mapboxTripProgressDivider)?.setBackgroundColor(dividerColor)
        findViewById<TextView>(R.id.mapboxTripProgressDividerLeft)?.setTextColor(dividerColor)
        findViewById<TextView>(R.id.mapboxTripProgressDividerRight)?.setTextColor(dividerColor)

        val backgroundColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxTripProgressView_tripProgressViewBackgroundColor,
                R.color.mapbox_trip_progress_background_color
            )
        )
        this.setBackgroundColor(backgroundColor)

        typedArray.recycle()
    }

    /**
     * Applies the necessary view side effects based on the input.
     *
     * @param state a [TripProgressState] containing the data that should be rendered.
     */
    override fun render(state: TripProgressState) {
        when (state) {
            is TripProgressState.Update -> renderUpdate(state)
        }
    }

    private fun renderUpdate(state: TripProgressState.Update) {
        viewDistanceRemaining?.setText(
            state.formatter.getDistanceRemaining(state.tripProgressUpdate),
            TextView.BufferType.SPANNABLE
        )

        viewEstimatedTimeToArrive?.setText(
            state.formatter.getEstimatedTimeToArrival(state.tripProgressUpdate),
            TextView.BufferType.SPANNABLE
        )

        viewTimeRemaining?.setText(
            state.formatter.getTimeRemaining(state.tripProgressUpdate),
            TextView.BufferType.SPANNABLE
        )
    }
}
