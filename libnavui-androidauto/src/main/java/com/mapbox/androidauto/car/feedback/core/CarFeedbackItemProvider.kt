package com.mapbox.androidauto.car.feedback.core

import com.mapbox.androidauto.car.feedback.ui.CarFeedbackItem

interface CarFeedbackItemProvider {
    fun feedbackItems(): List<CarFeedbackItem>
}
