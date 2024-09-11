# Migration from v2 to Navigation Core

## Major 

- Removed `NavigationOptions#accessToken`. Now access token will be read from `mapbox_access_token` string resource. Add a `mapbox_access_token.xml` file to your project 
to the following location: `src/main/res/values/mapbox_access_token.xml` with the following contents:
```
<?xml version="1.0" encoding="utf-8"?>
    <resources xmlns:tools="http://schemas.android.com/tools">
    <string name="mapbox_access_token" translatable="false" tools:ignore="UnusedResources">YOUR_ACCESS_TOKEN</string>
</resources>
```
You can also set the token via: `MapboxOptions.accessToken = YOUR_TOKEN` and change it in runtime. 
First, the token from MapboxOptions will be used. If it's not set, it will be read from resources.
- Similarly, removed access token parameter from the following APIs:
1. `NavigationView` constructor. Also, it's not possible to set it via xml anymore, i. e. `app:access_token` attribute is removed from `<com.mapbox.navigation.dropin.NavigationView>` xml tag.
2. `MapboxManeuverApi#getRoadShields` method.
3. `MapboxJunctionApi` constructor.
4. `MapboxAreaApi` constructor.
5. `MapboxSignboardApi` constructor.
6. `MapboxRouteShieldApi#getRouteShields` method.
7. `MapboxSpeechApi` constructor.

- Replaced `NavigationOptions#locationEngine` and `NavigationOptions#locationEngineRequest` with `NavigationOptions#locationOptions`.
1. If you want to use everything that is default, just don't set location options. Default request is:
```
LocationProviderRequest.Builder()
            .interval(
                IntervalSettings.Builder()
                    .minimumInterval(500)
                    .interval(1000L)
                    .build()
            )
            .accuracy(AccuracyLevel.HIGH)
            .build()
```
2. If you want to customize the request and use default provider, set the options in the following way:
```
NavigationOptions.Builder()
    .locationOptions(
        LocationOptions.Builder()
            .request(myRequest)
            .build()
    )
```
3. If you want to use a custom provider, do this:
```
NavigationOptions.Builder()
    .locationOptions(
        LocationOptions.Builder()
            .locationProviderFactory(
                DeviceLocationProviderFactory { request ->
                    ExpectedFactory.createValue(MyLocationProvider(request))
                },
                LocationProviderType.<YOUR_TYPE>
            )
            .build()
    )
```
, where `MyLocationProvider` implements `DeviceLocationProvider`.
Note that together with DeviceLocationProviderFactory, you have to pass a LocationProviderType. It can be either REAL, MOCKED or MIXED.
Use REAL if your location provider emits only real locations, where the device actually is.
Use MOCKED if you location provider emits only mocked.simulated locations. Can be used for cases with location simulations, replay, etc.
Use MIXED if your provider emits both mocked and real locations. Can be used for cases when you need to switch location providers in runtime, for example, if you have a toggle that enables/disables location simulation.
Note that if you have MOCKED or MIXED type, **the non-real locations must have isMock extra flag set to true**. Real locations must either have it set to false or not set at all.
To set this flag, use:
```
Location.Builder#extra(Value.valueOf(hashMapOf(LocationExtraKeys.IS_MOCK to Value.valueOf(true/false))))
```


4. If you want the locations to be replayed by the NavSDK, you used to have:
```
val mapboxReplayer = MapboxReplayer()
...
NavigationOptions.Builder(context)
    .locationEngine(ReplayLocationEngine(mapboxReplayer))
...    
mapboxNavigation.startTripSession()
```
, now you can replace it with:
```
// use the mapboxReplayer instance that NavSDK created for you:
val mapboxReplayer = mapboxNavigation.mapboxReplayer
...
NavigationOptions.Builder(context)
// no location options
...
// this is important: this invocation will tell NavSDK to start using mapboxReplayer instead of real location engine
mapboxNavigation.startReplayTripSession()
..
// you can use the mapboxReplayer in the same way you used to, neither its API nor its behaviour have changed
```

and 

