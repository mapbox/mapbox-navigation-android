package com.mapbox.navigation.route.onboard.task

import android.os.AsyncTask
import com.mapbox.navigation.route.onboard.OnOfflineTilesConfiguredCallback
import com.mapbox.navigation.route.onboard.model.OfflineError
import com.mapbox.navigator.Navigator

internal class ConfigureRouterTask(
    private val navigator: Navigator,
    private val tilePath: String,
    private val callback: OnOfflineTilesConfiguredCallback
) : AsyncTask<Void, Void, Long>() {

    @Synchronized override fun doInBackground(vararg paramsUnused: Void): Long =
        navigator.configureRouter(tilePath)

    override fun onPostExecute(numberOfTiles: Long) {
        if (numberOfTiles > 0) {
            callback.onConfigured(numberOfTiles.toInt())
        } else {
            val error = OfflineError("Offline tile configuration error: 0 tiles found in directory")
            callback.onConfigurationError(error)
        }
    }
}
