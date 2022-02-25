package com.mapbox.navigation.dropin.component.infopanel

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewModel
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.usecase.guidance.StartActiveGuidanceUseCase
import com.mapbox.navigation.dropin.usecase.route.FetchAndSetRouteUseCase
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class InfoPanelFreeDriveComponent(
    private val viewModel: DropInNavigationViewModel,
    private val fetchAndSetRouteUseCase: FetchAndSetRouteUseCase,
    private val startActiveGuidanceUseCase: StartActiveGuidanceUseCase,
    private val previewButton: View,
    private val startButton: View
) : UIComponent() {

    @SuppressLint("MissingPermission")
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        previewButton.setOnClickListener {
            viewModel.destination.value?.also { destination ->
                coroutineScope.launch {
                    fetchAndSetRouteUseCase(destination.point)
                }
            }
        }

        startButton.setOnClickListener {
            viewModel.destination.value?.also { destination ->
                coroutineScope.launch {
                    fetchAndSetRouteUseCase(destination.point)
                    startActiveGuidanceUseCase(Unit)
                }
            }
        }
    }
}
