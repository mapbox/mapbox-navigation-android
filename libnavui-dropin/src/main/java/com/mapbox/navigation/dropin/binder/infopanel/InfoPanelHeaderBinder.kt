package com.mapbox.navigation.dropin.binder.infopanel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.infopanel.InfoPanelHeaderComponent
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelHeaderBinder(
    private val context: NavigationViewContext
) : UIBinder {

    @ExperimentalCoroutinesApi
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_header_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)

        val store = context.viewModel.store
        val binding = MapboxInfoPanelHeaderLayoutBinding.bind(viewGroup)

        return navigationListOf(
            reloadOnChange(
                context.styles.routePreviewButtonStyle,
                context.styles.endNavigationButtonStyle,
                context.styles.startNavigationButtonStyle
            ) { previewStyle, endNavStyle, startNavStyle ->
                InfoPanelHeaderComponent(
                    store = store,
                    binding = binding,
                    routePreviewStyle = previewStyle,
                    endNavigationStyle = endNavStyle,
                    startNavigationStyle = startNavStyle,
                )
            },
            context.uiBinders.infoPanelTripProgressBinder.value?.bind(binding.tripProgressLayout)
                ?: InfoPanelTripProgressBinder(context).bind(binding.tripProgressLayout)
        )
    }
}
