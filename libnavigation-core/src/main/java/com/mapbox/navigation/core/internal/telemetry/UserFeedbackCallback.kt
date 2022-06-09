package com.mapbox.navigation.core.internal.telemetry

/**
 * A callback that is notified when a new user feedback has been posted.
 * Can be used to retrieve its feedbackId.
 */
fun interface UserFeedbackCallback {

    /**
     * Notifies that a new user feedback has been posted.
     *
     * @param userFeedback the posted [UserFeedback]
     */
    fun onNewUserFeedback(userFeedback: UserFeedback)
}
