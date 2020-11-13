package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.trip.model.eh.EHorizon
import com.mapbox.navigation.core.trip.model.eh.EHorizonMapper
import com.mapbox.navigation.core.trip.model.eh.EHorizonPosition
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigator.ElectronicHorizon
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.ElectronicHorizonResultType
import com.mapbox.navigator.GraphPosition
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

internal class ElectronicHorizonObserverImpl(
    private val jobController: JobControl
) : ElectronicHorizonObserver() {

    val eHorizonObservers = CopyOnWriteArraySet<EHorizonObserver>()
    var currentHorizon: EHorizon? = null
    var currentType: String? = null
    var currentPosition: EHorizonPosition? = null

    override fun onElectronicHorizonUpdated(
        horizon: ElectronicHorizon,
        type: ElectronicHorizonResultType
    ) {
        val eHorizon = EHorizonMapper.mapToEHorizon(horizon)
        val resultType = EHorizonMapper.mapToEHorizonResultType(type)
        jobController.scope.launch {
            currentHorizon = eHorizon
            currentType = resultType
            eHorizonObservers.forEach {
                it.onElectronicHorizonUpdated(
                    eHorizon,
                    resultType
                )
            }
        }
    }

    override fun onPositionUpdated(graphPosition: GraphPosition) {
        val position = EHorizonMapper.mapToEHorizonPosition(graphPosition)
        jobController.scope.launch {
            eHorizonObservers.forEach {
                currentPosition = position
                it.onPositionUpdated(position)
            }
        }
    }
}
