package com.mapbox.navigation.qa_test_app.view.customnavview

import android.graphics.Color
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Locale

class CustomSpeedLimitViewBinder : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val textView = TextView(viewGroup.context)
        textView.setBackgroundColor(Color.RED)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30.0f)
        TransitionManager.beginDelayedTransition(viewGroup, Fade())
        viewGroup.removeAllViews()
        viewGroup.addView(textView)
        return navigationCoroutine { mapboxNavigation ->
            mapboxNavigation.flowLocationMatcherResult().collect {
                val unit = it.speedLimitInfo.unit
                val unitString = when (unit) {
                    SpeedUnit.KILOMETERS_PER_HOUR -> "kmph"
                    SpeedUnit.METERS_PER_SECOND -> "mps"
                    SpeedUnit.MILES_PER_HOUR -> "mph"
                }
                val currentSpeedMs = it.enhancedLocation.speed
                val currentSpeed = when (unit) {
                    SpeedUnit.KILOMETERS_PER_HOUR -> currentSpeedMs * 3.6
                    SpeedUnit.METERS_PER_SECOND -> currentSpeedMs.toDouble()
                    SpeedUnit.MILES_PER_HOUR -> currentSpeedMs * 2.23694
                }
                val speedLimit = it.speedLimitInfo.speed

                textView.text = when {
                    speedLimit == null && currentSpeed <= 0.0 ->
                        "You're not moving"
                    speedLimit == null && currentSpeed > 0.0 ->
                        "Current speed: ${currentSpeed.formatSpeed(unitString)}"
                    speedLimit != null && currentSpeed >= 0.0 -> """
                        Current speed: ${currentSpeed.formatSpeed(unitString)}
                        Speed limit: $speedLimit $unitString
                    """.trimIndent()
                    else -> "UNKNOWN STATE: $currentSpeed, $speedLimit $unitString"
                }
            }
        }
    }

    fun Double.formatSpeed(unitString: String): String =
        String.format(Locale.US, "%.2f $unitString", this)

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun navigationCoroutine(
        function: suspend CoroutineScope.(MapboxNavigation) -> Unit
    ) = object : UIComponent() {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            super.onAttached(mapboxNavigation)
            coroutineScope.launch {
                function(mapboxNavigation)
            }
        }
    }
}
