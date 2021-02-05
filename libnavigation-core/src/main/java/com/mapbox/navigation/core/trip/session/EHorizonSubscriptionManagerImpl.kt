package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectDistanceInfo
import com.mapbox.navigation.core.trip.model.eh.EHorizonPosition
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonObjectDistanceInfo
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonObjectEnterExitInfo
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonPosition
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.ElectronicHorizonPosition
import com.mapbox.navigator.RoadObjectDistanceInfo
import com.mapbox.navigator.RoadObjectEnterExitInfo
import com.mapbox.navigator.RoadObjectsStoreObserver
import kotlinx.coroutines.launch
import java.util.HashMap
import java.util.concurrent.CopyOnWriteArraySet

internal class EHorizonSubscriptionManagerImpl(
    private val navigator: MapboxNativeNavigator,
    private val jobController: JobControl
) : EHorizonSubscriptionManager {

    private val eHorizonObservers = CopyOnWriteArraySet<EHorizonObserver>()
    private var currentPosition: EHorizonPosition? = null
    private var currentDistances: Map<String, EHorizonObjectDistanceInfo>? = null

    private val electronicHorizonObserver = object : ElectronicHorizonObserver() {
        override fun onRoadObjectEnter(roadObjectInfo: RoadObjectEnterExitInfo) {
            notifyAllObservers {
                onRoadObjectEnter(roadObjectInfo.mapToEHorizonObjectEnterExitInfo())
            }
        }

        override fun onRoadObjectExit(roadObjectInfo: RoadObjectEnterExitInfo) {
            notifyAllObservers {
                onRoadObjectExit(roadObjectInfo.mapToEHorizonObjectEnterExitInfo())
            }
        }

        override fun onPositionUpdated(
            position: ElectronicHorizonPosition,
            distances: HashMap<String, RoadObjectDistanceInfo>
        ) {
            val eHorizonPosition = position.mapToEHorizonPosition()
            val eHorizonDistances = mutableMapOf<String, EHorizonObjectDistanceInfo>()
            distances.forEach { (objectId, objectDistanceInfo) ->
                eHorizonDistances[objectId] = objectDistanceInfo.mapToEHorizonObjectDistanceInfo()
            }

            currentPosition = eHorizonPosition
            currentDistances = eHorizonDistances

            notifyAllObservers {
                onPositionUpdated(eHorizonPosition, eHorizonDistances)
            }
        }
    }

    private val roadObjectsStoreObserver = object : RoadObjectsStoreObserver() {
        override fun onRoadObjectAdded(roadObjectId: String) {
            notifyAllObservers { onRoadObjectAdded(roadObjectId) }
        }

        override fun onRoadObjectUpdated(roadObjectId: String) {
            notifyAllObservers { onRoadObjectUpdated(roadObjectId) }
        }

        override fun onRoadObjectRemoved(roadObjectId: String) {
            notifyAllObservers { onRoadObjectRemoved(roadObjectId) }
        }
    }

    private fun notifyAllObservers(action: EHorizonObserver.() -> Unit) {
        jobController.scope.launch {
            eHorizonObservers.forEach {
                it.action()
            }
        }
    }

    override fun registerObserver(observer: EHorizonObserver) {
        if (eHorizonObservers.isEmpty()) {
            setNavigatorObservers()
        }
        eHorizonObservers.add(observer)
        ifNonNull(currentPosition, currentDistances) { position, distances ->
            observer.onPositionUpdated(position, distances)
        }
    }

    override fun unregisterObserver(observer: EHorizonObserver) {
        eHorizonObservers.remove(observer)
        if (eHorizonObservers.isEmpty()) {
            removeNavigatorObservers()
        }
    }

    override fun unregisterAllObservers() {
        eHorizonObservers.clear()
        removeNavigatorObservers()
    }

    override fun reset() {
        // TODO do we need to cache more field? EHorizonObjects?
        currentDistances = null
        currentPosition = null
    }

    private fun setNavigatorObservers() {
        navigator.run {
            setElectronicHorizonObserver(electronicHorizonObserver)
            setRoadObjectsStoreObserver(roadObjectsStoreObserver)
        }
    }

    private fun removeNavigatorObservers() {
        navigator.run {
            setElectronicHorizonObserver(null)
            setRoadObjectsStoreObserver(null)
        }
    }
}
