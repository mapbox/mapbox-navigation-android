package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.feedback.ui.CarFeedbackAction
import com.mapbox.androidauto.navigation.ActiveGuidanceScreen
import com.mapbox.androidauto.navigation.audioguidance.CarAudioGuidanceUi
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenFactory

/**
 * Default screen for [MapboxScreen.ACTIVE_GUIDANCE].
 */
class ActiveGuidanceScreenFactory(
    private val mapboxCarContext: MapboxCarContext
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        return ActiveGuidanceScreen(
            mapboxCarContext,
            listOf(
                CarFeedbackAction(MapboxScreen.ACTIVE_GUIDANCE_FEEDBACK),
                CarAudioGuidanceUi()
            ),
        )
    }
}
