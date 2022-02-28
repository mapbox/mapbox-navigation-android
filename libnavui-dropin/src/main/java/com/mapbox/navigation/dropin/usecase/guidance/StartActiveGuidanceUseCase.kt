package com.mapbox.navigation.dropin.usecase.guidance

import android.annotation.SuppressLint
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.di.DispatcherMain
import com.mapbox.navigation.dropin.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@SuppressLint("MissingPermission")
internal class StartActiveGuidanceUseCase @Inject constructor(
    private val navigation: MapboxNavigation,
    @Named("activeNavigationStarted")
    private val activeNavigationStarted: MutableStateFlow<Boolean>,
    @DispatcherMain dispatcher: CoroutineDispatcher
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
