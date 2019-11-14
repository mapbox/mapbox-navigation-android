package com.mapbox.navigation.route.onboard.task

import android.os.Handler
import com.mapbox.navigation.route.onboard.OnOfflineTilesConfiguredCallback
import com.mapbox.navigation.route.onboard.model.OfflineError
import com.mapbox.navigation.utils.thread.Priority
import com.mapbox.navigation.utils.thread.PriorityRunnable
import com.mapbox.navigator.Navigator

// TODO: This is example of ConfigureRouterTask implemented via PriorityRunnable with ThreadPoolExecutor
// TODO: instead of AsyncTask
internal class ConfigureRouterTask2(
    taskId: String,
    priority: Priority,
    handler: Handler,
    private val navigator: Navigator,
    private val tilePath: String,
    private val callback: OnOfflineTilesConfiguredCallback
) : PriorityRunnable(
    taskId,
    priority,
    handler
) {

    override fun run() {
        val numberOfTiles = navigator.configureRouter(tilePath)
        handler.post {
            if (numberOfTiles > 0) {
                callback.onConfigured(numberOfTiles.toInt())
            } else {
                val error = OfflineError("Offline tile configuration error: 0 tiles found in directory")
                callback.onConfigurationError(error)
            }
        }
    }
}
