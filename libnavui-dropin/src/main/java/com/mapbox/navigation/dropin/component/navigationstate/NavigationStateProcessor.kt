package com.mapbox.navigation.dropin.component.navigationstate

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
