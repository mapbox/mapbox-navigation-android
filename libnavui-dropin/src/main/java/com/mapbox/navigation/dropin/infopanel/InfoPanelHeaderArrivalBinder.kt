package com.mapbox.navigation.dropin.infopanel

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderArrivalLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.arrivalTextComponent
import com.mapbox.navigation.dropin.internal.extensions.endNavigationButtonComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

internal class InfoPanelHeaderArrivalBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        viewGroup.removeAllViews()
        val binding = MapboxInfoPanelHeaderArrivalLayoutBinding.inflate(
            LayoutInflater.from(viewGroup.context),
            viewGroup
        )

        return context.run {
            navigationListOf(
                arrivalTextComponent(binding.arrivedTextContainer),
                endNavigationButtonComponent(binding.endNavigationButtonLayout)
            )
        }
    }
}
