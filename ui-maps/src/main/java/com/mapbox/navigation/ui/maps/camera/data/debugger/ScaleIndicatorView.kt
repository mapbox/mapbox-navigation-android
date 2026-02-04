package com.mapbox.navigation.ui.maps.camera.data.debugger

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.turf.TurfConstants.UNIT_METRES
import com.mapbox.turf.TurfMeasurement

/**
 * A custom View that displays a scale indicator with distance text in both metric and imperial formats.
 * Used by [MapboxNavigationViewportDataSourceDebugger] to show the map scale.
 */
internal class ScaleIndicatorView(context: Context) : LinearLayout(context) {

    private val density = context.resources.displayMetrics.density
    private val scaleLineLengthPx = (SCALE_LINE_LENGTH_DP * density).toInt()

    private val textView = TextView(context).apply {
        setTextColor(Color.GREEN)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP)
        gravity = Gravity.CENTER
    }

    private val lineView = View(context).apply {
        setBackgroundColor(Color.GREEN)
    }

    private var scaleLineDistanceMeters = 0.0
        set(value) {
            field = value
            updateText()
        }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundColor(Color.argb(BG_ALPHA, 0, 0, 0))

        val padding = (PADDING_DP * density).toInt()
        setPadding(padding, padding, padding, padding)

        val lineStrokeHeight = (LINE_STROKE_HEIGHT_DP * density).toInt()

        // FrameLayout.LayoutParams for positioning within MapView
        layoutParams = FrameLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        addView(
            textView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )

        addView(
            lineView,
            LayoutParams(scaleLineLengthPx, lineStrokeHeight).apply {
                topMargin = padding
            },
        )
    }

    /**
     * Updates the scale indicator based on the actual position of the line on the map.
     * The distance is calculated from where the line endpoints appear on the map.
     */
    fun update(mapboxMap: MapboxMap, mapView: View) {
        // Get position of lineView relative to mapView
        val lineLocation = IntArray(2)
        val mapLocation = IntArray(2)
        lineView.getLocationOnScreen(lineLocation)
        mapView.getLocationOnScreen(mapLocation)

        val offsetX = lineLocation[0] - mapLocation[0]
        val offsetY = lineLocation[1] - mapLocation[1]

        // Line endpoints in mapView coordinates
        val leftScreenX = offsetX.toDouble()
        val rightScreenX = offsetX.toDouble() + lineView.width
        val screenY = offsetY.toDouble() + lineView.height / 2.0

        val leftPoint = mapboxMap.coordinateForPixel(
            ScreenCoordinate(leftScreenX, screenY),
        )
        val rightPoint = mapboxMap.coordinateForPixel(
            ScreenCoordinate(rightScreenX, screenY),
        )
        scaleLineDistanceMeters = TurfMeasurement.distance(leftPoint, rightPoint, UNIT_METRES)
    }

    @SuppressLint("SetTextI18n")
    private fun updateText() {
        val metricText = formatMetricDistance(scaleLineDistanceMeters)
        val imperialText = formatImperialDistance(scaleLineDistanceMeters)
        textView.text = "$metricText | $imperialText"
    }

    private fun formatMetricDistance(meters: Double): String {
        return if (meters >= METERS_PER_KM) {
            "%.2f km".format(meters / METERS_PER_KM)
        } else {
            "%.1f m".format(meters)
        }
    }

    private fun formatImperialDistance(meters: Double): String {
        val feet = meters * FEET_PER_METER
        return if (feet >= FEET_THRESHOLD_FOR_MILES) {
            val miles = feet / FEET_PER_MILE
            "%.2f mi".format(miles)
        } else {
            "%.0f ft".format(feet)
        }
    }

    internal companion object {
        const val SCALE_LINE_LENGTH_DP = 100f
        const val LINE_STROKE_HEIGHT_DP = 3f
        const val TEXT_SIZE_SP = 14f
        const val PADDING_DP = 8f
        const val BG_ALPHA = 180
        const val METERS_PER_KM = 1000
        const val FEET_PER_METER = 3.28084
        const val FEET_PER_MILE = 5280
        const val FEET_THRESHOLD_FOR_MILES = 1000
    }
}
