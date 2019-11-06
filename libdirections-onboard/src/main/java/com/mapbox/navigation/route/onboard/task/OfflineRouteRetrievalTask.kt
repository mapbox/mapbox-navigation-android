package com.mapbox.navigation.route.onboard.task

import android.os.AsyncTask
import com.google.gson.Gson
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.route.onboard.OfflineRoute
import com.mapbox.navigation.route.onboard.OnOfflineRouteFoundCallback
import com.mapbox.navigation.route.onboard.model.OfflineError
import com.mapbox.navigation.route.onboard.model.OfflineRouteError
import com.mapbox.navigator.RouterResult

internal class OfflineRouteRetrievalTask(
    private val navigator: MapboxNativeNavigator,
    private val callback: OnOfflineRouteFoundCallback
) : AsyncTask<OfflineRoute, Void, DirectionsRoute>() {

    @Volatile private lateinit var routerResult: RouterResult

    // For testing only
    internal constructor(
        navigator: MapboxNativeNavigator,
        callback: OnOfflineRouteFoundCallback,
        routerResult: RouterResult
    ) : this(navigator, callback) {
        this.routerResult = routerResult
    }

    companion object {
        private const val FIRST_ROUTE = 0
    }

    override fun doInBackground(vararg offlineRoutes: OfflineRoute): DirectionsRoute? {
        val url = offlineRoutes[FIRST_ROUTE].buildUrl()

        synchronized(navigator) {
            routerResult = navigator.getRoute(url)
        }

        return offlineRoutes[FIRST_ROUTE].retrieveOfflineRoute(routerResult)
    }

    public override fun onPostExecute(offlineRoute: DirectionsRoute?) {
        if (offlineRoute != null) {
            callback.onRouteFound(offlineRoute)
        } else {
            callback.onError(OfflineError(generateErrorMessage()))
        }
    }

    private fun generateErrorMessage(): String {
        val (_, _, error, errorCode) = Gson().fromJson(routerResult.json, OfflineRouteError::class.java)

        val errorMessage = "Error occurred fetching offline route: $error - Code: $errorCode"

        // TODO LOGGER
        // Timber.e(errorMessage)
        return errorMessage
    }
}
