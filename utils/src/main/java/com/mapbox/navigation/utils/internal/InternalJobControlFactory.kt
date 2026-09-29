package com.mapbox.navigation.utils.internal

import com.mapbox.annotation.MapboxExperimental
import com.mapbox.common.dispatchers.SdkDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@OptIn(MapboxExperimental::class)
object InternalJobControlFactory {

    /**
     * Creates a [JobControl] using the default dispatcher. This is similar to [ThreadController] but the
     * resources created here aren't shared. It is your responsibility to cancel child jobs as
     * necessary.
     */
    fun createDefaultScopeJobControl(): JobControl {
        val parentJob = SupervisorJob()
        return JobControl(parentJob, CoroutineScope(parentJob + SdkDispatchers.Default))
    }

    /**
     * Creates a [JobControl] using the main dispatcher. This is similar to [ThreadController] but the
     * resources created here aren't shared. It is your responsibility to cancel child jobs as
     * necessary.
     */
    fun createMainScopeJobControl(): JobControl {
        val parentJob = SupervisorJob()
        return JobControl(parentJob, CoroutineScope(parentJob + SdkDispatchers.Main))
    }

    /**
     * Creates a [JobControl] using the immediate main dispatcher. This is similar to [ThreadController] but the
     * resources created here aren't shared. It is your responsibility to cancel child jobs as
     * necessary.
     */
    fun createImmediateMainScopeJobControl(): JobControl {
        val parentJob = SupervisorJob()
        return JobControl(parentJob, CoroutineScope(parentJob + SdkDispatchers.Main.immediate))
    }

    /**
     * Creates a [JobControl] using the IO dispatcher. This is similar to [ThreadController] but the
     * resources created here aren't shared. It is your responsibility to cancel child jobs as
     * necessary.
     */
    fun createIOScopeJobControl(): JobControl {
        val parentJob = SupervisorJob()
        return JobControl(parentJob, CoroutineScope(parentJob + ThreadController.IODispatcher))
    }
}
