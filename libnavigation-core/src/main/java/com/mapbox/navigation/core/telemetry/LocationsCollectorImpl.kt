//package com.mapbox.navigation.core.telemetry
//
//import android.location.Location
//import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry.LOG_CATEGORY
//import com.mapbox.navigation.core.trip.session.LocationMatcherResult
//import com.mapbox.navigation.utils.internal.logD
//
//internal class LocationsCollectorImpl : LocationsCollector {
//
//    companion object {
//        private const val LOCATION_BUFFER_MAX_SIZE = 20
//    }
//
//    private val locationsBuffer = mutableListOf<Location>()
//    private val eventsLocationsBuffer = mutableListOf<EventLocations>()
//
//    override val lastLocation: Location?
//        get() = locationsBuffer.lastOrNull()
//
//    private fun accumulatePostEventLocation(location: Location) {
//        val iterator = eventsLocationsBuffer.iterator()
//        while (iterator.hasNext()) {
//            iterator.next().let {
//                it.addPostEventLocation(location)
//                if (it.postEventLocationsSize() >= LOCATION_BUFFER_MAX_SIZE) {
//                    it.onBufferFull()
//                    iterator.remove()
//                }
//            }
//        }
//    }
//
//    private fun accumulateLocation(location: Location) {
//        locationsBuffer.run {
//            if (size >= LOCATION_BUFFER_MAX_SIZE) {
//                removeAt(0)
//            }
//            add(location)
//        }
//    }
//
//    override fun collectLocations(
//        locationsCollectorListener: LocationsCollector.LocationsCollectorListener
//    ) {
//        eventsLocationsBuffer.add(
//            EventLocations(
//                locationsBuffer.getCopy(),
//                mutableListOf(),
//                locationsCollectorListener
//            )
//        )
//    }
//
//    override fun flushBuffers() {
//        logD("flush buffer. Pending events = ${eventsLocationsBuffer.size}", LOG_CATEGORY)
//        eventsLocationsBuffer.forEach { it.onBufferFull() }
//        eventsLocationsBuffer.clear()
//    }
//
//    override fun flushBufferFor(
//        locationsCollectorListener: LocationsCollector.LocationsCollectorListener
//    ) {
//        logD("flush buffer for only one observer", LOG_CATEGORY)
//        eventsLocationsBuffer.find {
//            it.locationsCollectorListener === locationsCollectorListener
//        }?.also {
//            it.onBufferFull()
//            eventsLocationsBuffer.remove(it)
//        }
//    }
//
//    override fun onNewRawLocation(rawLocation: Location) {
//        accumulateLocation(rawLocation)
//        accumulatePostEventLocation(rawLocation)
//    }
//
//    override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
//        // Do nothing
//    }
//
//    @Synchronized
//    private fun <T> MutableList<T>.getCopy(): List<T> {
//        return mutableListOf<T>().also {
//            it.addAll(this)
//        }
//    }
//}
