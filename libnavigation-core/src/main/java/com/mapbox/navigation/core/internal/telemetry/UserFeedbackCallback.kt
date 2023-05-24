package com.mapbox.navigation.core.internal.telemetry

/**
 * A callback that is notified when a new user feedback has been posted.
 * Can be used to retrieve its feedbackId.
 */
fun interface UserFeedbackCallback {

    /**
     * Notifies that a new user feedback has been posted.
     *
     * @param userFeedbackInternal the posted [UserFeedbackInternal]
     */
    fun onNewUserFeedback(userFeedbackInternal: UserFeedbackInternal)
}
