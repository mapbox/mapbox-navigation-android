package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.car.feedback.core.CarFeedbackScreenFactory
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackPoll
import com.mapbox.androidauto.screenmanager.MapboxScreen

/**
 * Default screen for [MapboxScreen.SEARCH_FEEDBACK].
 */
class SearchPlacesFeedbackScreenFactory(
    private val mapboxCarContext: MapboxCarContext
) : CarFeedbackScreenFactory(mapboxCarContext) {
    override fun getSourceName(): String = MapboxScreen.SEARCH

    override fun getCarFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
        return mapboxCarContext.options.feedbackPollProvider
            .getSearchFeedbackPoll(carContext)
    }
}
