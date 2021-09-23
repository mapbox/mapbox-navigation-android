package com.mapbox.navigation.dropin.processor

import com.mapbox.navigation.dropin.contract.NavigationStateResult
import com.mapbox.navigation.dropin.state.NavigationState

internal sealed interface NavigationStateProcessor {
    fun process(): NavigationStateResult

    object ToEmpty : NavigationStateProcessor {
        override fun process(): NavigationStateResult =
            NavigationStateResult.ToEmpty(NavigationState.Empty)
    }

    object ToFreeDrive : NavigationStateProcessor {
        override fun process(): NavigationStateResult =
            NavigationStateResult.ToEmpty(NavigationState.FreeDrive)
    }

    object ToRoutePreview : NavigationStateProcessor {
        override fun process(): NavigationStateResult =
            NavigationStateResult.ToEmpty(NavigationState.RoutePreview)
    }

    object ToActiveNavigation : NavigationStateProcessor {
        override fun process(): NavigationStateResult =
            NavigationStateResult.ToEmpty(NavigationState.ActiveNavigation)
    }

    object ToArrival : NavigationStateProcessor {
        override fun process(): NavigationStateResult =
            NavigationStateResult.ToEmpty(NavigationState.Arrival)
    }
}