- Replaced `android.location.Location` with `com.mapbox.common.location.Location` everywhere in the public API.
- Replaced `ReplayLocationEngine` with `ReplayLocationProvider`.
- Removed `context` parameter from `MapboxReplayer#pushRealLocation` method.
- `BuildingValue#buildings` now returns `List<QueriedRenderedFeature>` instead of `List<QueriedFeature>`.
- `MapboxBuildingView#highlightBuilding` now accepts a list `List<QueriedRenderedFeature>` instead of `List<QueriedFeature>`. No app-side migration is required when this method is used together with `MapboxBuildingsApi`.
- Removed `context` parameter from `LocationPuckOptions.Builder#regularPuck` method.
- `MapboxSpeedInfoApi#updatePostedAndCurrentSpeed` now returns a nullable a value when current speed information is not available.
Replace:
```
val value = speedInfoApi.updatePostedAndCurrentSpeed(
    locationMatcherResult,
    distanceFormatterOptions
)
speedInfoView.render(value)
```
with:
```
val value = speedInfoApi.updatePostedAndCurrentSpeed(
    locationMatcherResult,
    distanceFormatterOptions
)
value?.let { speedInfoView.render(it) }
```
- Location component puck bearing enabled property (MapView.location.puckBearingEnabled) has been changed to false by default. Enable it manually when setting up the puck. For example, if you had:
```
binding.mapView.location.apply {
    setLocationProvider(navigationLocationProvider)
    enabled = true
}
```
, change it to:
```
binding.mapView.location.apply {
    setLocationProvider(navigationLocationProvider)
    puckBearingEnabled = true
    enabled = true
}
```
4. Use `CustomRouterRule` in order to setup test routes in instrumentation tests instead of custom implementation of `Router`.
Add  `Test router` dependency to your tests:
```
androidTestImplementation(com.mapbox.navigationcore:testing-router:${navigation_core_version})
```
Add custom router rule to your test:
```
@get:Rule
val navigationRouterRule = createNavigationRouterRule()
```
Set `get route` and `get router refresh` handlers:
```
navigationRouterRule.setRouter(object : MapboxNavigationTestRouter {
    override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback) {
        if (routeOptions == testRouteOptions) {
            callback.onRoutesReady(mockRoute.routeResponse)
        } else {
            callback.onFailure(TestRouterFailure.noRoutesFound())
        }
    }
})
navigationRouterRule.setRouteRefresher(object : MapboxNavigationTestRouteRefresher {
    override fun getRouteRefresh(options: RefreshOptions, callback: RouteRefreshCallback) {
        if (options.responseUUID == mockRoute.routeResponse.uuid()) {
            callback.onRefresh(mockRoute.routeResponse.routes()[options.routeIndex])
        } else {
            callback.onFailure(TestRefresherFailure.serverError())
        }
    }
})
```

Now all online route and route refresh requests to Mapbox Directions API will be intercepted by provided `MapboxNavigationTestRouter` and `MapboxNavigationTestRouteRefresher`. 

`CustomRouterRule` lets users mock only online routes. 
To test offline scenarios, you need to download tiles and build real offline routes onboard.
This way you will make sure that your app is compatible with the SDK's offline logic.

