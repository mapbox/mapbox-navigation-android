package com.mapbox.navigation.core.trip.session

/**
 * Interface to provide opportunity to handle sensor events updates.
 */
fun interface SensorEventUpdatedCallback {

    /**
     * Called whenever sensor event was updated.
     *
     * @param updated true if sensor event was updated successfully.
     */
    fun onSensorEventUpdated(updated: Boolean)
}
