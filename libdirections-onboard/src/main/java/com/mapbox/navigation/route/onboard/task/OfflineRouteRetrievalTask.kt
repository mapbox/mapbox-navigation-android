package com.mapbox.navigation.route.onboard.task

import android.os.AsyncTask
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.logger.MapboxLogger
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.route.onboard.OnOfflineRouteFoundCallback
import com.mapbox.navigation.route.onboard.model.OfflineError
import com.mapbox.navigation.route.onboard.model.OfflineRouteError
import com.mapbox.navigator.RouterResult

internal class OfflineRouteRetrievalTask(
    private val navigator: MapboxNativeNavigator,
    private val callback: OnOfflineRouteFoundCallback
) : AsyncTask<String, Void, List<Route>>() {

    @Volatile
    private lateinit var routerResult: RouterResult

    private val logger by lazy { MapboxLogger() }
    private val gson = Gson()

    // For testing only
    internal constructor(
        navigator: MapboxNativeNavigator,
        callback: OnOfflineRouteFoundCallback,
        routerResult: RouterResult
    ) : this(navigator, callback) {
        this.routerResult = routerResult
    }

    override fun doInBackground(vararg params: String): List<Route>? {
        val url = params.first()

        synchronized(navigator) {
            routerResult = navigator.getRoute(url)
        }

        val routesType = object : TypeToken<List<Route>>() {}.type
        return gson.fromJson(routerResult.json, routesType)
    }

    public override fun onPostExecute(offlineRoute: List<Route>?) {
        if (offlineRoute != null) {
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

        logger.e("OfflineRouteRetrievalTask", errorMessage)
        return errorMessage
    }
}