5. Deprecated classes, functions and fields have been removed. See [Nav SDK v2 documentation](https://docs.mapbox.com/android/navigation/api/2.17.7/) for more information about missing parts and migration guides.

6. `com.mapbox.navigation.core.reroute.NavigationRerouteController` has been merged with `com.mapbox.navigation.core.reroute.RerouteController`.

7. `MapboxNavigation` constructor and `onDestroy` methods are now internal. To create or destroy an instance of `MapboxNavigation` use `MapboxNavigationProvider`.
For example, if you used to have:
```kotlin
val mapboxNavigation = MapboxNavigation(options)
...
mapboxNavigation.onDestroy()
```
, replace it with:
```kotlin
val mapboxNavigation = MapboxNavigationProvider.create(options)
...
MapboxNavigationProvider.destroy()
```
8. Introduced support of Mapbox Map Matching API on Nav SDK level. If you navigated using map matched routes, replace usage of Mapbox Java API by the usage of Nav SDK API.
Instead of:
```kotlin
val mapMatching = MapboxMapMatching.builder()
    .accessToken(getMapboxAccessTokenFromResources())
    .coordinates(
        listOf(
            Point.fromLngLat(-117.17282, 32.71204),
            Point.fromLngLat(-117.17288, 32.71225),
            Point.fromLngLat(-117.17293, 32.71244),
        )
    )
    .build()
mapMatching.enqueueCall(object : Callback<MapMatchingResponse> {
    override fun onResponse(
        call: Call<MapMatchingResponse>,
        response: Response<MapMatchingResponse>
    ) {
        mapboxNavigation.setRoutes(
            response.body()?.matchings()?.map { it.toDirectionRoute() }
                ?: emptyList()
        )
    }
    
    override fun onFailure(call: Call<MapMatchingResponse>, throwable: Throwable) {
        
    }
})
```
do:
```kotlin
val options = MapMatchingOptions.Builder()
    .coordinates("-117.17282,32.71204;-117.17288,32.71225;-117.17293,32.71244")
    .build()
mapboxNavigation.requestMapMatching(
    options,
    object : MapMatchingAPICallback {
        override fun success(result: MapMatchingSuccessfulResult) {
            mapboxNavigation.setNavigationRoutes(result.matches)
        }

        override fun failure(failure: MapMatchingFailure) {
        }
        override fun onCancel() {
        }
    }
)
```

9. `NavigaitonRoute` can be created only via `MapboxNavigation`.
Consider requesting routes from [Mapbox Directions API](https://docs.mapbox.com/api/navigation/directions/) using `MapboxNavigation#requestRoutes`
or from [Mapbox Map Matching API](https://docs.mapbox.com/api/navigation/map-matching/).
If you need to pass `NavigationRoute` instance between processes which use the same version of Navigation Core Framework, consider using `NavigationRoute#serialize` and `NavigationRoute#deserializeFrom`.

10. UI functionality from `libnavui-maps`, `libnavui-voice`, `libnavui-tripprogress`, `libnavui-maneuver`, `libnavui-speedlimit`, `libnavui-status` has been moved into new module `libnavui-ui-components`.

11. Core functionality from `libnavui-shield`, `libnavui-tripprogress`, `libnavui-maneuver`, `libnavui-speedlimit` has been moved into new module `libnavigation-tripdata`.

12. Modules `libnavui-app`, `libnavui-dropin`  have been removed. See [Navigation SDK v2 repository](https://github.com/mapbox/mapbox-navigation-android) if you need to copy UI sources to your projects.

13. Module `libnavui-voice` has been renamed to `libnavigation-voice`. Package name has been changed to `com.mapbox.navigation.voice`.

14. Added "mbx.RouteLine" history events. They will be collected automatically by Core Framework for both manual recorder and Copilot recorder when the recording is started.
    You can enable route line events collection for Copilot using `CopilotOptions#shouldRecordRouteLineEvents` option. It is disabled by default.

15. Made `RouteShieldError#url` and `RouteShieldOrigin#originalUrl` nullable. They can be null in case the request had been cancelled before the URL was formed. 

### Route Line
- Split `MapboxRouteLineOptions` into `MapboxRouteLineApiOptions` and `MapboxRouteLineViewOptions`. If you used to have:
```kotlin
val options = MapboxRouteLineOptions.Builder(this)
    .withRouteLineResources(
        RouteLineResources.Builder()
            .routeLineColorResources(
                RouteLineColorResources.Builder()
                    .inActiveRouteLegsColor(Color.YELLOW)
                    .inactiveRouteLegCasingColor(Color.RED)
                    .routeLineTraveledColor(Color.CYAN)
                    .routeLineTraveledCasingColor(Color.BLACK)
                    .lowCongestionRange(0..29)
                    .moderateCongestionRange(30..49)
                    .heavyCongestionRange(50..79)
                    .severeCongestionRange(80..100)
                    .build()
            )
            .trafficBackfillRoadClasses(backfillRoadClasses)
            .originWaypointIcon(R.drawable.origin)
            .destinationWaypointIcon(R.drawable.destination)
            .routeLineScaleExpression(routeLineScaleExpression)
            .restrictedRoadDashArray(dashArray)
            .restrictedRoadOpacity(0.7)
            .restrictedRoadLineWidth(2.1)
            .build()
    )
    .withRouteLineBelowLayerId("road-label-navigation")
    .styleInactiveRouteLegsIndependently(true)
    .withVanishingRouteLineEnabled(true)
    .vanishingRouteLineUpdateInterval(interval)
    .withTolerance(tolerance)
    .displayRestrictedRoadSections(true)
    .softGradientTransition(30)
    .displaySoftGradientForTraffic(true)
    .iconPitchAlignment(IconPitchAlignment.MAP)
    .waypointLayerIconOffset(offset)
    .waypointLayerIconAnchor(IconAnchor.BOTTOM_LEFT)
    .shareLineGeometrySources(true)
    .lineDepthOcclusionFactor(0.85)
    .build()
val routeLineApi = MapboxRouteLineApi(options)
val routeLineView = MapboxRouteLineView(options)
```
change it to:
```kotlin
val apiOptions = MapboxRouteLineApiOptions.Builder()
    .lowCongestionRange(0..29)
    .moderateCongestionRange(30..49)
    .heavyCongestionRange(50..79)
    .severeCongestionRange(80..100)
    .styleInactiveRouteLegsIndependently(true)
    .vanishingRouteLineEnabled(true)
    .vanishingRouteLineUpdateIntervalNano(interval)
    .calculateRestrictedRoadSections(true)
    .trafficBackfillRoadClasses(backfillRoadClasses)
    .build()
val viewOptions = MapboxRouteLineViewOptions.Builder(context)
    .routeLineColorResources(
        RouteLineColorResources.Builder()
            .inActiveRouteLegsColor(Color.YELLOW)
            .inactiveRouteLegCasingColor(Color.RED)
            .routeLineTraveledColor(Color.CYAN)
            .routeLineTraveledCasingColor(Color.BLACK)
            .build()
    )
    .originWaypointIcon(R.drawable.origin)
    .destinationWaypointIcon(R.drawable.destination)
    .scaleExpressions(routeLineScaleExpression)
    .restrictedRoadDashArray(dashArray)
    .restrictedRoadOpacity(0.7)
    .restrictedRoadLineWidth(2.1)
    .routeLineBelowLayerId("road-label-navigation")
    .tolerance(tolerance)
    .displayRestrictedRoadSections(true)
    .softGradientTransition(30.0)
    .displaySoftGradientForTraffic(true)
    .iconPitchAlignment(IconPitchAlignment.MAP)
    .waypointLayerIconOffset(offset)
    .waypointLayerIconAnchor(IconAnchor.BOTTOM_LEFT)
    .shareLineGeometrySources(true)
    .lineDepthOcclusionFactor(0.85)
    .build()
val routeLineApi = MapboxRouteLineApi(apiOptions)
val routeLineView = MapboxRouteLineView(viewOptions)
```
Note that if you had `MapboxRouteLineOptions#displayRestrictedRoadSections` set to true, you must set it to true for both `MapboxRouteLineApiOptions#calculateRestrictedRoadSections` and `MapboxRouteLineViewOptions#displayRestricetdRoadSections`.
You can have a set-up where some of your `MapboxRouteLineView`s display the restricted data and others don't. Set `MapboxRouteLineApiOptions#calculateRestrictedRoadSections` if at least one of your `MapboxRouteLineView`s will display the restricted data. Set `MapboxRouteLineViewOptions#displayRestrictedRoadSections` only to those views, who are going to display it.

Removed API:
1. `RouteLineResources#roundedLineCap`
2. `MapboxRouteLineOptions#routeStyleDescriptors`
3. Removed the possibility of modify and reading data from `RouteLineSetValue`, `RouteLineClearValue` and `RouteLineUpdateValue`. Do not use these classes on your side, just pass the objects between `MapboxRouteLineAPI` and `MapboxRouteLineView`.
4. `MapboxRouteLineAPI#options` and `MapboxRouteLineView#options` properties are no longer public.
5. Made `RouteLineExpressionProvider` and `RouteLineTrimExpressionProvider` internal.
6. `MapboxRouteLineApi#showRouteWithLegIndexHighlighted`
7. `MapboxRouteLineApi#setPrimaryTrafficColor` - use `MapboxRouteLineView#updateDynamicOptions` to change primary traffic color.
8. `MapboxRouteLineApi#setAlternativeTrafficColor` - use `MapboxRouteLineView#updateDynamicOptions` to change alternative traffic color.

- Added a possibility to change a subset of `MapboxRouteLineViewOptions` in runtime without the need to recreate the components. The subset that can be changed is defined in `MapboxRouteLineViewDynamicOptionsBuilder`. To change the dynamic options in runtime, use the following code:
```
routeLineView.updateDynamicOptions(style) {
    routeLineColorResources(newColors)
    // ...
}
routeLineApi.getRouteDrawData {
    routeLineView.renderRouteDrawData(style, it)
}
```

Similarly, split `RouteLineConfig#options` into `RouteLineConfig#apiOptions` and `RouteLineConfig#viewOptions` and added `RouteLineConfig#viewOptionsUpdates`.
