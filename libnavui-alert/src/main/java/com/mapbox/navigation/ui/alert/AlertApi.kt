package com.mapbox.navigation.ui.alert

import com.mapbox.navigation.ui.base.model.AlertState

/**
 * Interface defining the API's for AlertView
 */
interface AlertApi {

    /**
     * Return the idle state
     * @return AlertState
     */
    fun getIdleState(): AlertState = AlertState.idle()

    /**
     * AlertView includes a progress that shows the time remaining for the view to dismiss automatically.
     * Set the parameter to 0L, if you don't want to see the progress
     * The value cannot be greater than 5000L
     * @param previousState AlertState
     * @param duration Long
     * @return AlertState
     */
    fun durationToDismiss(previousState: AlertState, duration: Long): AlertState {
        return reducer(previousState, setDismissDuration(AlertAction.SetDismissDuration(duration)))
    }

    /**
     * Set the text to be shown in the view
     * @param previousState AlertState
     * @param text String
     * @return AlertState
     */
    fun setAlertText(previousState: AlertState, text: String): AlertState {
        return reducer(previousState, setAlertText(AlertAction.SetAlertText(text)))
    }
}
