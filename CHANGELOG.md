# Changelog for the Mapbox Navigation SDK Core Framework for Android

## Navigation SDK Core Framework 3.16.4 - 14 November, 2025
#### Features


#### Bug fixes and improvements


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.16.4` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.16.4))
- Mapbox Navigation Native `v324.16.4`
- Mapbox Core Common `v24.16.4`
- Mapbox Java `v7.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.8.0))


## Navigation SDK Core Framework 3.16.3 - 14 November, 2025
#### Features


#### Bug fixes and improvements
- Fix ANR when calling `MapboxVoiceInstructionsPlayer::stop` 
- Fix NullPointerException when using `MapboxVoiceInstructionsPlayer`. 
- Optimize the performance of road cameras in Free Drive mode.  

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.16.3` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.16.3))
- Mapbox Navigation Native `v324.16.3`
- Mapbox Core Common `v24.16.3`
- Mapbox Java `v7.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.8.0))


## Navigation SDK Core Framework 3.16.2 - 04 November, 2025
#### Features
- Reworked tile-loading delay in the predictive cache: both tile loading and tile calculation are now deferred for improved performance. 

#### Bug fixes and improvements
- Add the `RoadCamerasConfig::belowLayerId` option to set the `belowLayerId` of the road camera icons layer. By default, the road camera icons are below the 2D CPP icon. 
- ⚠️ Breaking changes in Experimental API: `RoadCamerasConfig` constructor is now private. Use the `RoadCamerasConfig.Builder` to create an instance of `RoadCamerasConfig`. 

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.16.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.16.2))
- Mapbox Navigation Native `v324.16.2`
- Mapbox Core Common `v24.16.2`
- Mapbox Java `v7.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.8.0))


## Navigation SDK Core Framework 3.16.1 - 28 October, 2025
#### Features
- Expose roadEdgeId to LocationMatcherResult 

#### Bug fixes and improvements


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.16.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.16.1))
- Mapbox Navigation Native `v324.16.1`
- Mapbox Core Common `v24.16.1`
- Mapbox Java `v7.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.8.0))


## Navigation SDK Core Framework 3.16.0 - 23 October, 2025
#### Features

