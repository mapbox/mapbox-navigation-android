package com.mapbox.androidauto.car

import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import com.mapbox.androidauto.ActiveGuidanceState
import com.mapbox.androidauto.ArrivalState
import com.mapbox.androidauto.CarAppState
import com.mapbox.androidauto.FreeDriveState
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.RoutePreviewState
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.androidauto.navigation.audioguidance.CarAudioGuidanceUi
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSender
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackAction
import com.mapbox.androidauto.car.feedback.ui.CarGridFeedbackScreen
import com.mapbox.androidauto.car.feedback.ui.activeGuidanceCarFeedbackProvider
import com.mapbox.androidauto.car.feedback.ui.buildArrivalFeedbackProvider
import com.mapbox.androidauto.car.navigation.ActiveGuidanceScreen
import com.mapbox.androidauto.car.navigation.CarActiveGuidanceCarContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map

class MainScreenManager(val mainCarContext: MainCarContext) {

    fun currentScreen(): Screen = currentScreen(MapboxCarApp.carAppState.value)

    private fun currentScreen(carAppState: CarAppState): Screen {
        return when (carAppState) {
            FreeDriveState, RoutePreviewState -> MainCarScreen(mainCarContext)
            ActiveGuidanceState -> {
                ActiveGuidanceScreen(
                    CarActiveGuidanceCarContext(mainCarContext),
                    listOf(
                        CarFeedbackAction(
                            mainCarContext.mapboxCarMap,
                            CarFeedbackSender(),
                            activeGuidanceCarFeedbackProvider(mainCarContext.carContext)
                        ),
                        CarAudioGuidanceUi()
                    )
                )
            }
            // Push screen and capture feedback. When completed, go back to FreeDriveState and clear the current route.
            ArrivalState -> CarGridFeedbackScreen(
                mainCarContext.carContext,
                javaClass.simpleName,
                CarFeedbackSender(),
                feedbackItems = buildArrivalFeedbackProvider(mainCarContext.carContext),
                encodedSnapshot = null,
            ) {
                mainCarContext.mapboxNavigation.setNavigationRoutes(emptyList())
                MapboxCarApp.updateCarAppState(FreeDriveState)
            }
        }
    }

    suspend fun observeCarAppState() {
        MapboxCarApp.carAppState.map { currentScreen(it) }.distinctUntilChangedBy { it.javaClass }.collect { screen ->
            val screenManager = mainCarContext.carContext.getCarService(ScreenManager::class.java)
            logAndroidAuto("MainScreenManager screen change ${screen.javaClass.simpleName}")
            if (screenManager.top.javaClass != screen.javaClass) {
                screenManager.replace(screen)
            }
        }
    }
}

fun ScreenManager.replace(screen: Screen) {
    popToRoot()
    val root = top
    push(screen)
    root.finish()
}
