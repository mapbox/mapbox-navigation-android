package com.mapbox.navigation.dropin.component.infopanel

import android.content.res.Resources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class ArrivalTextComponent(
    private val textView: AppCompatTextView,
    private val textAppearance: StateFlow<Int>
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        textAppearance.observe {
            try {
                TextViewCompat.setTextAppearance(textView, it)
            } catch (e: Resources.NotFoundException) {
                logE(
                    "Failed to update textAppearance: ${e.localizedMessage}",
                    "ArrivalTextComponent"
                )
            }
        }
    }
}
