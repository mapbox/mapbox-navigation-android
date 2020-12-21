package com.mapbox.navigation.core.internal.utils

import com.mapbox.navigation.core.trip.session.MapboxTripSession

/**
 * Internal API used for testing. Sets the static unconditional polling patience value for all
 * instances in the process.
 *
 * Pass `null` to reset to default.
 *
 * Do not use in a production environment.
 */
fun setUnconditionalPollingPatience(patience: Long?) {
    MapboxTripSession.UNCONDITIONAL_STATUS_POLLING_PATIENCE = patience ?: 2000L
}

/**
 * Internal API used for testing. Sets the static unconditional polling interval value for all
 * instances in the process.
 *
 * Pass `null` to reset to default.
 *
 * Do not use in a production environment.
 */
fun setUnconditionalPollingInterval(interval: Long?) {
    MapboxTripSession.UNCONDITIONAL_STATUS_POLLING_INTERVAL = interval ?: 1000L
}
