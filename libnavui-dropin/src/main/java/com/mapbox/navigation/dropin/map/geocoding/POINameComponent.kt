package com.mapbox.navigation.dropin.map.geocoding

import android.content.res.Resources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class POINameComponent(
    private val store: Store,
    private val textView: AppCompatTextView,
    private val textAppearance: StateFlow<Int>
) : UIComponent() {
    private val resources get() = textView.resources

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        textAppearance.observe {
            try {
                TextViewCompat.setTextAppearance(textView, it)
            } catch (e: Resources.NotFoundException) {
                logE(
                    "Failed to update textAppearance: ${e.localizedMessage}",
                    "POINameComponent"
                )
            }
        }

        store.select { it.destination }.observe {
            val placeName = it?.features?.firstOrNull()?.placeName()
            textView.text = placeName
                ?: resources.getString(R.string.mapbox_drop_in_dropped_pin)
        }
    }
}
