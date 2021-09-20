package com.mapbox.navigation.core.trip.session

/**
 * Interface to provide opportunity to handle leg index updates.
 */
fun interface LegIndexUpdatedCallback {

    /**
     * Called whenever leg index was updated.
     *
     * @param updated true if leg index was updated successfully.
     */
    fun onLegIndexUpdatedCallback(updated: Boolean)
}
