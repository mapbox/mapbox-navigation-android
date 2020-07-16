package com.mapbox.services.android.navigation.v5.navigation

import android.os.AsyncTask
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouterParams

internal class ConfigureRouterTask(
    private val navigator: Navigator,
    private val routerParams: RouterParams,
    private val callback: OnOfflineTilesConfiguredCallback
) : AsyncTask<Void, Void, Any?>() {

    @Synchronized
    override fun doInBackground(vararg paramsUnused: Void): Any {
        return navigator.configureRouter(routerParams)
    }

    override fun onPostExecute(result: Any?) {
        callback.onConfigured()
    }
}
