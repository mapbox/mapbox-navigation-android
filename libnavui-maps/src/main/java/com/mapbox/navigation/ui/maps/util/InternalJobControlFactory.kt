package com.mapbox.navigation.ui.maps.util

import com.mapbox.navigation.utils.internal.JobControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal object InternalJobControlFactory {

    /**
     * Creates a [JobControl] using the default dispatcher. This is similar to [ThreadController] but the
     * resources created here aren't shared. It is your responsibility to cancel child jobs as
     * necessary.
     */
    fun createJobControl(): JobControl {
        val parentJob = SupervisorJob()
        return JobControl(parentJob, CoroutineScope(parentJob + Dispatchers.Default))
    }
}
