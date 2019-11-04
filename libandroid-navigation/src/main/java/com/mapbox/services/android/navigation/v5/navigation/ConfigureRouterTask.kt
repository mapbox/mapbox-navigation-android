package com.mapbox.services.android.navigation.v5.navigation

import android.os.AsyncTask
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.TileEndpointConfiguration

internal class ConfigureRouterTask(
    private val navigator: Navigator,
    private val tilePath: String,
    private val tileEndpointConfiguration: TileEndpointConfiguration,
    private val callback: OnOfflineTilesConfiguredCallback
) : AsyncTask<Void, Void, Long>() {

    @Synchronized
    override fun doInBackground(vararg paramsUnused: Void): Long =
        navigator.configureRouter(
            tilePath,
            null,
            null,
            tileEndpointConfiguration
        )

    override fun onPostExecute(numberOfTiles: Long) {
        if (numberOfTiles >= 0) {
            callback.onConfigured(numberOfTiles.toInt())
        } else {
            val error = OfflineError("Offline tile configuration error: 0 tiles found in directory")
            callback.onConfigurationError(error)
        }
    }
}
