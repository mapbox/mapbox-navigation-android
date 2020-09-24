package com.mapbox.navigation.ui.maps

import com.mapbox.navigation.ui.base.model.routeline.RouteLineState


typealias RouteLineReducer<T1, T2> = (input1: T1, input2: T2) -> T1

var reducer: RouteLineReducer<RouteLineState, RouteLineResult> = { previousState, result ->
    when (result) {
        is RouteLineResult.UpdateDistanceRemaining -> {
            previousState.updateDistanceRemaining(result.distanceRemaining, result.route)
            previousState
        }
        else -> previousState
    }
}


/*
internal val reducer: CounterReducer<CounterState, CounterResult> =
    { previousState, result ->
        when (result) {
            is CounterResult.Increment.Success -> {
                previousState.copy(incrementBy = previousState.counter + result.incrementBy)
            }
            is CounterResult.Increment.Failure -> {
                previousState.copy(error = result.error)
            }
        }
    }
 */
