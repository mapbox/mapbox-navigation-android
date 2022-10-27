package com.mapbox.navigation.dropin.infopanel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.arrival.ArrivalTextComponent
import com.mapbox.navigation.dropin.databinding.MapboxArrivalTextViewLayoutBinding
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelArrivalTextBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_arrival_text_view_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxArrivalTextViewLayoutBinding.bind(viewGroup)

        return ArrivalTextComponent(binding.arrivedText, context.styles.arrivalTextAppearance)
    }
}
