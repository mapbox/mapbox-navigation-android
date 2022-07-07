package com.mapbox.navigation.qa_test_app.view.customnavview

import android.graphics.Color
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
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
                val currentSpeed = it.enhancedLocation.speed
                val speedLimitKph = it.speedLimit?.speedKmph

                textView.text = when {
                    speedLimitKph == null && currentSpeed <= 0.0f ->
                        "You're not moving"
                    speedLimitKph == null && currentSpeed > 0.0 ->
                        "Current speed: ${currentSpeed.formatSpeed()}"
                    speedLimitKph != null && currentSpeed >= 0.0 -> """
                        Current speed: ${currentSpeed.formatSpeed()}
                        Speed limit: $speedLimitKph kmh
                    """.trimIndent()
                    else -> "UNKNOWN STATE: $currentSpeed $speedLimitKph"
                }
            }
        }
    }

    fun Float.formatSpeed(): String =
        String.format(Locale.US, "%.2f kmh", this)

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
