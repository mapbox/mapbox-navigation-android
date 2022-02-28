package com.mapbox.navigation.dropin.component.infopanel

import android.annotation.SuppressLint
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewModel
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelFreeDriveLayoutBinding
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.usecase.guidance.StartActiveGuidanceUseCase
import com.mapbox.navigation.dropin.usecase.route.FetchAndSetRouteUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class InfoPanelFreeDriveComponent @AssistedInject constructor(
    private val viewModel: DropInNavigationViewModel,
    private val fetchAndSetRouteUseCase: FetchAndSetRouteUseCase,
    private val startActiveGuidanceUseCase: StartActiveGuidanceUseCase,
    @Assisted private val binding: MapboxInfoPanelFreeDriveLayoutBinding
) : UIComponent() {

    @SuppressLint("MissingPermission")
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        binding.routePreview.setOnClickListener {
            viewModel.destination.value?.also { destination ->
                coroutineScope.launch {
                    fetchAndSetRouteUseCase(destination.point)
                }
            }
        }

        binding.startNavigation.setOnClickListener {
            viewModel.destination.value?.also { destination ->
                coroutineScope.launch {
                    fetchAndSetRouteUseCase(destination.point)
                    startActiveGuidanceUseCase(Unit)
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(binding: MapboxInfoPanelFreeDriveLayoutBinding): InfoPanelFreeDriveComponent
    }
}
