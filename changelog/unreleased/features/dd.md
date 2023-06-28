- Introduced `RoutesInvalidatedObserver` to be notified about routes invalidation. When this observer is fired, the routes passed as an argument will not be refreshed anymore, because they expired. It is recommended to rebuild a route in this case. Example usage:
```
mapboxNavigation.registerRoutesInvalidatedObserver { routes ->
    val invalidatedIds = routes.map { it.id }
    val currentRoutes = mapboxNavigation.getNavigationRoutes()
    val primaryRoute = currentRoutes.firstOrNull()
    if (routes.any { it.id == primaryRoute?.id }) {
        // primary route is outdated - trigger reroute
        mapboxNavigation.getRerouteController()?.reroute(object : NavigationRerouteController.RoutesCallback {
            override fun onNewRoutes(
                routes: List<NavigationRoute>,
                routerOrigin: RouterOrigin
            ) {
                mapboxNavigation.setNavigationRoutes(routes)
            }
        })
    } else {
        // remove outdated alternatives
        mapboxNavigation.setNavigationRoutes(currentRoutes.filterNot { it.id in invalidatedIds })
    }
}
```