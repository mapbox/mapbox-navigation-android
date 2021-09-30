package com.mapbox.navigation.utils.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object InternalJobControlFactory {

    /**
     * Creates a [JobControl] using the default dispatcher. This is similar to [ThreadController] but the
     * resources created here aren't shared. It is your responsibility to cancel child jobs as
     * necessary.
     */
    fun createDefaultScopeJobControl(): JobControl {
        val parentJob = SupervisorJob()
        return JobControl(parentJob, CoroutineScope(parentJob + Dispatchers.Default))
    }

    /**
     * Creates a [JobControl] using the main dispatcher. This is similar to [ThreadController] but the
     * resources created here aren't shared. It is your responsibility to cancel child jobs as
     * necessary.
     */
    fun createMainScopeJobControl(): JobControl {
        val parentJob = SupervisorJob()
        return JobControl(parentJob, CoroutineScope(parentJob + Dispatchers.Main))
    }
}
