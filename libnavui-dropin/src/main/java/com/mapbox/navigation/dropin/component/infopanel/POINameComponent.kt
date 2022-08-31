package com.mapbox.navigation.dropin.component.infopanel

import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

@ExperimentalPreviewMapboxNavigationAPI
internal class POINameComponent(
    private val store: Store,
    private val textView: AppCompatTextView,
) : UIComponent() {
    private val resources get() = textView.resources

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        store.select { it.destination }.observe {
            val placeName = it?.features?.firstOrNull()?.placeName()
            textView.text = placeName
                ?: resources.getString(R.string.mapbox_drop_in_dropped_pin)
        }
    }
}
