package com.mapbox.navigation.dropin.usecase.guidance

import android.annotation.SuppressLint
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@SuppressLint("MissingPermission")
internal class StartActiveGuidanceUseCase(
    private val navigation: MapboxNavigation,
    private val activeNavigationStarted: MutableStateFlow<Boolean>,
    dispatcher: CoroutineDispatcher
) : UseCase<Unit, Unit>(dispatcher) {

    override suspend fun execute(parameters: Unit) {
        with(navigation) {
            // Temporarily trigger replay here
            mapboxReplayer.clearEvents()
            resetTripSession()
            mapboxReplayer.pushRealLocation(navigationOptions.applicationContext, 0.0)
            mapboxReplayer.play()
            startReplayTripSession()
        }
        activeNavigationStarted.value = true
    }
}
