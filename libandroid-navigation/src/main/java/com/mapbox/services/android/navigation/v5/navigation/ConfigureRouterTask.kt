package com.mapbox.services.android.navigation.v5.navigation

import android.os.AsyncTask
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouterParams
import com.mapbox.services.android.navigation.v5.internal.navigation.HttpClient

internal class ConfigureRouterTask(
    private val navigator: Navigator,
    private val routerParams: RouterParams,
    private val callback: OnOfflineTilesConfiguredCallback
) : AsyncTask<Void, Void, Long>() {

    @Synchronized
    override fun doInBackground(vararg paramsUnused: Void): Long = 1
        // navigator.configureRouter(
        //     routerParams,
        //     HttpClient(routerParams.endpointConfig?.userAgent ?: "", true)
        // )

    override fun onPostExecute(numberOfTiles: Long) {
        if (numberOfTiles >= 0) {
            callback.onConfigured(numberOfTiles.toInt())
        } else {
            val error = OfflineError("Offline tile configuration error: 0 tiles found in directory")
            callback.onConfigurationError(error)
        }
    }
}
