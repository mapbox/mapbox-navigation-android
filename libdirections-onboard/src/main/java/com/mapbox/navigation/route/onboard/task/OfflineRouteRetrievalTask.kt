package com.mapbox.navigation.route.onboard.task

import android.os.AsyncTask
import com.google.gson.Gson
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.logger.Logger
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.route.onboard.OnOfflineRouteFoundCallback
import com.mapbox.navigation.route.onboard.model.OfflineError
import com.mapbox.navigation.route.onboard.model.OfflineRouteError
import com.mapbox.navigator.RouterResult

internal class OfflineRouteRetrievalTask(
    private val navigator: MapboxNativeNavigator,
    private val logger: Logger?,
    private val callback: OnOfflineRouteFoundCallback
) : AsyncTask<String, Void, List<DirectionsRoute>>() {

    @Volatile
    private lateinit var routerResult: RouterResult

    private val gson = Gson()

    // For testing only
    internal constructor(
        navigator: MapboxNativeNavigator,
        callback: OnOfflineRouteFoundCallback,
        routerResult: RouterResult
    ) : this(navigator, null, callback) {
        this.routerResult = routerResult
    }

    override fun doInBackground(vararg params: String): List<DirectionsRoute>? {
        val url = params.first()

        synchronized(navigator) {
            routerResult = navigator.getRoute(url)
        }

        return DirectionsResponse.fromJson(routerResult.json).routes()
    }

    public override fun onPostExecute(offlineRoute: List<DirectionsRoute>?) {
        if (!offlineRoute.isNullOrEmpty()) {
            callback.onRouteFound(offlineRoute)
        } else {
            callback.onError(OfflineError(generateErrorMessage()))
        }
    }

    private fun generateErrorMessage(): String {
        val (_, _, error, errorCode) = gson.fromJson(
            routerResult.json,
            OfflineRouteError::class.java
        )

        val errorMessage = "Error occurred fetching offline route: $error - Code: $errorCode"

        logger?.e(Tag("OfflineRouteRetrievalTask"), Message(errorMessage))
        return errorMessage
    }
}
