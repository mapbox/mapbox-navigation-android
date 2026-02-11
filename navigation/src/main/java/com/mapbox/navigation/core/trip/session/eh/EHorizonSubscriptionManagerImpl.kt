package com.mapbox.navigation.core.trip.session.eh

import com.mapbox.navigation.base.internal.factory.EHorizonFactory
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.trip.model.eh.EHorizonPosition
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.ElectronicHorizonPosition
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

internal class EHorizonSubscriptionManagerImpl(
    private val navigator: MapboxNativeNavigator,
    threadController: ThreadController,
) : EHorizonSubscriptionManager {

    private val mainJobController = threadController.getMainScopeAndRootJob()
    private val eHorizonObservers = CopyOnWriteArraySet<EHorizonObserver>()
    private var currentPosition: EHorizonPosition? = null
    private var currentDistances: List<RoadObjectDistanceInfo>? = null

    private val electronicHorizonObserver = object : ElectronicHorizonObserver {
        override fun onRoadObjectEnter(
            roadObjectInfo: com.mapbox.navigator.RoadObjectEnterExitInfo,
        ) {
            notifyAllObservers {
                onRoadObjectEnter(
                    EHorizonFactory.buildRoadObjectEnterExitInfo(roadObjectInfo),
                )
            }
        }

        override fun onRoadObjectExit(
            roadObjectInfo: com.mapbox.navigator.RoadObjectEnterExitInfo,
        ) {
            notifyAllObservers {
                onRoadObjectExit(
                    EHorizonFactory.buildRoadObjectEnterExitInfo(roadObjectInfo),
                )
            }
        }

        override fun onPositionUpdated(
            position: ElectronicHorizonPosition,
            distances: MutableList<com.mapbox.navigator.RoadObjectDistance>,
        ) {
            mainJobController.scope.launch {
                PerformanceTracker.trackPerformanceSync("EHorizon.onPositionUpdated") {
                    val eHorizonPosition = EHorizonFactory.buildEHorizonPosition(position)
                    val eHorizonDistances = mutableListOf<RoadObjectDistanceInfo>()
                    distances.forEach {
                        eHorizonDistances.add(EHorizonFactory.buildRoadObjectDistance(it))
                    }

                    currentPosition = eHorizonPosition
                    currentDistances = eHorizonDistances

                    notifyAllObservers {
                        onPositionUpdated(eHorizonPosition, eHorizonDistances)
                    }
                }
            }
        }

        override fun onRoadObjectPassed(info: com.mapbox.navigator.RoadObjectPassInfo) {
            notifyAllObservers {
                onRoadObjectPassed(
                    EHorizonFactory.buildRoadObjectPassInfo(info),
                )
            }
        }
    }

    private val roadObjectsStoreObserver =
        object : com.mapbox.navigator.RoadObjectsStoreObserver {
            override fun onRoadObjectAdded(roadObjectId: String) {
                notifyAllObservers { onRoadObjectAdded(roadObjectId) }
            }

            override fun onRoadObjectUpdated(roadObjectId: String) {
                notifyAllObservers { onRoadObjectUpdated(roadObjectId) }
            }

            override fun onRoadObjectRemoved(roadObjectId: String) {
                notifyAllObservers { onRoadObjectRemoved(roadObjectId) }
            }

            override fun onCustomRoadObjectMatched(id: String) {
                // TODO: adopt in https://mapbox.atlassian.net/browse/NAVAND-6910
            }

            override fun onCustomRoadObjectAddingCancelled(id: String) {
                // TODO: adopt in https://mapbox.atlassian.net/browse/NAVAND-6910
            }

            override fun onCustomRoadObjectMatchingFailed(id: String) {
                // TODO: adopt in https://mapbox.atlassian.net/browse/NAVAND-6910
            }
        }

    init {
        navigator.addNativeNavigatorRecreationObserver {
            if (eHorizonObservers.isNotEmpty()) {
                setNavigatorObservers()
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
            addRoadObjectsStoreObserver(roadObjectsStoreObserver)
        }
    }

    private fun removeNavigatorObservers() {
        navigator.run {
            setElectronicHorizonObserver(null)
            removeRoadObjectsStoreObserver(roadObjectsStoreObserver)
        }
    }

    private fun notifyAllObservers(action: suspend EHorizonObserver.() -> Unit) {
        mainJobController.scope.launch {
            PerformanceTracker.trackPerformanceSync("EHorizon.notifyAllObservers") {
                eHorizonObservers.forEach {
                    it.action()
                }
            }
        }
    }
}
