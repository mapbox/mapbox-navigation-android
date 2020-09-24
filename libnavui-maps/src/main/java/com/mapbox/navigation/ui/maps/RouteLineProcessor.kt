package com.mapbox.navigation.ui.maps

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.ui.base.api.routeline.RouteLineApi
import com.mapbox.navigation.ui.base.model.routeline.RouteLineState

typealias UpdateDistanceRemaining<T1, T2> = (input: T1) -> T2

val updateDistanceRemaining: UpdateDistanceRemaining<RouteLineAction, RouteLineResult> = { action ->
    if (action is RouteLineAction.UpdateDistanceRemaining) {
        RouteLineResult.UpdateDistanceRemaining(action.distanceRemaining, action.route)
    } else {
        RouteLineResult.Filler()
    }
}



interface RouteLineApiImpl: RouteLineApi {
    override fun updateDistanceRemaining(distanceRemaining: Float, route: DirectionsRoute, state: RouteLineState): RouteLineState {
        val result = updateDistanceRemaining(RouteLineAction.UpdateDistanceRemaining(distanceRemaining, route))
       return reducer(state, result)
    }
}

/*
typealias IncrementCounter<T1, T2> = (input: T1) -> T2

internal val incrementCounter: IncrementCounter<CounterAction, CounterResult> =
    { action ->
        if (action is CounterAction.Increment) {
            CounterResult.Increment.Success(action.incrementBy)
        }
        CounterResult.Increment.Failure(IllegalArgumentException("Unknown $action passed as input"))
    }
 */
