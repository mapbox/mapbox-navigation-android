package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.feedback.core.CarFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackPoll
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen

/**
 * Default screen for [MapboxScreen.FREE_DRIVE_FEEDBACK].
 */
class FreeDriveFeedbackScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : CarFeedbackScreenFactory(mapboxCarContext) {
    override fun getSourceName(): String = MapboxScreen.FREE_DRIVE

    override fun getCarFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
        return mapboxCarContext.options.feedbackPollProvider.getFreeDriveFeedbackPoll(carContext)
    }
}
