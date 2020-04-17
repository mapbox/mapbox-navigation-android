package com.mapbox.navigation.ui.alert

import com.mapbox.navigation.ui.base.model.AlertState
import java.lang.IllegalStateException

/**
 * The class performs logical operations on the [State] fed to it and reduces it to a new [State]
 * @property newState AlertState represents a new reduced [State]
 */
internal class AlertProcessor private constructor(
    private val newState: AlertState
) {

    /**
     * Returns the reduced state
     * @return AlertState reduced state
     */
    fun getNewState() = newState

    data class Builder(private val previousState: AlertState? = null) {

        private var avText: String? = null
        private var durationToDismiss: Long? = null

        /**
         * Set the text to be shown in the view
         * @param avText String text to set
         * @return Builder
         */
        fun showViewWith(avText: String) =
            apply { this.avText = avText }

        /**
         * Set the duration for the progress bar
         * @param durationToDismiss Long
         * @return Builder
         */
        fun durationToDismiss(durationToDismiss: Long) =
            apply {
                if (durationToDismiss > 5000L) {
                    throw IllegalArgumentException("durationToDismiss cannot be greater than 5000ms")
                }
                this.durationToDismiss = durationToDismiss
            }

        /**
         * Function that returns [AlertProcessor]
         * @return AlertProcessor
         */
        fun build(): AlertProcessor {
            previousState?.let {
                val newState = it
                avText?.let { text ->
                    newState.copy(avText = text)
                }
                durationToDismiss?.let { dismissDuration ->
                    newState.copy(durationToDismiss = dismissDuration)
                }
                return AlertProcessor(newState)
            } ?: throw IllegalStateException("AlertViewProcessor.Builder previousState cannot be null")
        }
    }
}
