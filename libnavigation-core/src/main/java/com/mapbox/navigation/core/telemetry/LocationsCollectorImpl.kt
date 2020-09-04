package com.mapbox.navigation.core.telemetry

import android.location.Location
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry.TAG

internal class LocationsCollectorImpl(
    private val logger: Logger?
) : LocationsCollector {

    companion object {
        private const val LOCATION_BUFFER_MAX_SIZE = 20
    }

    private val locationsBuffer = mutableListOf<Location>()
    private val eventsLocationsBuffer = mutableListOf<EventLocations>()

    override val lastLocation: Location?
        get() = locationsBuffer.lastOrNull()

    private fun accumulatePostEventLocation(location: Location) {
        val iterator = eventsLocationsBuffer.iterator()
        while (iterator.hasNext()) {
            iterator.next().let {
                it.addPostEventLocation(location)
                if (it.postEventLocationsSize() >= LOCATION_BUFFER_MAX_SIZE) {
                    it.onBufferFull()
                    iterator.remove()
                }
            }
        }
    }

    private fun accumulateLocation(location: Location) {
        locationsBuffer.run {
            if (size >= LOCATION_BUFFER_MAX_SIZE) {
                removeAt(0)
            }
            add(location)
        }
    }

    override fun collectLocations(
        onBufferFull: (List<Location>, List<Location>) -> Unit
    ) {
        eventsLocationsBuffer.add(
            EventLocations(
                locationsBuffer.getCopy(),
                mutableListOf(),
                onBufferFull
            )
        )
    }

    override fun flushBuffers() {
        logger?.d(TAG, Message("flush buffer. Pending events = ${eventsLocationsBuffer.size}"))
        eventsLocationsBuffer.forEach { it.onBufferFull() }
        eventsLocationsBuffer.clear()
    }

    override fun onRawLocationChanged(rawLocation: Location) {
        accumulateLocation(rawLocation)
        accumulatePostEventLocation(rawLocation)
    }

    override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
        // Do nothing
    }

    @Synchronized
    private fun <T> MutableList<T>.getCopy(): List<T> {
        return mutableListOf<T>().also {
            it.addAll(this)
        }
    }
}
