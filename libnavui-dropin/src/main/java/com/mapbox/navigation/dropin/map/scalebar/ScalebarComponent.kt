package com.mapbox.navigation.dropin.map.scalebar

import android.view.Gravity
import androidx.core.graphics.Insets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class ScalebarComponent(
    private val mapView: MapView,
    private val isEnabled: StateFlow<Boolean>,
    private val systemBarInsets: StateFlow<Insets>,
    private val options: StateFlow<DistanceFormatterOptions>,
) : UIComponent() {

    private val defaultMarginTop = 4f
    private val defaultMarginLeft = 4f

    init {
        mapView.scalebar.updateSettings {
            enabled = isEnabled.value
            isMetricUnits = when (options.value.unitType) {
                UnitType.METRIC -> true
                UnitType.IMPERIAL -> false
            }
            position = Gravity.TOP or Gravity.START
            // temporary workaround, remove after MAPSMBL-173
            textBorderWidth = 3f
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        options.observe { formatter ->
            mapView.scalebar.updateSettings {
                isMetricUnits = when (formatter.unitType) {
                    UnitType.METRIC -> true
                    UnitType.IMPERIAL -> false
                }
            }
        }

        isEnabled.observe { enabled ->
            mapView.scalebar.updateSettings {
                this.enabled = enabled
            }
        }

        systemBarInsets.observe { insets ->
            mapView.scalebar.updateSettings {
                marginTop = defaultMarginTop + insets.top
                marginLeft = defaultMarginLeft + insets.left
            }
        }
    }
}
