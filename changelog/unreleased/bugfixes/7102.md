- Added `MapboxNavigation#currentLegIndex`. Use this method to correctly pass leg index to `MapboxRouteLineAPI#setNavigationRoutes` method, this is especially important if you use independent inactive leg styling:
```kotlin
val routesObserver = RoutesObserver {
    routeLineAPI.setNavigationRoutes(result.navigationRoutes, mapboxNavigation.currentLegIndex()).apply {
        routeLineView.renderRouteDrawData(mapboxMap.getStyle()!!, this)
    }    
}
```