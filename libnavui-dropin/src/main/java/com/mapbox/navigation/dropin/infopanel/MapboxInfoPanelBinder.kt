package com.mapbox.navigation.dropin.infopanel

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R

internal class MapboxInfoPanelBinder : InfoPanelBinder() {

    override fun onCreateLayout(layoutInflater: LayoutInflater, root: ViewGroup): ViewGroup {
        return layoutInflater
            .inflate(R.layout.mapbox_info_panel_layout, root, false) as ViewGroup
    }

    override fun getHeaderLayout(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.infoPanelHeader)

    override fun getContentLayout(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.infoPanelContent)

    override fun applySystemBarsInsets(layout: ViewGroup, insets: Insets) {
        layout.updatePadding(bottom = insets.bottom)
        // top, left and right insets are applied by InfoPanelComponent
    }

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val observer = super.bind(viewGroup)
        return context?.let { context ->
            val layout = viewGroup.findViewById<ViewGroup>(R.id.infoPanelContainer)
            navigationListOf(
                InfoPanelComponent(layout, context),
                observer
            )
        } ?: observer
    }
}
