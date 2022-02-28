package com.mapbox.navigation.dropin.usecase.guidance

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.di.DispatcherMain
import com.mapbox.navigation.dropin.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class StopActiveGuidanceUseCase @Inject constructor(
    private val navigation: MapboxNavigation,
    @Named("activeNavigationStarted")
    private val activeNavigationStarted: MutableStateFlow<Boolean>,
    @DispatcherMain dispatcher: CoroutineDispatcher
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
