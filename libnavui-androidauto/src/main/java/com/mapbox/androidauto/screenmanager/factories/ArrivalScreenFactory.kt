package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.car.feedback.core.CarFeedbackScreenFactory
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackPoll
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

/**
 * Default screen for [MapboxScreen.ARRIVAL].
 */
class ArrivalScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : CarFeedbackScreenFactory(mapboxCarContext) {
    override fun getSourceName(): String = MapboxScreen.ARRIVAL

    override fun getCarFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
        return mapboxCarContext.feedbackPollProvider.getArrivalFeedbackPoll(carContext)
    }

    override fun onFinish() {
        MapboxNavigationApp.current()?.setNavigationRoutes(emptyList())
        MapboxScreenManager.replaceTop(MapboxScreen.FREE_DRIVE)
    }
}
