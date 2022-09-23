package com.mapbox.navigation.dropin.component.map

import android.view.Gravity
import androidx.core.graphics.Insets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.MapboxMapScalebarParams
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class ScalebarComponent(
    private val mapView: MapView,
    private val scalebarParams: StateFlow<MapboxMapScalebarParams>,
    private val systemBarInsets: StateFlow<Insets>,
) : UIComponent() {

    private val defaultMarginTop = 4f
    private val defaultMarginLeft = 4f

    init {
        setUpScalebar(scalebarParams.value)
        mapView.scalebar.updateSettings {
            position = Gravity.TOP or Gravity.START
            // temporary workaround, remove after MAPSMBL-173
            textBorderWidth = 3f
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        scalebarParams.observe { setUpScalebar(it) }
        systemBarInsets.observe { insets ->
            mapView.scalebar.updateSettings {
                marginTop = defaultMarginTop + insets.top
                marginLeft = defaultMarginLeft + insets.left
            }
        }
    }

    private fun setUpScalebar(params: MapboxMapScalebarParams) {
        mapView.scalebar.updateSettings {
            enabled = params.enabled
            isMetricUnits = params.isMetricUnits
        }
    }
}
