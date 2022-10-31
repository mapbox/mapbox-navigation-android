package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.feedback.core.CarFeedbackScreenFactory
import com.mapbox.androidauto.feedback.ui.CarFeedbackPoll
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
        return mapboxCarContext.options.feedbackPollProvider.getArrivalFeedbackPoll(carContext)
    }

    override fun onFinish() {
        MapboxNavigationApp.current()?.setNavigationRoutes(emptyList())
        MapboxScreenManager.replaceTop(MapboxScreen.FREE_DRIVE)
    }
}
