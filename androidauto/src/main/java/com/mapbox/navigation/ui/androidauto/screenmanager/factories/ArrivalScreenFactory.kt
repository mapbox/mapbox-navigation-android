package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.feedback.core.CarFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackPoll
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager

/**
 * Default screen for [MapboxScreen.ARRIVAL].
 */
class ArrivalScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : CarFeedbackScreenFactory(mapboxCarContext) {
    override fun getSourceName(): String = MapboxScreen.ARRIVAL

    override fun getCarFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
        return mapboxCarContext.options.feedbackPollProvider.getArrivalFeedbackPoll(carContext)
    }

    override fun onFinish() {
        MapboxNavigationApp.current()?.setNavigationRoutes(emptyList())
        MapboxScreenManager.replaceTop(MapboxScreen.FREE_DRIVE)
    }
}
