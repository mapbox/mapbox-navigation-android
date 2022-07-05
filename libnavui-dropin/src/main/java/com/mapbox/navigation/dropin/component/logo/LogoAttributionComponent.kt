package com.mapbox.navigation.dropin.component.logo

import androidx.core.graphics.Insets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class LogoAttributionComponent(
    private val mapView: MapView,
    private val systemBarInsets: StateFlow<Insets?>
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val initialAttributionMarginLeft = mapView.attribution.getSettings().marginLeft
        systemBarInsets.observe { insets ->
            if (insets != null) {
                val bottom = insets.bottom.toFloat()
                val left = insets.left.toFloat()
                val right = insets.right.toFloat()
                mapView.logo.updateSettings {
                    marginBottom = bottom
                    marginLeft = left
                    marginRight = right
                }
                mapView.attribution.updateSettings {
                    marginBottom = bottom
                    marginLeft = initialAttributionMarginLeft + left
                    marginRight = right
                }
            }
        }
    }
}
