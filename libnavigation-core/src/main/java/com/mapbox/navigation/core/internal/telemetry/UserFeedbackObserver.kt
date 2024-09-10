package com.mapbox.navigation.core.internal.telemetry

/**
 * An observer that is notified when a new user feedback has been posted.
 */
fun interface UserFeedbackObserver {

    /**
     * Notifies that a new user feedback has been posted.
     *
     * @param userFeedback the posted [ExtendedUserFeedback]
     */
    fun onNewUserFeedback(userFeedback: ExtendedUserFeedback)
}
