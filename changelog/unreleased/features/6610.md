- Added `RouteRefreshController` interface to manage route refreshes. Retrieve it via `MapboxNavigation#routeRefreshController`.
- Added `RouteRefreshController#requestImmediateRouteRefresh` to trigger route refresh request immediately.
- Moved `MapboxNavigation#registerRouteRefreshStateObserver` to `RouteRefreshController#registerRouteRefreshStateObserver`. To migrate, change:
  ```kotlin
  mapboxNavigation.registerRouteRefreshStateObserver(observer)
  ```
  to
  ```kotlin
  mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
  ```
- Moved `MapboxNavigation#unregisterRouteRefreshStateObserver` to `RouteRefreshController#unregisterRouteRefreshStateObserver`. To migrate, change:
  ```kotlin
  mapboxNavigation.unregisterRouteRefreshStateObserver(observer)
  ```
  to
  ```kotlin
  mapboxNavigation.routeRefreshController.unregisterRouteRefreshStateObserver(observer)
  ```  