- Added `MapMatchingOptions.voiceUnits` which allows applications to specify the unit system used for voice instructions in Map Matching. 
- Delay the start of the predictive cache when tiles are requested #6684
- Added `AdasEdgeAttributes#isBuiltUpArea` and `AdasEdgeAttributes#roadItems` properties. 
- Added a new `RoadObjectMatcherOptions` class that configures the road object matching behavior. Available through the `NavigationOptions` class. 
- Added `RouteCalloutUiStateProvider` class that allows to listen to Route Callout UI data. 
Normally, route callouts are drawn under the hood in NavSDK when this feature is enabled in `MapboxRouteLineApiOptions`.
However, there might be cases when app wants to only get the callout data from NavSDK and attach the DVA itself.
An example of such a case is using Mapbox Maps SDK Compose extensions: attaching a DVA for
Compose MapboxMap is done via [compose-specific API](https://docs.mapbox.com/android/maps/examples/compose/dynamic-view-annotations/),
which is not currently supported by NavSDK.
In this case you may listen to `RouteCalloutUiStateData` updates via `RouteCalloutUiStateProvider` and use its information by attach a DVA.
- Added experimental overloads for `MapboxManeuverApi#getRoadShields` and `MapboxRouteShieldApi#getRouteShields` that accept a `ShieldFontConfig` parameter, enabling custom font selection for route shields. 
- Added experimental `MapboxNavigationSVGExternalFileResolver` that can resolve fonts for SVG rendering from assets or use system fonts. 
- Updated `MapboxNavigation.replanRoute()` to now accept a new optional parameter of type `ReplanRoutesCallback`. 
- Added ability to filter by data source in EV charging station search operations. 
- Added `MapboxSpeedZoneInfo` class to represent speed zone information. Available through `UpcomingCamerasObserver::onSpeedZoneInfo` and `RoadCamerasMapCallback::onSpeedZoneInfo` callbacks. 
- Added experimental support for ADAS tiles in the predictive cache. See `PredictiveCacheNavigationOptions` for more information. 
- Added support for Android 16 KB page-size devices. To consume SDK compatible with NDK 27 you need to add `-ndk27` suffix to the artifact name, for example, `com.mapbox.navigationcore:navigation` -> `com.mapbox.navigationcore:navigation-ndk27`. 
- Added method overload `TilesetDescriptorFactory#getLatest(Boolean)` that allows to specify whether to include ADAS tiles. 
- Extracted TTS functionality into a new module `audio`. 
- DR improvements - more robust models for GNSS trust, road calibration, and wheel speed trust; 
- Improve main thread utilization by removing unintended locks (visible on systems with overloaded CPU) 
- Disable the defaults for collection of tunnel/bridge subgraphs in free drive. The clients will need to specify explicitly which objects to collect via AlertServiceOptions in the public SDK interface. 
- Added support of immediate update of location puck bearing in [NavigationLocationProvider] in case of overlapping key points. 
- `RoadCamerasManager` in active guidance now relies on new `road_camera` Directions API annotation, which improves the performance of the camera data retrieval and quality of the data. 
- Added `MapboxRoadCamera::activeGuidanceInfo`, containing information about the route id, leg index, geometry index and step intersection of the camera in active guidance. 
- ⚠️ Breaking changes in Experimental API. `MapboxEvSearchClientFactory.#getInstance()` no longer accepts access token as a parameter. The default `MapboxOptions.accessToken` will be used. 
- `CarPlaceSearchOptions.accessToken` and corresponding builder function has been deprecated because `accessToken` is no longer in use as the search component now uses the default `MapboxOptions.accessToken`. 
- Used legacy/custom date primitives in EV modules to support older Android API levels. 
- Added `DriverNotification`, `DriverNotificationProvider` interfaces with `EvBetterRouteNotificationProvider` and `SlowTrafficNotificationProvider` implementations. Add new `DriverNotificationManager` API to attach or detach providers and `DriverNotificationManager.observeDriverNotification()` to handle the flow of driver notifications. 
- Added default location providers. 
- Added `EvBusyChargingStationNotificationProvider` to notify when the EV is charging station is busy and propose alternative route. 
- Added experimental `NavigationPerformance#performanceTracingEnabled` which enables/disables internal performance trace sections. 
- Add `MapboxRoadCamera::inOnRoute` flag which indicates if the roiad camera is on the current route. 
- Add `MapboxRoadCamerasDisplayConfig::showOnlyOnRoute` config parameter to display only road cameras on the route. 
- Added support for EV charge point tariffs accessible via `EvStation.tariffs`. 
- New experimental property `LocationMatcherResult.correctedLocationData` is available. 
- New experimental function `GraphAccessor.getAdasisEdgeAttributes()` is available. It returns ADAS attributes for the requested edge. 
- Expose road type in the `MapboxRoadCamera` 
- Added support for section control speed cameras. 
- Extended `MapboxTripProgressApi` to provide information about time zone at leg/route destination. 
- Added `TripProgressUpdateFormatter.getEstimatedTimeToArrival` overload that formats ETA using a given time zone. 
- Added curvatures support on intersections in ADAS tiles 
- Reduced amount of error logs  
- Added periodic logs of Navigator/Cache configs 
- Added support for wheel speed usage during no signal simulation to determine passed distance for mobile profile 
- Improved off road transitions 
- :warning: Breaking changes in Experimental API `MapboxRouteCalloutView#renderCallouts(RouteCalloutData,MapboxRouteLineView)`. It's required to associate Route line with Callout View. 
- Added experimental `SearchAlongRouteUtils` class to optimize search along routes scenario by providing optimally selected points. 
- Added experimental `RoutingTilesOptions#hdTilesOptions` to configure HD tiles endpoint. 
- `DataInputsManager` now can be used from any thread. 
- Added experimental Road Cameras modules to provide notifications about road cameras along the route and show them on the map. 
- Added option to display the route line with a blur effect. 
- Added experimental functions `MapboxNavigation#startTripSessionWithPermissionCheck()` and `MapboxNavigation#startReplayTripSessionWithPermissionCheck` that immediately throw `IllegalStateException` if they are called with `withForegroundService` parameter set to true, but Android foreground service permissions requirements are not met. 
- Added support for SVG junction views, see `MapboxJunctionApi#generateJunction(instructions: BannerInstructions, @JunctionViewFormat format: String, consumer: MapboxNavigationConsumer<Expected<JunctionError, JunctionViewData>>)`. [#6803](https://github.com/mapbox/mapbox-navigation-android/pull/6803)
- Added experimental `NavigationRoute#routeRefreshMetadata` which contains data related to refresh of the route object. [#6736](https://github.com/mapbox/mapbox-navigation-android/pull/6736)
- Signature of experimental `EtcGateApi#updateEtcGateInfo` function has been changed, now it accepts `EtcGateApi.EtcGateInfo` as a function parameter. [#6508](https://github.com/mapbox/mapbox-navigation-android/pull/6508)
- Experimental Data Inputs functionality has been removed from the `core` module to a separate `datainputs` module (`MapboxNavigation#dataInputs` and everything from the package `com.mapbox.navigation.datainputs` have been removed). [Contact us](https://www.mapbox.com/support) to get more information on how to get access to the module. [#6508](https://github.com/mapbox/mapbox-navigation-android/pull/6508)
- Experimental Adasis functionality has been removed from the `core` module (`MapboxNavigation`'s functions `setAdasisMessageObserver`, `resetAdasisMessageObserver`, `updateExternalSensorData`, and `GraphAccessor#getAdasisEdgeAttributes` have been removed). [Contact us](https://www.mapbox.com/support) in case you're interested in ADASIS functionality. [#6508](https://github.com/mapbox/mapbox-navigation-android/pull/6508)
- Added experimental `RoutingTilesOptions#fallbackNavigationTilesVersion` which lets define version of navigation tiles to fallback in case of offline routing failure with navigation tiles defined in `RoutingTilesOptions#tilesVersion`. [#6475](https://github.com/mapbox/mapbox-navigation-android/pull/6475)
- Added experimental `MapboxRouteLineViewOptions#fadeOnHighZoomsConfig` and `MapboxRouteArrowOptions#fadeOnHighZoomsConfig` to configure smooth fading out of route line or/and arrows on high zoom levels. [#6367](https://github.com/mapbox/mapbox-navigation-android/pull/6367)
- The `PredictiveCacheController(PredictiveCacheOptions)` constructor is now deprecated. Use `PredictiveCacheController(MapboxNavigation, PredictiveCacheOptions)` instead. [#6376](https://github.com/mapbox/mapbox-navigation-android/pull/6376)
- Added `NavigationScaleGestureHandlerOptions#followingRotationAngleThreshold` that define threshold angle for rotation for `FOLLOWING` Navigation Camera state. [#6234](https://github.com/mapbox/mapbox-navigation-android/pull/6234)
- Added the ability to filter road names based on the system language [#6163](https://github.com/mapbox/mapbox-navigation-android/pull/6163)
- `com.mapbox.navigation.base.road.model.RoadComponent` objects that contain only slashes in their text are filtered out [#6163](https://github.com/mapbox/mapbox-navigation-android/pull/6163)
- Now `EHorizonResultType.Type` has a new element called `EHorizonResultType.NOT_AVAILABLE`. [#6290](https://github.com/mapbox/mapbox-navigation-android/pull/6290)
- Old `MapboxNavigation.postUserFeedback()` functions have been deprecated, use an overloading that accepts `UserFeedback` as a parameter. [#5781](https://github.com/mapbox/mapbox-navigation-android/pull/5781)
- Introduce MapboxRouteCalloutApi and MapboxRouteCalloutView to attach callouts to route lines with info about duration  [#2743](https://github.com/mapbox/mapbox-navigation-android/pull/2743)
- Optimized memory usage in Directions API model classes by interning frequently occurring strings in JSON. [#5854](https://github.com/mapbox/mapbox-navigation-android/pull/5854)
- Added experimental `MapboxNavigation#replanRoute` to handle cases when user changes route options during active guidance, [#5286](https://github.com/mapbox/mapbox-navigation-android/pull/5286)
for example enabling avoid ferries.
- Added `DataInputsManager` to allow the provision of data from external sensors to the navigator, see `MapboxNavigation.dataInputsManager`. Experimental `EtcGateInfo` has been moved to `com.mapbox.navigation.core.datainputs` package. `EtcGateApi` has been deprecated. [#5957](https://github.com/mapbox/mapbox-navigation-android/pull/5957)
- Removing the ExperimentalMapboxNavigationAPI flag for Search predictive cache. [#5615](https://github.com/mapbox/mapbox-navigation-android/pull/5615)
- [BREAKING CHANGE] `PredictiveCacheOptions.unrecognizedTilesetDescriptorOptions` has been renamed to `PredictiveCacheOptions.predictiveCacheSearchOptionsList`. Additionally, `PredictiveCacheUnrecognizedTilesetDescriptorOptions` has been renamed to `PredictiveCacheSearchOptions`. Now, only search-related options can be passed to `PredictiveCacheSearchOptions`. [#5244](https://github.com/mapbox/mapbox-navigation-android/pull/5244)
- Introduced experimental traffic adjustment mechanism during a drive and added `TrafficOverrideOptions` to control this feature [#2811](https://github.com/mapbox/mapbox-navigation-android/pull/2811)
- Changed `Alternatives` that deviate close to a destination point are removed before a fork is reached. [#5848](https://github.com/mapbox/mapbox-navigation-android/pull/5848)
- Added `RerouteStrategyForMapMatchedRoutes` to `RerouteOptions`. Reroute option `enableLegacyBehaviorForMapMatchedRoute` was removed, use `NavigateToFinalDestination` strategy instead. [#5256](https://github.com/mapbox/mapbox-navigation-android/pull/5256)

#### Bug fixes and improvements

- Fixed Android Auto build for the NDK 27 compatible variant 
- Fix the bug that causes road cameras on alternative routes to not be removed from the road when its road is not active or passed during active guidance. 
-  Fix ADAS tiles loading for ambient cache without consumer #7128
- Improved Visual Turn Experience in Active Guidance and Free-Drive for High-Frequency Input #3863
- The crash happens on route parsing with the steps=false #6889
- Support settings for per-level routing tiles compression #6622
#### Known issues :warning:
- Fix the bug that causes road cameras on alternative routes to be marked as passed but not removed from the map. 
- Fixed the incorrect order of callbacks when notifying about road cameras on the route. 
- Optimize the `MapboxRouteArrowView` to skip re-rendering arrows that have not changed. 
- Decrased excessively high GeoJSON buffer size from 128 to 32 to improve the memory footprint. 
- Avoid unnecessary navigation arrow GeoJSON updates 
- Optimized camera animations that involve significant zoom change. 
- Fixed an issue where the closer part of route line might have been overlapped by a farther part in case they covered the same space within a single leg (e. g. U-turns on narrow roads).  
- Don't reset the re-route request when on-route/off-route events are flaky. 
- Use the `enhancedLocation` in the RoadCamerasManager class to get a more accurate current speed for the vehicle. 
- Fixed an issue where after a reroute the vanishing point on the route line might have been ahead of the actual vehicle's position. 
- Added `HistoryRecorderOptions#shouldRecordRouteLineEvents` property to enable/disable route line events collection for manual recording (see `CopilotOptions#shouldRecordRouteLineEvents` for the same functionality with Copilot); it is disabled by default. 
- Fixed an issue where the Speed Camera notification would appear prematurely when the car's speed was 0. 
- Fix Route replayer: normalize bearing values to be in the range of [0..360) degrees. 
- Fixed a bug where alternative routes from `RoutesUpdatedResult#ignoredRoutes` were set to `RoutesUpdatedResult#navigationRoutes` after the first route progress update. 
- Fix when already passed part of route appears behind CCP 
- Fixed a crash that happened on foreground service start on Android APIs 28 and below. 
- Deprecated EstimatedTimeToArrivalFormatter and introduced EstimatedTimeOfArrivalFormatter, which allows to format ETA with respect to destination time zone. 
- Deprecated TripProgressUpdateFormatter.estimatedTimeToArrivalFormatter and introduced TripProgressUpdateFormatter.estimatedTimeOfArrivalFormatter, which allows to format ETA with respect to destination time zone. 
- Fixed `MapboxNavigationSDKInitializerImpl` logic so that `uxfKey` is properly retrieved and sent over. 
- Fixed the condition for verifying the last good signal state in the offroad detection logic 
- Fixed incorrect calculation of a "missing part" of the route causing all lanes to be mark as divergent 
- Fixed EHorizon rural road objects sometimes marked as urban 
- Fixed a bug that happened during reroute in case if initial route was requested with `approaches` option specified. 
- Improved reroute and alternative routes behavior 
- Fixed map matching bug after leaving a tunnel 
- Increased route stickiness in dead reckoning mode 
- Added ability to send raw unfused GNSS location in addition to fused one 
- Improved odometry and road graph fusing in urban canyons 
- Signature of experimental `RawGnssSatelliteData` has been changed, now it requires `residual` as a constructor parameter 
- Experimental `RawGnssLocation` type has been removed, now `RawGnssData` requires `DilutionOfPrecision` as a parameter 
- Now service type is specified explicitly when foreground location service starts. 
- Nav SDK now removes passed alternative routes as soon as user passed fork point. [#6813](https://github.com/mapbox/mapbox-navigation-android/pull/6813)
- Fixed a potential route line layers visibility race, which might have happened if you invoked `MapboxRouteLineView#showPrimaryRoute` and `MapboxRouteLineView#renderRouteDrawData` approximately at the same time.  [#6751](https://github.com/mapbox/mapbox-navigation-android/pull/6751)
- Optimized CA routes handling by skiping route parsing if it's already exist in direction session    [#6868](https://github.com/mapbox/mapbox-navigation-android/pull/6868)
- Fixed `CarSearchLocationProvider` produces _NullPointerException_ when using Mapbox Search SDK.  [#6702](https://github.com/mapbox/mapbox-navigation-android/pull/6702)
- Fixed an issue preventing Copilot from correctly recording history events.   [#6787](https://github.com/mapbox/mapbox-navigation-android/pull/6787)
- Improved reroute and alternative routes behavior [#6989](https://github.com/mapbox/mapbox-navigation-android/pull/6989)
- Fixed a bug causing some history files recorded during the previous app sessions not to be uploaded by the Copilot. [#6359](https://github.com/mapbox/mapbox-navigation-android/pull/6359)
- Fixed an issue where native memory was not being properly released after the `MapboxNavigation` object was destroyed. [#6376](https://github.com/mapbox/mapbox-navigation-android/pull/6376)
- Fixed the issue of unwanted rerouting occurring immediately after setting a new route [#6163](https://github.com/mapbox/mapbox-navigation-android/pull/6163)
- Fixed a crash caused by an overflow in the JNI global reference table. [#6290](https://github.com/mapbox/mapbox-navigation-android/pull/6290)
- Fixed an issue with vignettes in Romania and Bulgaria for offline routing when tolls are excluded. [#6290](https://github.com/mapbox/mapbox-navigation-android/pull/6290)
- Addressed several issues that occurred when switching to an alternative route. [#6290](https://github.com/mapbox/mapbox-navigation-android/pull/6290)
- Implementation of `RerouteController#registerRerouteStateObserver` now invokes observer immediately with current state instead of posting invocation to the main looper.  [#5286](https://github.com/mapbox/mapbox-navigation-android/pull/5286)
- Fixed UI jank caused by on-device TextToSpeech player. [#5638](https://github.com/mapbox/mapbox-navigation-android/pull/5638)
- Removed `PredictiveCacheController#removeSearchControllers` and `PredictiveCacheController#createSearchControllers`. Now search predictive cache controller is created and destroyed together with `PredictiveCacheController` instance if `PredictiveCacheOptions.predictiveCacheSearchOptionsList` is provided. [#5714](https://github.com/mapbox/mapbox-navigation-android/pull/5714)
- Improve performance when handling large road objects on the eHorizon's MPP. [#6014](https://github.com/mapbox/mapbox-navigation-android/pull/6014)
- Fixed `Routes` that origin is out of primary route cannot be added as alternatives. [#5848](https://github.com/mapbox/mapbox-navigation-android/pull/5848)
- Fixed a crash due to incorrect OpenLR input data [#5400](https://github.com/mapbox/mapbox-navigation-android/pull/5400)
- Fixed a bug with spinning smoothed coordinate [#5400](https://github.com/mapbox/mapbox-navigation-android/pull/5400)
- Fixed issue for calculating the trim-offset value for the vanishing route line feature when the current geometry index emitted by route progress is greater than the value expected.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.16.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.16.0))
- Mapbox Navigation Native `v324.16.0`
- Mapbox Core Common `v24.16.0`
- Mapbox Java `v7.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.8.0))


## Navigation SDK Core Framework 3.16.0-rc.2 - 17 October, 2025
#### Features


#### Bug fixes and improvements
- Fixed Android Auto build for the NDK 27 compatible variant 

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.16.0-rc.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.16.0-rc.2))
- Mapbox Navigation Native `v324.16.0-rc.2`
- Mapbox Core Common `v24.16.0-rc.2`
- Mapbox Java `v7.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.8.0))


## Navigation SDK Core Framework 3.16.0-rc.1 - 10 October, 2025
#### Features
- Added `MapMatchingOptions.voiceUnits` which allows applications to specify the unit system used for voice instructions in Map Matching. 
- Delay the start of the predictive cache when tiles are requested #6684

#### Bug fixes and improvements
- Fix the bug that causes road cameras on alternative routes to not be removed from the road when its road is not active or passed during active guidance. 
-  Fix ADAS tiles loading for ambient cache without consumer #7128
- Improved Visual Turn Experience in Active Guidance and Free-Drive for High-Frequency Input #3863
- The crash happens on route parsing with the steps=false #6889
- Support settings for per-level routing tiles compression #6622
#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.16.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.16.0-rc.1))
- Mapbox Navigation Native `v324.16.0-rc.1`
- Mapbox Core Common `v24.16.0-rc.1`
- Mapbox Java `v7.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.8.0))


## Navigation SDK Core Framework 3.16.0-beta.1 - 26 September, 2025
#### Notes
3.16.x is the next version after 3.12.x. For technical reasons, versions 3.13.x, 3.14.x and 3.15.x are skipped. Starting from 3.16.x, the Nav SDK minor version will be aligned with other Mapbox dependencies.

#### Features
- Added `AdasEdgeAttributes#isBuiltUpArea` and `AdasEdgeAttributes#roadItems` properties. 
- Added a new `RoadObjectMatcherOptions` class that configures the road object matching behavior. Available through the `NavigationOptions` class. 
- Added `RouteCalloutUiStateProvider` class that allows to listen to Route Callout UI data. 
Normally, route callouts are drawn under the hood in NavSDK when this feature is enabled in `MapboxRouteLineApiOptions`.
However, there might be cases when app wants to only get the callout data from NavSDK and attach the DVA itself.
An example of such a case is using Mapbox Maps SDK Compose extensions: attaching a DVA for
Compose MapboxMap is done via [compose-specific API](https://docs.mapbox.com/android/maps/examples/compose/dynamic-view-annotations/),
which is not currently supported by NavSDK.
In this case you may listen to `RouteCalloutUiStateData` updates via `RouteCalloutUiStateProvider` and use its information by attach a DVA.
- Added experimental overloads for `MapboxManeuverApi#getRoadShields` and `MapboxRouteShieldApi#getRouteShields` that accept a `ShieldFontConfig` parameter, enabling custom font selection for route shields. 
- Added experimental `MapboxNavigationSVGExternalFileResolver` that can resolve fonts for SVG rendering from assets or use system fonts. 
- Updated `MapboxNavigation.replanRoute()` to now accept a new optional parameter of type `ReplanRoutesCallback`. 

#### Bug fixes and improvements
- Fix the bug that causes road cameras on alternative routes to be marked as passed but not removed from the map. 
- Fixed the incorrect order of callbacks when notifying about road cameras on the route. 

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.16.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.16.0-beta.1))
- Mapbox Navigation Native `v324.16.0-beta.1`
- Mapbox Core Common `v24.16.0-beta.1`
- Mapbox Java `v7.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.8.0))


## Navigation SDK Core Framework 3.12.0-beta.1 - 15 August, 2025
#### Features
- Added ability to filter by data source in EV charging station search operations. 
- Added `MapboxSpeedZoneInfo` class to represent speed zone information. Available through `UpcomingCamerasObserver::onSpeedZoneInfo` and `RoadCamerasMapCallback::onSpeedZoneInfo` callbacks. 
- Added experimental support for ADAS tiles in the predictive cache. See `PredictiveCacheNavigationOptions` for more information. 

#### Bug fixes and improvements
- Optimize the `MapboxRouteArrowView` to skip re-rendering arrows that have not changed. 
- Decrased excessively high GeoJSON buffer size from 128 to 32 to improve the memory footprint. 
- Avoid unnecessary navigation arrow GeoJSON updates 
- Optimized camera animations that involve significant zoom change. 
- Fixed an issue where the closer part of route line might have been overlapped by a farther part in case they covered the same space within a single leg (e. g. U-turns on narrow roads).  
- Don't reset the re-route request when on-route/off-route events are flaky. 
- Use the `enhancedLocation` in the RoadCamerasManager class to get a more accurate current speed for the vehicle. 


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Navigation-native `324.15.0-beta.2`
- Common SDK `24.15.0-beta.2`
- Maps SDK `11.15.0-beta.2`
- Android Search SDK `2.15.0-beta.2`

## Navigation SDK Core Framework 3.11.0-beta.1 - 04 July, 2025
#### Features
- Added support for Android 16 KB page-size devices. To consume SDK compatible with NDK 27 you need to add `-ndk27` suffix to the artifact name, for example, `com.mapbox.navigationcore:navigation` -> `com.mapbox.navigationcore:navigation-ndk27`. 
- Added method overload `TilesetDescriptorFactory#getLatest(Boolean)` that allows to specify whether to include ADAS tiles. 
- Extracted TTS functionality into a new module `audio`. 
- DR improvements - more robust models for GNSS trust, road calibration, and wheel speed trust; 
- Improve main thread utilization by removing unintended locks (visible on systems with overloaded CPU) 
- Disable the defaults for collection of tunnel/bridge subgraphs in free drive. The clients will need to specify explicitly which objects to collect via AlertServiceOptions in the public SDK interface. 
- Added support of immediate update of location puck bearing in [NavigationLocationProvider] in case of overlapping key points. 
- `RoadCamerasManager` in active guidance now relies on new `road_camera` Directions API annotation, which improves the performance of the camera data retrieval and quality of the data. 
- Added `MapboxRoadCamera::activeGuidanceInfo`, containing information about the route id, leg index, geometry index and step intersection of the camera in active guidance. 

#### Bug fixes and improvements
- Fixed an issue where after a reroute the vanishing point on the route line might have been ahead of the actual vehicle's position. 
- Added `HistoryRecorderOptions#shouldRecordRouteLineEvents` property to enable/disable route line events collection for manual recording (see `CopilotOptions#shouldRecordRouteLineEvents` for the same functionality with Copilot); it is disabled by default. 
- Fixed an issue where the Speed Camera notification would appear prematurely when the car's speed was 0. 
- Fix Route replayer: normalize bearing values to be in the range of [0..360) degrees. 
- Fixed a bug where alternative routes from `RoutesUpdatedResult#ignoredRoutes` were set to `RoutesUpdatedResult#navigationRoutes` after the first route progress update. 
- Fix when already passed part of route appears behind CCP 

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Java `v7.4.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.4.0))


## Navigation SDK Core Framework 3.10.0-beta.1 - 22 May, 2025
#### Features
- ⚠️ Breaking changes in Experimental API. `MapboxEvSearchClientFactory.#getInstance()` no longer accepts access token as a parameter. The default `MapboxOptions.accessToken` will be used. 
- `CarPlaceSearchOptions.accessToken` and corresponding builder function has been deprecated because `accessToken` is no longer in use as the search component now uses the default `MapboxOptions.accessToken`. 
- Used legacy/custom date primitives in EV modules to support older Android API levels. 
- Added `DriverNotification`, `DriverNotificationProvider` interfaces with `EvBetterRouteNotificationProvider` and `SlowTrafficNotificationProvider` implementations. Add new `DriverNotificationManager` API to attach or detach providers and `DriverNotificationManager.observeDriverNotification()` to handle the flow of driver notifications. 
- Added default location providers. 
- Added `EvBusyChargingStationNotificationProvider` to notify when the EV is charging station is busy and propose alternative route. 
- Added experimental `NavigationPerformance#performanceTracingEnabled` which enables/disables internal performance trace sections. 

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.13.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.13.0-beta.1))
- Mapbox Navigation Native `v324.13.0-beta.1`
- Mapbox Core Common `v24.13.0-beta.1`
- Mapbox Java `v7.4.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.4.0))


## Navigation SDK Core Framework 3.7.0-beta.1 - 22 January, 2025
#### Features
- Add `MapboxRoadCamera::inOnRoute` flag which indicates if the roiad camera is on the current route. 
- Add `MapboxRoadCamerasDisplayConfig::showOnlyOnRoute` config parameter to display only road cameras on the route. 
- Added support for EV charge point tariffs accessible via `EvStation.tariffs`. 
- New experimental property `LocationMatcherResult.correctedLocationData` is available. 
- New experimental function `GraphAccessor.getAdasisEdgeAttributes()` is available. It returns ADAS attributes for the requested edge. 
- Expose road type in the `MapboxRoadCamera` 
- Added support for section control speed cameras. 
- Extended `MapboxTripProgressApi` to provide information about time zone at leg/route destination. 
- Added `TripProgressUpdateFormatter.getEstimatedTimeToArrival` overload that formats ETA using a given time zone. 
- Added curvatures support on intersections in ADAS tiles 
- Reduced amount of error logs  
- Added periodic logs of Navigator/Cache configs 
- Added support for wheel speed usage during no signal simulation to determine passed distance for mobile profile 
- Improved off road transitions 
- :warning: Breaking changes in Experimental API `MapboxRouteCalloutView#renderCallouts(RouteCalloutData,MapboxRouteLineView)`. It's required to associate Route line with Callout View. 
- Added experimental `SearchAlongRouteUtils` class to optimize search along routes scenario by providing optimally selected points. 

#### Bug fixes and improvements
- Fixed a crash that happened on foreground service start on Android APIs 28 and below. 
- Deprecated EstimatedTimeToArrivalFormatter and introduced EstimatedTimeOfArrivalFormatter, which allows to format ETA with respect to destination time zone. 
- Deprecated TripProgressUpdateFormatter.estimatedTimeToArrivalFormatter and introduced TripProgressUpdateFormatter.estimatedTimeOfArrivalFormatter, which allows to format ETA with respect to destination time zone. 
- Fixed `MapboxNavigationSDKInitializerImpl` logic so that `uxfKey` is properly retrieved and sent over. 
- Fixed the condition for verifying the last good signal state in the offroad detection logic 
- Fixed incorrect calculation of a "missing part" of the route causing all lanes to be mark as divergent 
- Fixed EHorizon rural road objects sometimes marked as urban 
- Fixed a bug that happened during reroute in case if initial route was requested with `approaches` option specified. 

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.10.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.10.0-beta.1))
- Mapbox Navigation Native `v323.0.0-beta.2`
- Mapbox Core Common `v24.10.0-beta.2`
- Mapbox Java `v7.3.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.3.1))


## Navigation SDK Core Framework 3.6.0-beta.1 - 29 November, 2024
#### Features
- Added experimental `RoutingTilesOptions#hdTilesOptions` to configure HD tiles endpoint. 
- `DataInputsManager` now can be used from any thread. 
- Added experimental Road Cameras modules to provide notifications about road cameras along the route and show them on the map. 
- Added option to display the route line with a blur effect. 
- Added experimental functions `MapboxNavigation#startTripSessionWithPermissionCheck()` and `MapboxNavigation#startReplayTripSessionWithPermissionCheck` that immediately throw `IllegalStateException` if they are called with `withForegroundService` parameter set to true, but Android foreground service permissions requirements are not met. 

#### Bug fixes and improvements
- Improved reroute and alternative routes behavior 
- Fixed map matching bug after leaving a tunnel 
- Increased route stickiness in dead reckoning mode 
- Added ability to send raw unfused GNSS location in addition to fused one 
- Improved odometry and road graph fusing in urban canyons 
- Signature of experimental `RawGnssSatelliteData` has been changed, now it requires `residual` as a constructor parameter 
- Experimental `RawGnssLocation` type has been removed, now `RawGnssData` requires `DilutionOfPrecision` as a parameter 
- Now service type is specified explicitly when foreground location service starts. 

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.9.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.9.0-beta.1))
- Mapbox Navigation Native `v322.0.0-beta.1`
- Mapbox Core Common `v24.9.0-beta.1`
- Mapbox Java `v7.3.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.3.1))


## Navigation SDK Core Framework 3.5.0-beta.1 - 17 October, 2024
#### Features
- Added support for SVG junction views, see `MapboxJunctionApi#generateJunction(instructions: BannerInstructions, @JunctionViewFormat format: String, consumer: MapboxNavigationConsumer<Expected<JunctionError, JunctionViewData>>)`. [#6803](https://github.com/mapbox/mapbox-navigation-android/pull/6803)
- Added experimental `NavigationRoute#routeRefreshMetadata` which contains data related to refresh of the route object. [#6736](https://github.com/mapbox/mapbox-navigation-android/pull/6736)

#### Bug fixes and improvements
- Nav SDK now removes passed alternative routes as soon as user passed fork point. [#6813](https://github.com/mapbox/mapbox-navigation-android/pull/6813)
- Fixed a potential route line layers visibility race, which might have happened if you invoked `MapboxRouteLineView#showPrimaryRoute` and `MapboxRouteLineView#renderRouteDrawData` approximately at the same time.  [#6751](https://github.com/mapbox/mapbox-navigation-android/pull/6751)
- Optimized CA routes handling by skiping route parsing if it's already exist in direction session    [#6868](https://github.com/mapbox/mapbox-navigation-android/pull/6868)
- Fixed `CarSearchLocationProvider` produces _NullPointerException_ when using Mapbox Search SDK.  [#6702](https://github.com/mapbox/mapbox-navigation-android/pull/6702)
- Fixed an issue preventing Copilot from correctly recording history events.   [#6787](https://github.com/mapbox/mapbox-navigation-android/pull/6787)
- Improved reroute and alternative routes behavior [#6989](https://github.com/mapbox/mapbox-navigation-android/pull/6989)

#### Known issues :warning:


#### Other changes
- Changed `AutoArrivalController`: moves to a next waypoint immediately.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.8.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.8.0-beta.1))
- Mapbox Navigation Native `v321.0.0-beta.1`
- Mapbox Core Common `v24.8.0-beta.1`
- Mapbox Java `v7.3.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.3.1))


## Navigation SDK Core Framework 3.4.0-beta.1 - 05 September, 2024
#### Features
- Signature of experimental `EtcGateApi#updateEtcGateInfo` function has been changed, now it accepts `EtcGateApi.EtcGateInfo` as a function parameter. [#6508](https://github.com/mapbox/mapbox-navigation-android/pull/6508)
- Experimental Data Inputs functionality has been removed from the `core` module to a separate `datainputs` module (`MapboxNavigation#dataInputs` and everything from the package `com.mapbox.navigation.datainputs` have been removed). [Contact us](https://www.mapbox.com/support) to get more information on how to get access to the module. [#6508](https://github.com/mapbox/mapbox-navigation-android/pull/6508)
- Experimental Adasis functionality has been removed from the `core` module (`MapboxNavigation`'s functions `setAdasisMessageObserver`, `resetAdasisMessageObserver`, `updateExternalSensorData`, and `GraphAccessor#getAdasisEdgeAttributes` have been removed). [Contact us](https://www.mapbox.com/support) in case you're interested in ADASIS functionality. [#6508](https://github.com/mapbox/mapbox-navigation-android/pull/6508)
- Added experimental `RoutingTilesOptions#fallbackNavigationTilesVersion` which lets define version of navigation tiles to fallback in case of offline routing failure with navigation tiles defined in `RoutingTilesOptions#tilesVersion`. [#6475](https://github.com/mapbox/mapbox-navigation-android/pull/6475)
- Added experimental `MapboxRouteLineViewOptions#fadeOnHighZoomsConfig` and `MapboxRouteArrowOptions#fadeOnHighZoomsConfig` to configure smooth fading out of route line or/and arrows on high zoom levels. [#6367](https://github.com/mapbox/mapbox-navigation-android/pull/6367)
- The `PredictiveCacheController(PredictiveCacheOptions)` constructor is now deprecated. Use `PredictiveCacheController(MapboxNavigation, PredictiveCacheOptions)` instead. [#6376](https://github.com/mapbox/mapbox-navigation-android/pull/6376)
- Added `NavigationScaleGestureHandlerOptions#followingRotationAngleThreshold` that define threshold angle for rotation for `FOLLOWING` Navigation Camera state. [#6234](https://github.com/mapbox/mapbox-navigation-android/pull/6234)
- Added the ability to filter road names based on the system language [#6163](https://github.com/mapbox/mapbox-navigation-android/pull/6163)
- `com.mapbox.navigation.base.road.model.RoadComponent` objects that contain only slashes in their text are filtered out [#6163](https://github.com/mapbox/mapbox-navigation-android/pull/6163)
- Now `EHorizonResultType.Type` has a new element called `EHorizonResultType.NOT_AVAILABLE`. [#6290](https://github.com/mapbox/mapbox-navigation-android/pull/6290)
- Old `MapboxNavigation.postUserFeedback()` functions have been deprecated, use an overloading that accepts `UserFeedback` as a parameter. [#5781](https://github.com/mapbox/mapbox-navigation-android/pull/5781)
- Introduce MapboxRouteCalloutApi and MapboxRouteCalloutView to attach callouts to route lines with info about duration  [#2743](https://github.com/mapbox/mapbox-navigation-android/pull/2743)

#### Bug fixes and improvements
- Fixed a bug causing some history files recorded during the previous app sessions not to be uploaded by the Copilot. [#6359](https://github.com/mapbox/mapbox-navigation-android/pull/6359)
- Fixed an issue where native memory was not being properly released after the `MapboxNavigation` object was destroyed. [#6376](https://github.com/mapbox/mapbox-navigation-android/pull/6376)
- Fixed the issue of unwanted rerouting occurring immediately after setting a new route [#6163](https://github.com/mapbox/mapbox-navigation-android/pull/6163)
- Fixed a crash caused by an overflow in the JNI global reference table. [#6290](https://github.com/mapbox/mapbox-navigation-android/pull/6290)
- Fixed an issue with vignettes in Romania and Bulgaria for offline routing when tolls are excluded. [#6290](https://github.com/mapbox/mapbox-navigation-android/pull/6290)
- Addressed several issues that occurred when switching to an alternative route. [#6290](https://github.com/mapbox/mapbox-navigation-android/pull/6290)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.7.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.7.0-beta.1))
- Mapbox Navigation Native `v318.0.0`
- Mapbox Core Common `v24.7.0-beta.2`
- Mapbox Java `v7.2.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.2.0))


## Navigation SDK Core Framework 3.3.0-beta.1 - 19 July, 2024
#### Features
- Optimized memory usage in Directions API model classes by interning frequently occurring strings in JSON. [#5854](https://github.com/mapbox/mapbox-navigation-android/pull/5854)
- Added experimental `MapboxNavigation#replanRoute` to handle cases when user changes route options during active guidance, [#5286](https://github.com/mapbox/mapbox-navigation-android/pull/5286)
for example enabling avoid ferries.
- Added `DataInputsManager` to allow the provision of data from external sensors to the navigator, see `MapboxNavigation.dataInputsManager`. Experimental `EtcGateInfo` has been moved to `com.mapbox.navigation.core.datainputs` package. `EtcGateApi` has been deprecated. [#5957](https://github.com/mapbox/mapbox-navigation-android/pull/5957)
- Removing the ExperimentalMapboxNavigationAPI flag for Search predictive cache. [#5615](https://github.com/mapbox/mapbox-navigation-android/pull/5615)
- [BREAKING CHANGE] `PredictiveCacheOptions.unrecognizedTilesetDescriptorOptions` has been renamed to `PredictiveCacheOptions.predictiveCacheSearchOptionsList`. Additionally, `PredictiveCacheUnrecognizedTilesetDescriptorOptions` has been renamed to `PredictiveCacheSearchOptions`. Now, only search-related options can be passed to `PredictiveCacheSearchOptions`. [#5244](https://github.com/mapbox/mapbox-navigation-android/pull/5244)
- Introduced experimental traffic adjustment mechanism during a drive and added `TrafficOverrideOptions` to control this feature [#2811](https://github.com/mapbox/mapbox-navigation-android/pull/2811)
- Changed `Alternatives` that deviate close to a destination point are removed before a fork is reached. [#5848](https://github.com/mapbox/mapbox-navigation-android/pull/5848)

#### Bug fixes and improvements
- Implementation of `RerouteController#registerRerouteStateObserver` now invokes observer immediately with current state instead of posting invocation to the main looper.  [#5286](https://github.com/mapbox/mapbox-navigation-android/pull/5286)
- Fixed UI jank caused by on-device TextToSpeech player. [#5638](https://github.com/mapbox/mapbox-navigation-android/pull/5638)
- Removed `PredictiveCacheController#removeSearchControllers` and `PredictiveCacheController#createSearchControllers`. Now search predictive cache controller is created and destroyed together with `PredictiveCacheController` instance if `PredictiveCacheOptions.predictiveCacheSearchOptionsList` is provided. [#5714](https://github.com/mapbox/mapbox-navigation-android/pull/5714)
- Improve performance when handling large road objects on the eHorizon's MPP. [#6014](https://github.com/mapbox/mapbox-navigation-android/pull/6014)
- Fixed `Routes` that origin is out of primary route cannot be added as alternatives. [#5848](https://github.com/mapbox/mapbox-navigation-android/pull/5848)

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.6.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.6.0-beta.1))
- Mapbox Navigation Native `v315.0.0`
- Mapbox Core Common `v24.6.0-beta.1`
- Mapbox Java `v7.1.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.1.0))


## Navigation SDK Core Framework 3.2.0-beta.1 - 13 June, 2024
#### Features
- Added `RerouteStrategyForMapMatchedRoutes` to `RerouteOptions`. Reroute option `enableLegacyBehaviorForMapMatchedRoute` was removed, use `NavigateToFinalDestination` strategy instead. [#5256](https://github.com/mapbox/mapbox-navigation-android/pull/5256)

#### Bug fixes and improvements
- Fixed a crash due to incorrect OpenLR input data [#5400](https://github.com/mapbox/mapbox-navigation-android/pull/5400)
- Fixed a bug with spinning smoothed coordinate [#5400](https://github.com/mapbox/mapbox-navigation-android/pull/5400)
- Fixed issue for calculating the trim-offset value for the vanishing route line feature when the current geometry index emitted by route progress is greater than the value expected.

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.5.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.5.0-beta.1))
- Mapbox Navigation Native `v312.0.0`
- Mapbox Core Common `v24.5.0-beta.4`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))


## Navigation SDK Core Framework 3.1.0-rc.1 - 17 May, 2024
#### Features
- Improved `MapboxNavigationViewportDataSource` behavior, it updates view port data only when map size is calculated which prevents from using incorrect data. [#5017](https://github.com/mapbox/mapbox-navigation-android/pull/5017)
- Optimized network consumption of Continuous Alternatives feature.

#### Bug fixes and improvements


#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.4.0-rc.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.4.0-rc.2))
- Mapbox Navigation Native `v309.0.0`
- Mapbox Core Common `v24.4.0-rc.2`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))


## Navigation SDK Core Framework 3.1.0-beta.1 - 07 May, 2024
#### Features
- Added `androidauto` module, that brings Navigation Android Auto SDK. The module was carried over from Nav SDK v2 and has been adapted to work with the Maps v11. [#4454](https://github.com/mapbox/mapbox-navigation-android/pull/4454)
- Optimized predictive cache usage. [#4601](https://github.com/mapbox/mapbox-navigation-android/pull/4601)
- Added a new option `PredictiveCacheMapsOptions.extraOptions` that allows to specify extra tileset descriptor options. [#4749](https://github.com/mapbox/mapbox-navigation-android/pull/4749)
- Added a new option `AdasisConfigProfileShortTypeOptions.historyAverageSpeed` which specifies whether `historyAverageSpeed` data should be generated in ADASis messages. [#4931](https://github.com/mapbox/mapbox-navigation-android/pull/4931)

#### Bug fixes and improvements
- Made RerouteState.Failed retryable in case reroute failed due to not having yet received a route progress for a new route that had just been set. [#4861](https://github.com/mapbox/mapbox-navigation-android/pull/4861)
- Added support for custom users in map matching requests. [#4808](https://github.com/mapbox/mapbox-navigation-android/pull/4808)
- Fixed an issue where there was no fallback to onboard router in some cases. Now we allow onboard routing in all cases except when the request was cancelled. [#4754](https://github.com/mapbox/mapbox-navigation-android/pull/4754)
- Fixed an issue related to cached ADAS data not being released. [#4754](https://github.com/mapbox/mapbox-navigation-android/pull/4754)
- Removed route alerts for border crossings in neutral waters. [#4754](https://github.com/mapbox/mapbox-navigation-android/pull/4754)
- Fixed a crash caused by filtered alternatives in cases where the primary route status is different from the incoming route status. [#4754](https://github.com/mapbox/mapbox-navigation-android/pull/4754)
- Fixed `PredictiveCacheLocationOptions::routeBufferRadiusInMeters` default value. [#4673](https://github.com/mapbox/mapbox-navigation-android/pull/4673)
- Fixed an issue where there was no fallback to onboard router in some cases. Now we allow onboard routing in all cases except when the request was cancelled. [#4931](https://github.com/mapbox/mapbox-navigation-android/pull/4931)
- Fixed an issue related to cached ADAS data not being released. [#4931](https://github.com/mapbox/mapbox-navigation-android/pull/4931)
- Removed route alerts for border crossings in neutral waters. [#4931](https://github.com/mapbox/mapbox-navigation-android/pull/4931)
- Fixed a crash caused by filtered alternatives in cases where the primary route status is different from the incoming route status. [#4931](https://github.com/mapbox/mapbox-navigation-android/pull/4931)
- Fixed context leak on Activity recreation [#4626](https://github.com/mapbox/mapbox-navigation-android/pull/4626)
- Improved alternative routes Navigation SDK generates during reroute. [#4831](https://github.com/mapbox/mapbox-navigation-android/pull/4831)

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.4.0-beta.3` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.4.0-beta.3))
- Mapbox Navigation Native `v308.0.0`
- Mapbox Core Common `v24.4.0-beta.3`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))


## Navigation SDK Core Framework 3.0.0 - 12 April, 2024
#### Features
- Modules structure (mostly UI components modules) has been significantly changed. See migration guide for more information https://docs.mapbox.com/android/navigation/guides/migration-from-v2/#artifacts
- Deprecated classes, functions and fields have been removed. See [Nav SDK v2 documentation](https://docs.mapbox.com/android/navigation/api/2.17.7/) for more information about missing parts and migration guides.
- `com.mapbox.navigation.core.reroute.NavigationRerouteController` has been merged with `com.mapbox.navigation.core.reroute.RerouteController`
- Introduced support of Mapbox Map Matching API, see `MapboxNavigation#requestMapMatching`. 
- Changed LocationOptions API. Now custom location providers that emit mocked locations are allowed. See documentation to get more information. 
- Added Advanced Driver Assistance Systems (ADAS) functionality as an experimental API. 
- Added `NavigationViewApi#recenterCamera()` that allows to programatically recenter the camera position as if the recenter button was clicked. 
- Introduced support for highlighting 3D buildings.
- Navigation Core Framework doesn't let creating `NavigationRoute` from json anymore. Use `MapboxNavigation#requestRoutes` and `MapboxNavigation#requestMapMatching` to request `NavigationRoute`s.
- Changed type of `RouterOrigin`. Now it's an annotation which defines possible string values for router origin. `RouterOrigin.Offboard` is renamed to `RouterOrigin.ONLINE`, `RouterOrigin.Onboard` is renamed to `RouterOrigin.OFFLINE`, `RouterOrigin.Custom` is removed.
- Navigation Core Framework automatically updates alternative routes and switches to an online alternative in case of an offline primary route. Register `RoutesObserver` to keep track of current routes. Use `MapboxNavigation#setContinuousAlternativesEnabled` to enable/disable automatic update.
- `NavigationRouteAlternativesObserver`, `RouteAlternativesObserver`, `NavigationRouteAlternativesRequestCallback`, `MapboxNavigation#registerRouteAlternativesObserver`, `MapboxNavigation#requestAlternativeRoutes` were removed in favour of automatic alternatives update.
- Added support for `RouteOptions#suppressVoiceInstructionLocalNames` to onboard router.
- Changed the semantics of `Location#timestamp` that you receive in `onLocationMatcherResult`, instead of being set to current system time it is now has location input time + lookahead (extrapolation) time.
- Route Line component has been refactored and significantly changed. See migration guide for more information: https://docs.mapbox.com/android/navigation/guides/migration-from-v2/#mapboxroutelineapi
- Removed `LongRoutesOptimisationOptions`. Navigation Core Framework is now optimized to handle long routes by default. [#3741](https://github.com/mapbox/mapbox-navigation-android/pull/3741)
- `RouterFailure.code` has been replaced with `RouterFailure.type`. See `RouterFailureType` to find all possible error types. [#3555](https://github.com/mapbox/mapbox-navigation-android/pull/3555)
- Added "mbx.RouteLine" history events. They will be collected automatically by NavSDK for both manual recorder and Copilot recorder when the recording is started.  [#3785](https://github.com/mapbox/mapbox-navigation-android/pull/3785)
  You can enable route line events collection for Copilot using `CopilotOptions#shouldRecordRouteLineEvents` option. It is disabled by default.
- Improved EHorizon path evaluation. [#3555](https://github.com/mapbox/mapbox-navigation-android/pull/3555)
- Added experimental property `NavigationRoute#responseOriginAPI` which describes API that Navigation CF used to generate data for `NavigationRoute`. `NavigationRoute#responseOriginAPI` could be used to form expectations from navigation experience given the fact that routes with `ResponseOriginAPI.MAP_MATCHING_API` has limited support currently. [#4010](https://github.com/mapbox/mapbox-navigation-android/pull/4010)
- Public data classes have been replaces with normal classes with generated `equals`, `hashCode`, `toString`. `copy` function and destructuring declarations are not available for affected classes. [#4142](https://github.com/mapbox/mapbox-navigation-android/pull/4142)
- Added support of seamless switch to an alternative route in case of deviation to the alternative route from a route received via `MapboxNavigation#requestMapMatching`. [#3972](https://github.com/mapbox/mapbox-navigation-android/pull/3972)

#### Bug fixes and improvements
- Made `MapboxNavigation` constructor and `onDestroy` internal. To create an instance of `MapboxNavigation` use `MapboxNavigationProvider#create`; to destroy it use `MapboxNavigationProvider#destroy`. [3274](https://github.com/mapbox/navigation/pull/3274)
- Fixed a bug with multiple instances of cache which resulted in excessive memory consumption. 
- Fixed an issue where reroute for multi-leg routes used to fail in case waypoint_names or waypoint_targets parameters were specified without an explicit waypoint_indices parameter. 
- Improved handling of no storage available during navigation tiles downloading. 
- Improved handling of invalid config in `DeviceProfile#customConfig`. 
- Fixed a native crash in E-Horizon implementation caused by internal race condition. 
- Made `MapboxReplayer` constructor public. 
- Removed `OnlineRouteAlternativesSwitch`. Use `NavigationRouteAlternativesObserver` to receive an online alternative for the current offline route. Unlike `OnlineRouteAlternativesSwitch`, `NavigationRouteAlternativesObserver` doesn't switch to an online alternative automatically. 
- Changed structure of `NavigationRoute`. Now it can represent routes received from Mapbox Map Matching API as well as Mapbox Directions API.
  `NavigationRoute#directionsResponse` has been removed. Use `NavigationRoute#waypoints` and `NavigationRoute#responseUUID` to access data which used to be available via `NavigationRoute#directionsResponse`.
  `NavigationRoute#routeOptions` has been removed. Try to utilise data available in `NavigationRoute`, for example instead of using coordinates from route options, use `NavigationRoute#waypoints`. Temporary property `NavigationRoute#evMaxCharge` has been added to access maximum possible charge for the vehicle the route was requested for instead of `navigationRoute.routeOptions.getUnrecognizedProperty("ev_max_charge")`.
  
- Improve GNSS jump detection for better pitch based map-matching in tunnels
- Eliminate border crossing object when moving to neutral waters and back
- Fixed leak of CarAppLifecycleOwner on every copilot start. [#3803](https://github.com/mapbox/mapbox-navigation-android/pull/3803)
- Resolved an issue where a crash could occur if telemetry sending settings were changed after creating `MapboxNavigation`. [#3900](https://github.com/mapbox/mapbox-navigation-android/pull/3900)
- Fixed an issue with vanishing route line not working for single-leg routes. [#3818](https://github.com/mapbox/mapbox-navigation-android/pull/3818)
- Eliminated waiting for online route in case of reroute if onboard is ready. For rerouting return the route that was calculated sooner will be returned, and the back-online feature is expected to handle the case of switching back to online. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Improved highway exits detection. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Improved passed alternatives handling. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Fixed an issue when onboard route calculation might have been cancelled if online router returned critical error. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Fixed an issue in processing ETC signals when distant projections where treated as valid map-matched locations. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Fixed leak of CarAppLifecycleOwner on every copilot start. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Started sending special type POSITION messages in OFF-ROAD mode. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Fixed ANR `at com.mapbox.common.LifecycleUtils.hasServiceRunningInForeground(LifecycleUtils.kt:25)`. [#dd](https://github.com/mapbox/mapbox-navigation-android/pull/dd)
- Added `RerouteOptions#enableLegacyBehaviorForMapMatchedRoute` which lets enable reroute logic from Nav SDK v2 for routes generated by Mapbox Map Matching API. [#4130](https://github.com/mapbox/mapbox-navigation-android/pull/4130)
- Changed reroute state transition in case of deviation from a primary route which is received via `MapboxNavigation#requestMapMatching` when no alternative routes available to switch to. Reroute state used to be `Idle`, but now performs the following transition: `Idle` -> `FetchingRoute` -> `Failed` -> `Idle`. [#3972](https://github.com/mapbox/mapbox-navigation-android/pull/3972)
- Made `RouteShieldError#url` and `RouteShieldOrigin#originalUrl` nullable. They can be null in case the request had been cancelled before the URL was formed. [#4164](https://github.com/mapbox/mapbox-navigation-android/pull/4164)
- Improved road shield rendering behaviour for long text: now the closest length-wise available icon will be downloaded, previously it had been fixed to a maximum of 6 characters. [#4164](https://github.com/mapbox/mapbox-navigation-android/pull/4164)
- Added a Copilot option that allows to provide a different user id for history recording context. [#4301](https://github.com/mapbox/mapbox-navigation-android/pull/4301)
- Fixed an issue where incidents and closures far ahead along the route might have disappeared after route refresh. [#4410](https://github.com/mapbox/mapbox-navigation-android/pull/4410)
- Made `RouteShieldError#url` and `RouteShieldOrigin#originalUrl` nullable. They can be null in case the request had been cancelled before the URL was formed. [#4164](https://github.com/mapbox/mapbox-navigation-android/pull/4164)
- Improved road shield rendering behaviour for long text: now the closest length-wise available icon will be downloaded, previously it had been fixed to a maximum of 6 characters. [#4164](https://github.com/mapbox/mapbox-navigation-android/pull/4164)
- Improved tunnel exit detection. [#4578](https://github.com/mapbox/mapbox-navigation-android/pull/4578)
- Internal dependencies updated, which includes removal of unwanted URLs from the binary. [#4578](https://github.com/mapbox/mapbox-navigation-android/pull/4578)
- Improved incidents behavior: far away incidents are kept after partial route refresh. [#4578](https://github.com/mapbox/mapbox-navigation-android/pull/4578)
- Improved incidents behavior: expired incidents are removed on route refresh. [#4578](https://github.com/mapbox/mapbox-navigation-android/pull/4578)
- Improved incidents behavior: incidents with incorrect endTime are not removed. [#4578](https://github.com/mapbox/mapbox-navigation-android/pull/4578)
- Fixed EV route parsing issue. [#4578](https://github.com/mapbox/mapbox-navigation-android/pull/4578)
- Fixed a crash that could happen on incorrect waypoints in the route response. [#4578](https://github.com/mapbox/mapbox-navigation-android/pull/4578)
- Fixed continuous alternatives bugs leading to excessive `getRoute` calls. [#4578](https://github.com/mapbox/mapbox-navigation-android/pull/4578)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.3.0))
- Mapbox Navigation Native `v305.0.0`
- Mapbox Core Common `v24.3.1`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))


## Navigation SDK Core Framework 3.0.0-rc.5 - 29 March, 2024
#### Features
- The new `ui-components` module is now available. This module offers UI components that were previously accessible in Nav SDK v2. [#4235](https://github.com/mapbox/mapbox-navigation-android/pull/4235)

#### Bug fixes and improvements
- Made `RouteShieldError#url` and `RouteShieldOrigin#originalUrl` nullable. They can be null in case the request had been cancelled before the URL was formed. [#4164](https://github.com/mapbox/mapbox-navigation-android/pull/4164)
- Improved road shield rendering behaviour for long text: now the closest length-wise available icon will be downloaded, previously it had been fixed to a maximum of 6 characters. [#4164](https://github.com/mapbox/mapbox-navigation-android/pull/4164)
- Added a Copilot option that allows to provide a different user id for history recording context. [#4301](https://github.com/mapbox/mapbox-navigation-android/pull/4301)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.2.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.2.2))
- Mapbox Navigation Native `v304.0.0`
- Mapbox Core Common `v24.2.3`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))


## Navigation SDK Core Framework 1.0.0-rc.3 - 01 March, 2024

#### Features
- Added new property `AdasisDataSendingConfig.treeTrailingLength` that allows to specify the trailing length of the path tree, relative to the map-matched position, in the ADASIS provider. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)

#### Bug fixes and improvements
- Resolved an issue where a crash could occur if telemetry sending settings were changed after creating `MapboxNavigation`. [#3900](https://github.com/mapbox/mapbox-navigation-android/pull/3900)
- Fixed an issue with vanishing route line not working for single-leg routes. [#3818](https://github.com/mapbox/mapbox-navigation-android/pull/3818)
- Eliminated waiting for online route in case of reroute if onboard is ready. For rerouting return the route that was calculated sooner will be returned, and the back-online feature is expected to handle the case of switching back to online. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Improved highway exits detection. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Improved passed alternatives handling. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Fixed an issue when onboard route calculation might have been cancelled if online router returned critical error. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Fixed an issue in processing ETC signals when distant projections where treated as valid map-matched locations. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Fixed leak of CarAppLifecycleOwner on every copilot start. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Fixed ADASIS generator errors related to the split edges. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Fixed ADAS cache tiles eviction mechanism. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Started handling "unlilimited" speed limits in ADASIS. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Started sending special type POSITION messages in OFF-ROAD mode. [#3968](https://github.com/mapbox/mapbox-navigation-android/pull/3968)
- Fixed ANR `at com.mapbox.common.LifecycleUtils.hasServiceRunningInForeground(LifecycleUtils.kt:25)`. [#dd](https://github.com/mapbox/mapbox-navigation-android/pull/dd)

#### Known issues :warning:


#### Other changes
- Improved performance of the `MapboxRouteLineView#renderRouteLineUpdates` function, especially when `MapboxRouteLineApiOptions#styleInactiveRouteLegsIndependently` is enabled.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.2.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.2.0))
- Mapbox Navigation Native `v303.0.0`
- Mapbox Core Common `v24.2.0`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))


## Navigation SDK Core Framework 1.0.0-rc.2 - 16 February, 2024

#### Features
- Removed `LongRoutesOptimisationOptions`. Navigation Core Framework is now optimized to handle long routes by default. [#3741](https://github.com/mapbox/mapbox-navigation-android/pull/3741)
- Added billing explanation logs. Now Navigation SDK explains in the logs why certain Active Guidance or Free Drive Trip session started/stopped/paused/resumed. Billing explanations have `[BillingExplanation]` prefix in the logcat. [#3803](https://github.com/mapbox/mapbox-navigation-android/pull/3803)
- Removed `NavigaitonRoute#deserializeFrom` and `NavigationRoute#serialize` from public API because it's too easy to misuse them and create a leak of resources. [#3767](https://github.com/mapbox/mapbox-navigation-android/pull/3767)
- `RouterFailure.code` has been replaced with `RouterFailure.type`. See `RouterFailureType` to find all possible error types. [#3555](https://github.com/mapbox/mapbox-navigation-android/pull/3555)
- Improve map-matching on elevated roads. [#3555](https://github.com/mapbox/mapbox-navigation-android/pull/3555)
- Added "mbx.RouteLine" history events. They will be collected automatically by NavSDK for both manual recorder and Copilot recorder when the recording is started.  [#3785](https://github.com/mapbox/mapbox-navigation-android/pull/3785)
  You can enable route line events collection for Copilot using `CopilotOptions#shouldRecordRouteLineEvents` option. It is disabled by default.
- Improved EHorizon path evaluation. [#3555](https://github.com/mapbox/mapbox-navigation-android/pull/3555)
- `libnav-ui` module has been renamed to `libnavigation-voice`. Package name has been changed to `com.mapbox.navigation.voice`.

#### Bug fixes and improvements
- Fixed leak of CarAppLifecycleOwner on every copilot start. [#3803](https://github.com/mapbox/mapbox-navigation-android/pull/3803)

#### Known issues :warning:


#### Other changes
- Added Polish translation for UI elements.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.2.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.2.0-rc.1))
- Mapbox Navigation Native `v302.0.0`
- Mapbox Core Common `v24.2.0-rc.2`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))


## Navigation SDK Core Framework 1.0.0-rc.1 - 06 February, 2024

#### Features
- Changed type of `RouterOrigin`. Now it's an annotation which defines possible string values for router origin.
- `RouterOrigin.Offboard` is renamed to `RouterOrigin.ONLINE`.
- `RouterOrigin.Onboard` is renamed to `RouterOrigin.OFFLINE`.
- `RouterOrigin.Custom` is removed.
- Navigation Core Framework automatically updates alternative routes and switches to an online alternative in case of an offline primary route. Register `RoutesObserver` to keep track of current routes. Use `MapboxNavigation#setContinuousAlternativesEnabled` to enable/disable automatic update.
- `NavigationRouteAlternativesObserver`, `RouteAlternativesObserver`, `NavigationRouteAlternativesRequestCallback`, `MapboxNavigation#registerRouteAlternativesObserver`, `MapboxNavigation#requestAlternativeRoutes` were removed in favour of automatic alternatives update.
- `libnavui-voice` module has been renamed to `libnavigation-voice`. Package name has been changed to `com.mapbox.navigation.voice`.
- Added support for `RouteOptions#suppressVoiceInstructionLocalNames` to onboard router.
- Changed the semantics of `Location#timestamp` that you receive in `onLocationMatcherResult`, instead of being set to current system time it is now has location input time + lookahead (extrapolation) time.
- Implement ADASIS message batching.
- Split `MapboxRouteLineOptions` into `MapboxRouteLineApiOptions` and `MapboxRouteLineViewOptions`.
- Split `MapboxRouteLineOptions#displayRestrictedRoadSections` into `MapboxRouteLineApiOptions#calculateRestrictedRoadSections` and `MapboxRouteLineViewOptions#displayRestrictedRoadSections`. You can have a set-up where some of your `MapboxRouteLineView`s display the restricted data and others don't. Set `MapboxRouteLineApiOptions#calculateRestrictedRoadSections` if at least one of your `MapboxRouteLineView`s will display the restricted data. Set `MapboxRouteLineViewOptions#displayRestrictedRoadSections` only to those views, who are going to display it.
- Moved:
1. `MapboxRouteLineOptions.Builder#withRouteLineBelowLayerId` to `MapboxRouteLineViewOptions.Builder#routeLineBelowLayerId`;
2. `MapboxRouteLineOptions.Builder#withTolerance` to `MapboxRouteLineViewOptions.Builder#tolerance`;
3. `MapboxRouteLineOptions.Builder#withVanishingRouteLineEnabled` to `MapboxRouteLineApiOptions.Builder#vanishingRouteLineEnabled`;
4. `MapboxRouteLineOptions#styleInactiveRouteLegsIndependently`, `MapboxRouteLineOptions#vanishingRouteLineEnabled` and `MapboxRouteLineOptions#vanishingRouteLineUpdateIntervalNano` to `MapboxRouteLineApiOptions`.
5. `MapboxRouteLineOptions#softGradientTransition`, `MapboxRouteLineOptions#displaySoftGradientForTraffic`, `MapboxROuteLineOptions#shareLineGeometrySources`, `MapboxRouteLineOptions#lineDepthOcclusionFactor`, `MapboxRouteLineOptions#waypointLayerIconOffset`, `MapboxRouteLineOptions#waypointLayerIconAnchor` and `MapboxRouteLineOptions#iconPitchAlignment` to `MapboxRouteLineViewOptions`.
- Removed `RouteLineResources` class:
1. `routeLineColorResources`, `originWaypointIcon`, `destinationWaypointIcon`, `restrictedRoadDashArray`, `restrictedRoadOpacity` and `restrictedRoadLineWidth` were moved to `MapboxRouteLineViewOptions`
2. `trafficBackfillRoadClasses` was moved to `MapboxRouteLineApiOptions`
3. `scaleExpression` properties were moved to `MapboxRouteLineViewOptions#scaleExpressions` wrapper of type `RouteLineScaleExpressions`
4. `roundedLineCap` property was removed
- Moved `congestionRange` properties from `RouteLineColorResources` to `MapboxRouteLineApiOptions`.
- Removed `MapboxRouteLineOptions#routeStyleDescriptors` option.
- Removed the possibility of modify and reading data from `RouteLineSetValue`, `RouteLineClearValue` and `RouteLineUpdateValue`. Do not use these classes on your side, just pass the objects between `MapboxRouteLineAPI` and `MapboxRouteLineView`.
- `MapboxRouteLineAPI#options` and `MapboxRouteLineView#options` properties are no longer public.
- Made `RouteLineExpressionProvider` and `RouteLineTrimExpressionProvider` internal.
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
- Split `RouteLineConfig#options` into `RouteLineConfig#apiOptions` and `RouteLineConfig#viewOptions`
- Added `RouteLineConfig#viewOptionsUpdates`.
- Removed `MapboxRouteLineApi#showRouteWithLegIndexHighlighted`, `MapboxRouteLineApi#setPrimaryTrafficColor` and `MapboxRouteLineApi#setAlternativeTrafficColor` methods.
- Changed `MapboxRouteLineApi#setVanishingOffset` method behavior in the following way: if the route had not been set to `MapboxRouteLineApi` prior to the point when `setVanishingOffset` was invoked, it will return an error now. Previously it used to return a value, which was, however, useless for rendering without the route.
- Fixed an issue when soft gradient was not applied correctly in `MapboxRouteLineApi#setVanishingOffset` result.

#### Bug fixes and improvements
- Fix adas tiles eviction algorithm
- Fixed a mapmatching issue where the position might have been snapped to the wrong part of a highway after leaving tunnel
- Improve GNSS jump detection for better pitch based map-matching in tunnels
- Eliminate border crossing object when moving to neutral waters and back

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.1.0))
- Mapbox Navigation Native `v301.0.1`
- Mapbox Core Common `v24.1.0`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))

## Navigation SDK Core Framework 1.0.0-beta.5 - 02 February, 2024

#### Features
- The `maneuver` module now offers only API related to maneuver logic. Views was removed from the module
- `MapboxRecenterButton`, `MapboxRouteOverviewButton`, `MapboxRoadNameView`, `MapboxCameraModeButton` have been removed from ui-maps module. `MapboxExtendableButtonLayoutBinding` has been removed from `ui-base`  module.
- New module `libnavigation-tripdata` is available. It accumulates core api from `libnavui-shield`, `libnavui-tripprogress`, `libnavui-maneuver`, `libnavui-speedlimit` modules, which have been removed.
- Introduced support for highlighting 3D buildings.
- Navigation Core Framework doesn't let creating `NavigationRoute` from json anymore. Use `MapboxNavigation#requestRoutes` and `MapboxNavigation#requestMapMatching` to request `NavigationRoute`s.
- Added `NavigationRoute#serialize` and `NavigationRoute#deserializeFrom` to support immediate transfer of `NavigationRoute` between applications and processes with the same Navigation Core Framework version. 

#### Bug fixes and improvements
- Fixed crash in CustomRouterRule caused by invalid url schema in request

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.1.0))
- Mapbox Navigation Native `v300.0.1`
- Mapbox Core Common `v24.1.0`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))


## Navigation SDK Core Framework 1.0.0-beta.4 - 26 January, 2024
### Changelog

#### Bug fixes and improvements
- Improved `CustomRouterRule` to work independently from device network setup. 
- Removed `CustomRouterRule#baseUrl`. Now `CustomRouterRule` intercepts all route and route refresh requests to Mapbox Directions API. 
- Fixed a bug with multiple instances of cache which resulted in excessive memory consumption. 
- Fixed an issue where reroute for multi-leg routes used to fail in case waypoint_names or waypoint_targets parameters were specified without an explicit waypoint_indices parameter. 
- Improved Map Matching to avoid false deviation of the location puck to a parallel street. 
- Improved handling of no storage available during navigation tiles downloading. 
- Improved handling of invalid config in `DeviceProfile#customConfig`. 
- Fixed a native crash in E-Horizon implementation caused by internal race condition. 
- Made `MapboxReplayer` constructor public. 
- Removed `OnlineRouteAlternativesSwitch`. Use `NavigationRouteAlternativesObserver` to receive an online alternative for the current offline route. Unlike `OnlineRouteAlternativesSwitch`, `NavigationRouteAlternativesObserver` doesn't switch to an online alternative automatically. 
- Changed structure of `NavigationRoute`. Now it can represent routes received from Mapbox Map Matching API as well as Mapbox Directions API.
  `NavigationRoute#directionsResponse` has been removed. Use `NavigationRoute#waypoints`, `NavigationRoute#responseUUID`, and `NavigationRoute#responseMetadata` to access data which used to be available via `NavigationRoute#directionsResponse`.
  `NavigationRoute#routeOptions` has been removed. Try to utilise data available in `NavigationRoute`, for example instead of using coordinates from route options, use `NavigationRoute#waypoints`. Temporary property `NavigationRoute#evMaxCharge` has been added to access maximum possible charge for the vehicle the route was requested for instead of `navigationRoute.routeOptions.getUnrecognizedProperty("ev_max_charge")`.
  
- Added `MapMatchingSuccessfulResult#navigationRoutes` which represent routes that could be set to navigator using `MapboxNavigation#setNavigationRoutes`. 
- Changed type of `MapMatchingSuccessfulResult#matches` from `List<NavigationRoute>` to `List<MapMatchingMatch>`. Now every item from `MapMatchingSuccessfulResult#matches` represents a Match Object from Mapbox Map Matching API. 

## Features
- Changed LocationOptions API. Now custom location providers that emit mocked locations are allowed. In order to set a custom location provider, you now need to invoke `LocationOptions.Builder#locationProviderFactory` and pass a factory that will create a DeviceLocationProvider implementation based on a request parameter together will location provider type, which can be one of: REAL, MOCKED, MIXED. Note that if your provider can emit non-real locations, it must set `isMock` extra flag value to true for such locations. To set this flag, use:
```
Location.Builder#extra(Value.valueOf(hashMapOf(LocationExtraKeys.IS_MOCK to Value.valueOf(true/false))))
```
 
- Added Advanced Driver Assistance Systems (ADAS) functionality as an experimental API. 
- Added `NavigationViewApi#recenterCamera()` that allows to programatically recenter the camera position as if the recenter button was clicked. 

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.1.0))
- Mapbox Navigation Native `v300.0.1`
- Mapbox Core Common `24.1.0`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))


## Navigation SDK Core Framework 1.0.0-beta.3 - 19 January, 2024
### Changelog

#### Bug fixes and improvements
- Support more options for `MapMatchingOptions`. 
- Made `MapboxNavigation` constructor and `onDestroy` internal. To create an instance of `MapboxNavigation` use `MapboxNavigationProvider#create`; to destroy it use `MapboxNavigationProvider#destroy`. [3274](https://github.com/mapbox/navigation/pull/3274)

## Features
- Introduced support of Mapbox Map Matching API, see `MapboxNavigation#requestMapMatching`. 

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.1.0))
- Mapbox Navigation Native `v300.0.1`
- Mapbox Core Common `24.1.0`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))
