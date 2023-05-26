package com.mapbox.navigation.core.internal.telemetry

interface UserFeedbackSubscriber {

    fun registerUserFeedbackCallback(userFeedbackCallback: UserFeedbackCallback)

    fun unregisterUserFeedbackCallback(userFeedbackCallback: UserFeedbackCallback)
}
