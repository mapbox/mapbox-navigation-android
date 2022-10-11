package com.mapbox.navigation.dropin.infopanel

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.headerContentBinder
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import kotlinx.coroutines.flow.Flow

internal class InfoPanelHeaderBinder(
    private val headerContentBinder: Flow<UIBinder>
) : UIBinder {

    internal constructor(context: NavigationViewContext) : this(context.headerContentBinder())

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val container = inflateLayout(viewGroup).infoPanelHeaderContent

        return reloadOnChange(headerContentBinder) { it.bind(container) }
    }

    private fun inflateLayout(viewGroup: ViewGroup): MapboxInfoPanelHeaderLayoutBinding {
        viewGroup.removeAllViews()
        val inflater = LayoutInflater.from(viewGroup.context)
        inflater.inflate(R.layout.mapbox_info_panel_header_layout, viewGroup, true)
        return MapboxInfoPanelHeaderLayoutBinding.bind(viewGroup)
    }
}
