package com.mapbox.navigation.core.internal.utils

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigator.Navigator

object InternalUtils {

    internal var UNCONDITIONAL_POLLING_PATIENCE_MILLISECONDS = 2000L
    internal var UNCONDITIONAL_POLLING_INTERVAL_MILLISECONDS = 1000L

    /**
     * Internal API used for testing. Sets the static unconditional polling patience value for all
     * instances in the process. Needs to be set prior to [MapboxNavigation] creation.
     *
     * Pass `null` to reset to default.
     *
     * Do not use in a production environment.
     */
    fun setUnconditionalPollingPatience(patienceInMillis: Long?) {
        UNCONDITIONAL_POLLING_PATIENCE_MILLISECONDS = patienceInMillis ?: 2000L
    }

    /**
     * Internal API used for testing. Sets the static unconditional polling interval value for all
     * instances in the process. Needs to be set prior to [MapboxNavigation] creation.
     *
     * Pass `null` to reset to default.
     *
     * Do not use in a production environment.
     */
    fun setUnconditionalPollingInterval(intervalInMillis: Long?) {
        UNCONDITIONAL_POLLING_INTERVAL_MILLISECONDS = intervalInMillis ?: 1000L
    }

    fun getNativeUserAgentFragment(): String = Navigator.getUserAgentFragment()
}
