package com.mapbox.services.android.navigation.v5.location.replay

import android.location.Location
import android.os.Handler
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

internal class ReplayLocationDispatcher : Runnable {
    private var locationsToReplay: MutableList<Location>? = null
    private var current: Location? = null
    private var handler: Handler? = null
    private var replayLocationListeners: CopyOnWriteArraySet<ReplayLocationListener>? = null
    private val NON_NULL_AND_NON_EMPTY_LOCATION_LIST_REQUIRED = "Non-null and non-empty location list " + "required."
    private val HEAD = 0

    constructor(locationsToReplay: List<Location>) {
        checkValidInput(locationsToReplay)
        this.locationsToReplay = CopyOnWriteArrayList(locationsToReplay)
        initialize()
        this.replayLocationListeners = CopyOnWriteArraySet()
        this.handler = Handler()
    }

    // For testing only
    constructor(locationsToReplay: MutableList<Location>, handler: Handler) {
        checkValidInput(locationsToReplay)
        this.locationsToReplay = locationsToReplay
        initialize()
        this.replayLocationListeners = CopyOnWriteArraySet()
        this.handler = handler
    }

    override fun run() {
        dispatchLocation(current)
        scheduleNextDispatch()
    }

    fun stop() {
        clearLocations()
        stopDispatching()
    }

    fun pause() {
        stopDispatching()
    }

    fun update(locationsToReplay: List<Location>) {
        checkValidInput(locationsToReplay)
        this.locationsToReplay = CopyOnWriteArrayList(locationsToReplay)
        initialize()
    }

    fun add(toReplay: List<Location>) {
        locationsToReplay?.let { locationsToReplay ->
            val shouldRedispatch = locationsToReplay.isEmpty()
            addLocations(toReplay)
            if (shouldRedispatch) {
                stopDispatching()
                scheduleNextDispatch()
            }
        }
    }

    fun addReplayLocationListener(listener: ReplayLocationListener) {
        replayLocationListeners?.add(listener)
    }

    fun removeReplayLocationListener(listener: ReplayLocationListener) {
        replayLocationListeners?.remove(listener)
    }

    private fun checkValidInput(locations: List<Location>?) {
        val isValidInput = locations == null || locations.isEmpty()
        require(!isValidInput) { NON_NULL_AND_NON_EMPTY_LOCATION_LIST_REQUIRED }
    }

    private fun initialize() {
        current = locationsToReplay?.removeAt(HEAD)
    }

    private fun addLocations(toReplay: List<Location>) {
        locationsToReplay?.addAll(toReplay)
    }

    private fun dispatchLocation(location: Location?) {
        replayLocationListeners?.let {replayLocationListeners->
            for (listener in replayLocationListeners) {
                listener.onLocationReplay(location)
            }
        }
    }

    private fun scheduleNextDispatch() {
        locationsToReplay?.let {locationsToReplay->
            if (locationsToReplay.isEmpty()) {
                stopDispatching()
                return
            }
        }
        current?.let {currentLocation->
            val currentTime = currentLocation.time
            current = locationsToReplay?.removeAt(HEAD)
            val nextTime = currentLocation.time
            val diff = nextTime - currentTime
            handler?.postDelayed(this, diff)
        }
    }

    private fun clearLocations() {
        locationsToReplay?.clear()
    }

    private fun stopDispatching() {
        handler?.removeCallbacks(this)
    }
}