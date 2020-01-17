package com.mapbox.services.android.navigation.v5.internal.navigation

import android.location.Location
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

internal class RouteProcessorBackgroundThread(
    private val navigation: MapboxNavigation,
    private val responseHandler: Handler,
    private val listener: Listener
) : HandlerThread(MAPBOX_NAVIGATION_THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND) {

    companion object {
        private const val MAPBOX_NAVIGATION_THREAD_NAME = "mapbox_navigation_thread"
    }

    private val routeProcessor = NavigationRouteProcessor()
    private lateinit var workerHandler: Handler
    private lateinit var runnable: RouteProcessorRunnable

    override fun start() {
        super.start()
        if (!::workerHandler.isInitialized) {
            workerHandler = Handler(looper)
        }
        runnable = RouteProcessorRunnable(routeProcessor, navigation, workerHandler, responseHandler, listener)
        workerHandler.post(runnable)
    }

    override fun quit(): Boolean {
        if (isAlive && ::workerHandler.isInitialized && ::runnable.isInitialized) {
            workerHandler.removeCallbacks(runnable)
        }
        return super.quit()
    }

    fun updateLocation(rawLocation: Location) {
        navigation.retrieveMapboxNavigator().updateLocation(rawLocation)
        if (!isAlive) {
            start()
        }
        runnable.updateRawLocation(rawLocation)
    }

    /**
     * Listener for posting back to the Navigation Service once the thread finishes calculations.
     *
     *
     * No matter what, with each new message added to the queue, these callbacks get invoked once
     * finished and within Navigation Service it is determined if the public corresponding listeners
     * need invoking or not; the Navigation event dispatcher class handles those callbacks.
     */
    interface Listener {

        fun onNewRouteProgress(location: Location, routeProgress: RouteProgress)

        fun onMilestoneTrigger(triggeredMilestones: List<Milestone>, routeProgress: RouteProgress)

        fun onUserOffRoute(location: Location, userOffRoute: Boolean)

        fun onCheckFasterRoute(
            location: Location,
            routeProgress: RouteProgress,
            checkFasterRoute: Boolean
        )
    }
}
