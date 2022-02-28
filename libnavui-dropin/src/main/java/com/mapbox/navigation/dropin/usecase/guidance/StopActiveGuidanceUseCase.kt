package com.mapbox.navigation.dropin.usecase.guidance

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class StopActiveGuidanceUseCase(
    private val navigation: MapboxNavigation,
    private val activeNavigationStarted: MutableStateFlow<Boolean>,
    dispatcher: CoroutineDispatcher
) : UseCase<Unit, Unit>(dispatcher) {

    override suspend fun execute(parameters: Unit) {
        with(navigation) {
            // Stop replay here
            mapboxReplayer.clearEvents()
            resetTripSession()
        }
        activeNavigationStarted.value = false
    }
}
