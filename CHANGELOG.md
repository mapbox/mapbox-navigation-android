# Changelog for the Mapbox Navigation SDK for Android

Mapbox welcomes participation and contributions from everyone.

## Unreleased
#### Features
#### Bug fixes and improvements
- Fixed an issue where `RoutingTilesOptions#tilesBaseUri` was used as a base url for route refresh requests instead of `RouteOptions#baseUrl`.
- Fixed an ANR caused by Copilot processing long routes. [#6984](https://github.com/mapbox/mapbox-navigation-android/pull/6984)
- Improved inactive leg independent styling: now the inactive leg will be styled differently right away, not only when the route progress updates begin.

## Mapbox Navigation SDK 2.7.5 - 15 June, 2023
### Changelog
[Changes between v2.7.4 and v2.7.5](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.4...v2.7.5)

#### Features


#### Bug fixes and improvements

- Fixed an issue where `RoutingTilesOptions#tilesBaseUri` was used as a base url for route refresh requests instead of `RouteOptions#baseUrl`.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v10.7.0))
- Mapbox Navigation Native `v111.2.0`
- Mapbox Core Common `v22.1.0`
- Mapbox Java `v6.7.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.7.0))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))


## Mapbox Navigation SDK 2.7.4 - 06 March, 2023
### Changelog
[Changes between v2.7.3 and v2.7.4](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.3...v2.7.4)

#### Features


#### Bug fixes and improvements

- Fixed an ANR caused by Copilot processing long routes. [#6984](https://github.com/mapbox/mapbox-navigation-android/pull/6984)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v10.7.0))
- Mapbox Navigation Native `v111.1.1`
- Mapbox Core Common `v22.1.0`
- Mapbox Java `v6.7.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.7.0))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))


## Mapbox Navigation SDK 2.7.3 - 27 January, 2023
### Changelog
[Changes between v2.7.2 and v2.7.3](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.2...v2.7.3)

#### Bug fixes and improvements
- Fixed some internal Copilot-related issues and added minor improvements.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v10.7.0))
- Mapbox Navigation Native `v111.1.1`
- Mapbox Core Common `v22.1.0`
- Mapbox Java `v6.7.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.7.0))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))


## Mapbox Navigation SDK 2.7.2 - 10 January, 2023
### Changelog
[Changes between v2.7.1 and v2.7.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.1...v2.7.2)

#### Features
- Added public-preview Copilot feature.

#### Bug fixes and improvements
- Ensure map-matching considers HOV-only roads as auto accessible.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v10.7.0))
- Mapbox Navigation Native `v111.1.1`
- Mapbox Core Common `v22.1.0`
- Mapbox Java `v6.7.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.7.0))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))


## Mapbox Navigation SDK 2.7.1 - 12 December, 2022
### Changelog
[Changes between v2.7.0 and v2.7.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.0...v2.7.1)

#### Features

#### Bug fixes and improvements
- Fixed an issue where `RouteProgress#BannerInstructions` could've become `null` when `MapboxNavigation#updateLegIndex` was called. [#6684](https://github.com/mapbox/mapbox-navigation-android/pull/6684)
- Fixed an issue where `RouteProgress#VoiceInstructions` could've become `null` when `MapboxNavigation#updateLegIndex` was called. [#6689](https://github.com/mapbox/mapbox-navigation-android/pull/6689)
- Fixed an issue where `RouteProgress#BannerInstructions` could've become `null` when setting alternative routes. [#6715](https://github.com/mapbox/mapbox-navigation-android/pull/6715)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v10.7.0))
- Mapbox Navigation Native `v111.0.0`
- Mapbox Core Common `v22.1.0`
- Mapbox Java `v6.7.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.7.0))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))


## Mapbox Navigation SDK 2.7.0 - 17 August, 2022
### Changelog
[Changes between v2.6.0 and v2.7.0](https://github.com/mapbox/mapbox-navigation-android/compare/v2.6.0...v2.7.0)

#### Features
- Introduced `ViewOptionsCustomization.isInfoPanelHideable` that allows control over whether the `NavigationView` Info Panel can hide when it is swiped down. [#6132](https://github.com/mapbox/mapbox-navigation-android/pull/6132)
- Introduced `ViewOptionsCustomization.infoPanelForcedState` that allows overriding of the `NavigationView` Info Panel (BottomSheetBehaviour) state. [#6132](https://github.com/mapbox/mapbox-navigation-android/pull/6132)
- Added `FollowingFrameOptions.focalPoint` that can be used to define the position of the first framed geometry point (typically the user location indicator, if available) in the `MapboxNavigationViewportDataSource.followingPadding`. [#5875](https://github.com/mapbox/mapbox-navigation-android/pull/5875)
- Added `ViewBinderCustomization.infoPanelBinder` that allows installation of a custom info panel layout in `NavigationView`. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
- Added `ViewStyleCustomization.infoPanelPeekHeight` that allows customization of `NavigationView` info panel bottom sheet peek height. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
- Added `ViewStyleCustomization.infoPanelMarginStart`, `ViewStyleCustomization.infoPanelMarginEnd`, `ViewStyleCustomization.infoPanelBackground` that allows customization of a `NavigatioView` default info panel margins and background. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
- Introduced new API(s) `MapboxTripProgressApi#getTripDetails()`, `MapboxTripProgressView#renderTripOverview()` and `MapboxTripProgressView#renderLegOverview()` to allow users to visualize trip related details for the entire route or a given leg of a route, before starting active navigation. [#6068](https://github.com/mapbox/mapbox-navigation-android/pull/6068)
- Added `MapboxNavigation.requestRoadGraphDataUpdate` method and `RoadGraphDataUpdateCallback` class to request road graph data update and recreate `MapboxNavigation` instance in the callback to be able to use latest data even if the application lifecycle is very long. [#6044](https://github.com/mapbox/mapbox-navigation-android/pull/6044)
- :warning: Deprecated `RoadObjectsOnRouteObserver` in favor of a new getter `NavigationRoute.upcomingRoadObjects` to get access to list of `UpcomingRoadObject`. [#6032](https://github.com/mapbox/mapbox-navigation-android/pull/6032)
- Added `amenities` to `RestStop`. [#6007](https://github.com/mapbox/mapbox-navigation-android/pull/6007)
- Introduced `NavigationViewOptions.showInfoPanelInFreeDrive` option that allows showing of the BottomSheet Info Panel when `NavigationView` is in the Free Drive state. [#6011](https://github.com/mapbox/mapbox-navigation-android/pull/6011)
- Added `VoiceInstructionsPlayerOptions.abandonFocusDelay` option that allows specifying a delay in milliseconds until the player abandons audio focus after playing all queued voice instructions. [#5969](https://github.com/mapbox/mapbox-navigation-android/pull/5969)
- Added refresh of alternatives routes. [#5923](https://github.com/mapbox/mapbox-navigation-android/pull/5923)
- Moved `MapboxCameraModeButton` to `libnavui-maps` module. Moved `MapboxExtendableButton` to `libnavui-base` module. Added new styles for `MapboxAudioGuidanceButton`, `MapboxCameraModeButton` and `MapboxExtendableButton` views. Those styles can be used to change the default button shape to "Oval," "Square" or "Circle" [#5962](https://github.com/mapbox/mapbox-navigation-android/pull/5962)
- Added `IncidentInfo#affectedRoadNames`. [#6008](https://github.com/mapbox/mapbox-navigation-android/pull/6008)

#### Bug fixes and improvements
- Updated `NavigationView` to render upcoming maneuvers. [#6175](https://github.com/mapbox/mapbox-navigation-android/pull/6175)
- Updated `NavigationView` to allow drawing of the info panel behind the translucent navigation bar. [#6145](https://github.com/mapbox/mapbox-navigation-android/pull/6145)
- Fixed an issue where `NavigationView` switches from Active Guidance to Free Drive state after rotating device when replay is enabled. [#6140](https://github.com/mapbox/mapbox-navigation-android/pull/6140)
- Fixed reroute for profiles other than driving/traffic. [#6146](https://github.com/mapbox/mapbox-navigation-android/pull/6146)
- Introduced `NavigationViewListener#onInfoPanelHidden` to inform user when `InfoPanel` hides. [#6113](https://github.com/mapbox/mapbox-navigation-android/pull/6113)
- Introduced `NavigationViewListener#onInfoPanelExpanded` to inform user when `InfoPanel` expands. [#6113](https://github.com/mapbox/mapbox-navigation-android/pull/6113)
- Introduced `NavigationViewListener#onInfoPanelCollapsed` to inform user when `InfoPanel` collapses. [#6113](https://github.com/mapbox/mapbox-navigation-android/pull/6113)
- Introduced `NavigationViewOptions.enableMapLongClickIntercept` that would allow users to disable `NavigationView` from handling `OnMapLongClick` events. [#6116](https://github.com/mapbox/mapbox-navigation-android/pull/6116)
- Introduced `MapViewObserver#onAttached` and `MapViewObserver#onDetached` to get access to `MapView` instance used by `NavigationView`. [#6116](https://github.com/mapbox/mapbox-navigation-android/pull/6116)
- Removed `NavigationViewListener.onMapStyleChanged`. [#6116](https://github.com/mapbox/mapbox-navigation-android/pull/6116)
- Added `ComponentInstaller` to `Maneuver` and `SpeedLimit` that offer simplified integration of maneuvers and speed limits APIs. [#6117](https://github.com/mapbox/mapbox-navigation-android/pull/6117)
- Fixed bearing calculation error during tunnel dead reckoning. [#6118](https://github.com/mapbox/mapbox-navigation-android/pull/6118)
- Updated `MapboxRouteLineApiExtensions` so that when coroutine scope calling the suspend functions is canceled, `MapboxRouteLineApi::cancel` is called. [#6094](https://github.com/mapbox/mapbox-navigation-android/pull/6094)
- :warning: Fixed an issue where `RoutesObserver` would be called with the previous routes set upon registration while a new routes set was already being processed. Now, the observer waits for the processing of `MapboxNavigation#setNavigationRoutes` to finish before delivering the result. [#6079](https://github.com/mapbox/mapbox-navigation-android/pull/6079)
- Remove the `MapView` from the `RouteArrowComponent` so that it can be used by Android Auto. [#6053](https://github.com/mapbox/mapbox-navigation-android/pull/6053)
- Enabled tunnel dead reckoning drift compensation by default for Auto profile. [#6061](https://github.com/mapbox/mapbox-navigation-android/pull/6061)
- Increased route line stickiness for Auto profile. [#6061](https://github.com/mapbox/mapbox-navigation-android/pull/6061)
- Improved off-road detection for Auto profile by relying more on the heading change relative to the road. [#6061](https://github.com/mapbox/mapbox-navigation-android/pull/6061)
- Added `PredictiveCacheController` constructor with `PredictiveCacheOptions`, `PredictiveCacheController(PredictiveCacheOptions)`. [#6071](https://github.com/mapbox/mapbox-navigation-android/pull/6071)
- Deprecated constructors `PredictiveCacheController(PredictiveCacheLocationOptions, PredictiveCacheControllerErrorHandler)` and `PredictiveCacheController(PredictiveCacheLocationOptions, PredictiveCacheLocationOptions, PredictiveCacheControllerErrorHandler)`. [#6071](https://github.com/mapbox/mapbox-navigation-android/pull/6071)
- Added `PredictiveCacheController#predictiveCacheControllerErrorHandler` to set and get `PredictiveCacheControllerErrorHandler`. [#6071](https://github.com/mapbox/mapbox-navigation-android/pull/6071)
- Added `PredictiveCacheMapsOptions`(map specific, that also allow to specify zoom levels for which the map tiles should be cached) and `PredictiveCacheNavigationOptions`(navigation specific) available through the `PredictiveCacheOptions`. [#6071](https://github.com/mapbox/mapbox-navigation-android/pull/6071)
- Reduced log entries related to getting map layers. [#6101](https://github.com/mapbox/mapbox-navigation-android/pull/6101)
- Changed `RouteOptionsUpdater` to use `snapping_include_closures=true` for origin of each re-route request. This resolves a situation when Nav SDK returned a route in an opposite direction or on a parallel road when a driver caused a re-route by entering a closed section of a road. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)
- :warning: Added checks to `DirectionsRoute#toNavigationRoute` and `NavigationRoute#toDirectionsRoute` mappers which restrict mapping `NavigationRoute` to `DirectionsRoute` and vice versa for some Directions API features and properties (currently including only preview EV routing features), because the `DirectionsRoute` cannot carry information necessary to support turn-by-turn navigation when these features are enabled. If you are using EV routing preview feature, make sure to only interact with `MapboxNavigation#requestRoutes(RouteOptions, NavigationRouterCallback)`, `MapboxNavigation#setNavigationRoutes(List<NavigationRoute>)`, and equivalent `NavigationRoute` APIs. [#6004](https://github.com/mapbox/mapbox-navigation-android/pull/6004)
- Adjusted the `RoutesSetCallback` API. [#6040](https://github.com/mapbox/mapbox-navigation-android/pull/6040)
- Updated `NavigationView` to reset `SharedApp` state when `MapboxNavigation`is destroyed. [#6039](https://github.com/mapbox/mapbox-navigation-android/pull/6039)
- Updated `NavigationView` to enable vanishing route line by default. Previous behaviour can be restored by setting `MapboxRouteLineOptions` with `vanishingRouteLineEnabled` flag set to `false` [#6055](https://github.com/mapbox/mapbox-navigation-android/pull/6055)
  ```kotlin
  navigationView.customizeViewOptions {
      routeLineOptions = ViewOptionsCustomization.defaultRouteLineOptions(context)
          .toBuilder(context)
          .withVanishingRouteLineEnabled(false)
          .build()
  }
  ```
- Made `rerouteController` argument in `MapboxNavigation#setRerouteController` nullable. Null can be passed to disable automatic rerouting. [#5977](https://github.com/mapbox/mapbox-navigation-android/pull/5977)
- Introduced `RoutesSetCallback` parameter to `MapboxNavigation#setNavigationRoutes`, which is called after the routes passed to `MapboxNavigation#setNavigationRoutes` are processed or are failed to be processed. [#5946](https://github.com/mapbox/mapbox-navigation-android/pull/5946)
- Changed the behaviour of `RoutesObserver`: `onRoutesChanged` method will not be triggered if the navigator fails to process routes passed via `MapboxNavigation#setNavigationRoutes`. [#5946](https://github.com/mapbox/mapbox-navigation-android/pull/5946)
- Fixed Attribution Icon position in `NavigationView`. [#6012](https://github.com/mapbox/mapbox-navigation-android/pull/6012)
- Fixed Toggle Camera Mode Button behavior in `NavigationView`. [#6014](https://github.com/mapbox/mapbox-navigation-android/pull/6014)
- Increased `AudioFocusDelegateProvider` visibility to public to allow instantiation of the default `AsyncAudioFocusDelegate`. [#5969](https://github.com/mapbox/mapbox-navigation-android/pull/5969)
- Fixed serialization of models with unrecognized properties. [#6021](https://github.com/mapbox/mapbox-navigation-android/pull/6021)
- Fixed the intermittent native crash caused during _Free Drive_ transition from _Active Guidance with alternatives. [#6034](https://github.com/mapbox/mapbox-navigation-android/pull/6034)
- :warning: Changed the default log level from `Debug` to `Info`. To change the level for logs produced by Mapbox SDKs use `LogConfiguration.setLoggingLevel(LoggingLevel)`. [#5987](https://github.com/mapbox/mapbox-navigation-android/pull/5987)
- Fixed reroute request interruption when setting the `NavigationRerouteController` [#5950](https://github.com/mapbox/mapbox-navigation-android/pull/5950).
- Fixed setting trim offsets to route line trail layers. [#5982](https://github.com/mapbox/mapbox-navigation-android/pull/5982)
- Fixed a Drop-In UI issue where legacy shields were displayed instead of Mapbox designed ones with some of the map styles. [#5984](https://github.com/mapbox/mapbox-navigation-android/pull/5984)
- Updated `NavigationView` to support edge-to-edge display. [#5976](https://github.com/mapbox/mapbox-navigation-android/pull/5976)
- Updated `MapboxSpeechApi` to use persistent cache to decrease the bandwidth consumption. [#5790](https://github.com/mapbox/mapbox-navigation-android/pull/5790)
- Updated `DefaultResourceLoader` offline behaviour to return resources from the disk cache when available. [#5970](https://github.com/mapbox/mapbox-navigation-android/pull/5970)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.7.0))
- Mapbox Navigation Native `v111.0.0`
- Mapbox Core Common `v22.1.0`
- Mapbox Java `v6.7.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.7.0))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))
- Mapbox Android Telemetry `v8.1.5` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/telem-8.1.5-core-5.0.2))


## Mapbox Navigation SDK 2.7.0-rc.2 - 11 August, 2022
### Changelog
[Changes between v2.7.0-rc.1 and v2.7.0-rc.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.0-rc.1...v2.7.0-rc.2)

#### Features
#### Bug fixes and improvements
- Fixed reroute for profiles other than driving/traffic. [#6146](https://github.com/mapbox/mapbox-navigation-android/pull/6146)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.7.0))
- Mapbox Navigation Native `v111.0.0`
- Mapbox Core Common `v22.1.0`
- Mapbox Java `v6.7.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.7.0-beta.1))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))
- Mapbox Android Telemetry `v8.1.5` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/telem-8.1.5-core-5.0.2))


## Mapbox Navigation SDK 2.7.0-rc.1 - 04 August, 2022
### Changelog
[Changes between v2.7.0-beta.3 and v2.7.0-rc.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.0-beta.3...v2.7.0-rc.1)

#### Features
- Added `FollowingFrameOptions.focalPoint` that can be used to define the position of the first framed geometry point (typically the user location indicator, if available) in the `MapboxNavigationViewportDataSource.followingPadding`. [#5875](https://github.com/mapbox/mapbox-navigation-android/pull/5875)

#### Bug fixes and improvements
- Introduced `NavigationViewListener#onInfoPanelHidden` to inform user when `InfoPanel` hides. [#6113](https://github.com/mapbox/mapbox-navigation-android/pull/6113)
- Introduced `NavigationViewListener#onInfoPanelExpanded` to inform user when `InfoPanel` expands. [#6113](https://github.com/mapbox/mapbox-navigation-android/pull/6113)
- Introduced `NavigationViewListener#onInfoPanelCollapsed` to inform user when `InfoPanel` collapses. [#6113](https://github.com/mapbox/mapbox-navigation-android/pull/6113)
- Introduced `NavigationViewOptions.enableMapLongClickIntercept` that would allow users to disable `NavigationView` from handling `OnMapLongClick` events. [#6116](https://github.com/mapbox/mapbox-navigation-android/pull/6116)
- Introduced `MapViewObserver#onAttached` and `MapViewObserver#onDetached` to get access to `MapView` instance used by `NavigationView`. [#6116](https://github.com/mapbox/mapbox-navigation-android/pull/6116)
- Removed `NavigationViewListener.onMapStyleChanged`. [#6116](https://github.com/mapbox/mapbox-navigation-android/pull/6116)
- Added `ComponentInstaller` to `Maneuver` and `SpeedLimit` that offer simplified integration of maneuvers and speed limits APIs. [#6117](https://github.com/mapbox/mapbox-navigation-android/pull/6117)
- Fixed bearing calculation error during tunnel dead reckoning. [#6118](https://github.com/mapbox/mapbox-navigation-android/pull/6118)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.7.0))
- Mapbox Navigation Native `v111.0.0`
- Mapbox Core Common `v22.1.0`
- Mapbox Java `v6.7.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.7.0-beta.1))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))
- Mapbox Android Telemetry `v8.1.5` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/telem-8.1.5-core-5.0.2))


## Mapbox Navigation SDK 2.7.0-beta.3 - 29 July, 2022
### Changelog
[Changes between v2.7.0-beta.2 and v2.7.0-beta.3](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.0-beta.2...v2.7.0-beta.3)

#### Bug fixes and improvements
- Updated `MapboxRouteLineApiExtensions` so that when coroutine scope calling the suspend functions is canceled, `MapboxRouteLineApi::cancel` is called. [#6094](https://github.com/mapbox/mapbox-navigation-android/pull/6094)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.7.0-rc.1))
- Mapbox Navigation Native `v110.0.0`
- Mapbox Core Common `v22.1.0-rc.1`
- Mapbox Java `v6.7.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.7.0-beta.1))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))
- Mapbox Android Telemetry `v8.1.5` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/telem-8.1.5-core-5.0.2))


## Mapbox Navigation SDK 2.7.0-beta.2 - 22 July, 2022
### Changelog
[Changes between v2.7.0-beta.1 and v2.7.0-beta.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.0-beta.1...v2.7.0-beta.2)

#### Features
- Added `ViewBinderCustomization.infoPanelBinder` that allows installation of a custom info panel layout in `NavigationView`. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
- Added `ViewStyleCustomization.infoPanelPeekHeight` that allows customization of `NavigationView` info panel bottom sheet peek height. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
- Added `ViewStyleCustomization.infoPanelMarginStart`, `ViewStyleCustomization.infoPanelMarginEnd`, `ViewStyleCustomization.infoPanelBackground` that allows customization of a `NavigatioView` default info panel margins and background. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
- Introduced new API(s) `MapboxTripProgressApi#getTripDetails()`, `MapboxTripProgressView#renderTripOverview()` and `MapboxTripProgressView#renderLegOverview()` to allow users to visualize trip related details for the entire route or a given leg of a route, before starting active navigation. [#6068](https://github.com/mapbox/mapbox-navigation-android/pull/6068)

#### Bug fixes and improvements
- :warning: Fixed an issue where `RoutesObserver` would be called with the previous routes set upon registration while a new routes set was already being processed. Now, the observer waits for the processing of `MapboxNavigation#setNavigationRoutes` to finish before delivering the result. [#6079](https://github.com/mapbox/mapbox-navigation-android/pull/6079)
- Remove the `MapView` from the `RouteArrowComponent` so that it can be used by Android Auto. [#6053](https://github.com/mapbox/mapbox-navigation-android/pull/6053)
- Enabled tunnel dead reckoning drift compensation by default for Auto profile. [#6061](https://github.com/mapbox/mapbox-navigation-android/pull/6061)
- Increased route line stickiness for Auto profile. [#6061](https://github.com/mapbox/mapbox-navigation-android/pull/6061)
- Improved off-road detection for Auto profile by relying more on the heading change relative to the road. [#6061](https://github.com/mapbox/mapbox-navigation-android/pull/6061)
- Added `PredictiveCacheController` constructor with `PredictiveCacheOptions`, `PredictiveCacheController(PredictiveCacheOptions)`. [#6071](https://github.com/mapbox/mapbox-navigation-android/pull/6071)
- Deprecated constructors `PredictiveCacheController(PredictiveCacheLocationOptions, PredictiveCacheControllerErrorHandler)` and `PredictiveCacheController(PredictiveCacheLocationOptions, PredictiveCacheLocationOptions, PredictiveCacheControllerErrorHandler)`. [#6071](https://github.com/mapbox/mapbox-navigation-android/pull/6071)
- Added `PredictiveCacheController#predictiveCacheControllerErrorHandler` to set and get `PredictiveCacheControllerErrorHandler`. [#6071](https://github.com/mapbox/mapbox-navigation-android/pull/6071)
- Added `PredictiveCacheMapsOptions`(map specific, that also allow to specify zoom levels for which the map tiles should be cached) and `PredictiveCacheNavigationOptions`(navigation specific) available through the `PredictiveCacheOptions`. [#6071](https://github.com/mapbox/mapbox-navigation-android/pull/6071)
- Reduced log entries related to getting map layers. [#6101](https://github.com/mapbox/mapbox-navigation-android/pull/6101)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.7.0-rc.1))
- Mapbox Navigation Native `v109.0.0`
- Mapbox Core Common `v22.1.0-rc.1`
- Mapbox Java `v6.7.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.7.0-beta.1))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))
- Mapbox Android Telemetry `v8.1.5` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/telem-8.1.5-core-5.0.2))


## Mapbox Navigation SDK 2.7.0-beta.1 - 14 July, 2022
### Changelog
[Changes between v2.7.0-alpha.3 and v2.7.0-beta.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.0-alpha.3...v2.7.0-beta.1)

#### Features
- Added `MapboxNavigation.requestRoadGraphDataUpdate` method and `RoadGraphDataUpdateCallback` class to request road graph data update and recreate `MapboxNavigation` instance in the callback to be able to use latest data even if the application lifecycle is very long. [#6044](https://github.com/mapbox/mapbox-navigation-android/pull/6044)

#### Bug fixes and improvements
- Changed `RouteOptionsUpdater` to use `snapping_include_closures=true` for origin of each re-route request. This resolves a situation when Nav SDK returned a route in an opposite direction or on a parallel road when a driver caused a re-route by entering a closed section of a road. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)
- :warning: Added checks to `DirectionsRoute#toNavigationRoute` and `NavigationRoute#toDirectionsRoute` mappers which restrict mapping `NavigationRoute` to `DirectionsRoute` and vice versa for some Directions API features and properties (currently including only preview EV routing features), because the `DirectionsRoute` cannot carry information necessary to support turn-by-turn navigation when these features are enabled. If you are using EV routing preview feature, make sure to only interact with `MapboxNavigation#requestRoutes(RouteOptions, NavigationRouterCallback)`, `MapboxNavigation#setNavigationRoutes(List<NavigationRoute>)`, and equivalent `NavigationRoute` APIs. [#6004](https://github.com/mapbox/mapbox-navigation-android/pull/6004)
- Adjusted the `RoutesSetCallback` API. [#6040](https://github.com/mapbox/mapbox-navigation-android/pull/6040)
- Updated `NavigationView` to reset `SharedApp` state when `MapboxNavigation`is destroyed. [#6039](https://github.com/mapbox/mapbox-navigation-android/pull/6039)
- Updated `NavigationView` to enable vanishing route line by default. Previous behaviour can be restored by setting `MapboxRouteLineOptions` with `vanishingRouteLineEnabled` flag set to `false` [#6055](https://github.com/mapbox/mapbox-navigation-android/pull/6055)
  ```kotlin
  navigationView.customizeViewOptions {
      routeLineOptions = ViewOptionsCustomization.defaultRouteLineOptions(context)
          .toBuilder(context)
          .withVanishingRouteLineEnabled(false)
          .build()
  }
  ```

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.7.0-beta.1))
- Mapbox Navigation Native `v108.0.1`
- Mapbox Core Common `v22.1.0-beta.1`
- Mapbox Java `v6.6.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.6.0))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))
- Mapbox Android Telemetry `v8.1.5` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/telem-8.1.5-core-5.0.2))


## Mapbox Navigation SDK 2.7.0-alpha.3 - July 8, 2022
### Changelog
[Changes between v2.7.0-alpha.2 and v2.7.0-alpha.3](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.0-alpha.2...v2.7.0-alpha.3)

#### Features
- :warning: Deprecated `RoadObjectsOnRouteObserver` in favor of a new getter `NavigationRoute.upcomingRoadObjects` to get access to list of `UpcomingRoadObject`. [#6032](https://github.com/mapbox/mapbox-navigation-android/pull/6032)
- Added `amenities` to `RestStop`. [#6007](https://github.com/mapbox/mapbox-navigation-android/pull/6007)
- Introduced `NavigationViewOptions.showInfoPanelInFreeDrive` option that allows showing of the BottomSheet Info Panel when `NavigationView` is in the Free Drive state. [#6011](https://github.com/mapbox/mapbox-navigation-android/pull/6011)
- Added `VoiceInstructionsPlayerOptions.abandonFocusDelay` option that allows specifying a delay in milliseconds until the player abandons audio focus after playing all queued voice instructions. [#5969](https://github.com/mapbox/mapbox-navigation-android/pull/5969)

#### Bug fixes and improvements
- Made `rerouteController` argument in `MapboxNavigation#setRerouteController` nullable. Null can be passed to disable automatic rerouting. [#5977](https://github.com/mapbox/mapbox-navigation-android/pull/5977)
- Introduced `RoutesSetCallback` parameter to `MapboxNavigation#setNavigationRoutes`, which is called after the routes passed to `MapboxNavigation#setNavigationRoutes` are processed or are failed to be processed. [#5946](https://github.com/mapbox/mapbox-navigation-android/pull/5946)
- Changed the behaviour of `RoutesObserver`: `onRoutesChanged` method will not be triggered if the navigator fails to process routes passed via `MapboxNavigation#setNavigationRoutes`. [#5946](https://github.com/mapbox/mapbox-navigation-android/pull/5946)
- Fixed Attribution Icon position in `NavigationView`. [#6012](https://github.com/mapbox/mapbox-navigation-android/pull/6012)
- Fixed Toggle Camera Mode Button behavior in `NavigationView`. [#6014](https://github.com/mapbox/mapbox-navigation-android/pull/6014)
- Increased `AudioFocusDelegateProvider` visibility to public to allow instantiation of the default `AsyncAudioFocusDelegate`. [#5969](https://github.com/mapbox/mapbox-navigation-android/pull/5969)
- Fixed serialization of models with unrecognized properties. [#6021](https://github.com/mapbox/mapbox-navigation-android/pull/6021)
- Fixed the intermittent native crash caused during _Free Drive_ transition from _Active Guidance with alternatives. [#6034](https://github.com/mapbox/mapbox-navigation-android/pull/6034)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.7.0-beta.1))
- Mapbox Navigation Native `v108.0.1`
- Mapbox Core Common `v22.1.0-beta.1`
- Mapbox Java `v6.6.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.6.0))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))
- Mapbox Android Telemetry `v8.1.4`([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/telem-8.1.4-core-5.0.2))

## Mapbox Navigation SDK 2.6.0 - July 7, 2022
### Changelog
[Changes between v2.5.1 and v2.6.0](https://github.com/mapbox/mapbox-navigation-android/compare/v2.5.1...v2.6.0)

#### Features
- Moved `MapboxCameraModeButton` to `libnavui-maps` module. Moved `MapboxExtendableButton` to `libnavui-base` module. Added new styles for `MapboxAudioGuidanceButton`, `MapboxCameraModeButton` and `MapboxExtendableButton` views. Those styles can be used to change the default button shape to "Oval," "Square" or "Circle" [#5962](https://github.com/mapbox/mapbox-navigation-android/pull/5962)
- Introduced `NavigationViewOptions.showInfoPanelInFreeDrive` option that allows showing of the BottomSheet Info Panel when `NavigationView` is in the Free Drive state. [#6011](https://github.com/mapbox/mapbox-navigation-android/pull/6011)
- Added `ViewBinderCustomization.customActionButtons` that allows installation of custom action buttons in `NavigationView` ActionBinder [#5937](https://github.com/mapbox/mapbox-navigation-android/pull/5937)
- Overloaded `PredictiveCacheController` `constructor` adding `predictiveCacheGuidanceLocationOptions` so that Maps / Navigation controller options can be configured separately. Noting that when using the secondary constructor `predictiveCacheLocationOptions` is used as `predictiveCacheGuidanceLocationOptions` to retain backwards compatibility. [#5927](https://github.com/mapbox/mapbox-navigation-android/pull/5927)
- Added an api for interacting with `NavigationView`. This API is experimental with the intention to become stable. [#5919](https://github.com/mapbox/mapbox-navigation-android/pull/5919)
- Added support for `NavigationViewListener`. This listener can be registered with `NavigationView` to observe changes to: navigation state, destination setting/clearing, Map `Style`, camera modes, camera viewport size, and audio guidance mute/un-mute state. [#5922](https://github.com/mapbox/mapbox-navigation-android/pull/5922)
- Added `MapboxNavigationApp#installComponents()` and `MapboxNavigation#installComponents()` APIs that offer simplified integration of voice, route line and route arrow APIs. These extensions allow to instantiate wrappers that automatically integrate `MapboxNavigation` with the selected components, taking care of data and lifecycle management. See documentation for `RouteLineComponent`, `RouteArrowComponent` and `AudioGuidanceButtonComponent`. [#5874](https://github.com/mapbox/mapbox-navigation-android/pull/5874)
- Added support for user feedbacks with custom types and subtypes. [#5915](https://github.com/mapbox/mapbox-navigation-android/pull/5915)
- :warning: Expired data in the current primary route is cleaned up if 3 consecutive refresh attempts fail. Congestion annotations become `"unknown"`. Numeric congestion annotations become `null`. Expired incidents disappear. [#5767](https://github.com/mapbox/mapbox-navigation-android/pull/5767).
- `RouteLineTrimOffset` exposed in `RouteLineDynamicData` to override the vanishing point offset produced by the `MapboxRouteLineApi`. [#5858](https://github.com/mapbox/mapbox-navigation-android/pull/5858)
- Added capabilities for Drop-In UI to render in landscape mode. [#5823](https://github.com/mapbox/mapbox-navigation-android/pull/5823)
- Added support for sharing multiple instances of `MapboxNavigationObserver`. [#5829](https://github.com/mapbox/mapbox-navigation-android/pull/5829)
- Added support for patches in Tile store. Tile store may download a patch to update an existing tile instead of downloading a new one. [#5861](https://github.com/mapbox/mapbox-navigation-android/pull/5861)

#### Bug fixes and improvements
- Fixed Attribution Icon position in `NavigationView` [#6012](https://github.com/mapbox/mapbox-navigation-android/pull/6012)
- Fixed Toggle Camera Mode Button behavior in `NavigationView`. [#6014](https://github.com/mapbox/mapbox-navigation-android/pull/6014)
- Fixed a Drop-In UI issue where legacy shields were displayed instead of Mapbox designed ones with some of the map styles. [#5984](https://github.com/mapbox/mapbox-navigation-android/pull/5984)
- Fixed setting trim offsets to route line trail layers. [#5982](https://github.com/mapbox/mapbox-navigation-android/pull/5982)
- Updated `NavigationView` to support edge-to-edge display. [#5976](https://github.com/mapbox/mapbox-navigation-android/pull/5976)
- Fixed user location indicator's velocity when `NavigationLocationProvider` is used together with `keyPoints`. [#5925](https://github.com/mapbox/mapbox-navigation-android/pull/5925)
- Fixed the issue with the close icon in the trip notification occasionally using wrong color when including ui-dropin dependency. [#5956](https://github.com/mapbox/mapbox-navigation-android/pull/5956)
- Added more callbacks to `NavigationViewListener` to allow for observing events related to fetching a route. [#5948](https://github.com/mapbox/mapbox-navigation-android/pull/5948)
- Fixed an issue where the default `NavigationCamera` transition to the `Following` state did not respect `NavigationCameraTransitionOptions#maxDuration`. [#5921](https://github.com/mapbox/mapbox-navigation-android/pull/5921)
- Added empty frames to Drop-In UI on the left and right side of the screen, thereby allowing users to inject custom views. [#5930](https://github.com/mapbox/mapbox-navigation-android/pull/5930)
- Improvements in route line layer management to eliminate blinking when alternative routes are recomputed and redrawn after passing a fork, in addition to improving performance by reducing route line layer redraws. [#5859](https://github.com/mapbox/mapbox-navigation-android/pull/5859)
- :warning: Moved `MapboxAudioGuidanceButton` from Drop-in UI to Voice module. [#5874](https://github.com/mapbox/mapbox-navigation-android/pull/5874)
- Changed `Onboard` router to fail fast and deliver an appropriate message when EV route requests are made, which are not supported yet. [#5905](https://github.com/mapbox/mapbox-navigation-android/pull/5905)
- Fixed an issue where offline route requests sometimes crashed the SDK. [#5905](https://github.com/mapbox/mapbox-navigation-android/pull/5905)
- :warning: Changed  default `NavigationCamera` transitions to use the `flyTo` animation when transitioning to `NavigationCameraState#FOLLOWING` mode. [#5871](https://github.com/mapbox/mapbox-navigation-android/pull/5871)
- Improved enhanced locations bearing changes calculation on corners with high frequency input signal. [#5878](https://github.com/mapbox/mapbox-navigation-android/pull/5878)
- Fixed off-road detection in unmapped underground garages. [#5878](https://github.com/mapbox/mapbox-navigation-android/pull/5878)
- :warning: Changed `SaveHistoryCallback` to fire on the main thread instead of a worker thread. [#5878](https://github.com/mapbox/mapbox-navigation-android/pull/5878)
- Fixed an issue where the hosting `LifecycleOwner` used to dictate the lifecycle of the `NavigationView` was taken from the `Activity` even if the view was embedded in a `Fragment`. [#5818](https://github.com/mapbox/mapbox-navigation-android/pull/5818)
- Added an option to use different `ViewModelStoreOwner`s with the `NavigationView` (for example the one hosted by a `Fragment`). [#5818](https://github.com/mapbox/mapbox-navigation-android/pull/5818)
- Reduced memory consumptions on startup by not decoding tiles in predictive cache and latest version controller. [#5848](https://github.com/mapbox/mapbox-navigation-android/pull/5847)
- Fixed an issue where the vanishing point of the primary route line was not always reset when new routes were drawn following a previous active navigation session. [#5842](https://github.com/mapbox/mapbox-navigation-android/pull/5842)
- Fixed `LocationMatcherResult#location#time`. Now it follows Android conventions, i.e. contains UTC time in milliseconds instead of elapsed time. [#5861](https://github.com/mapbox/mapbox-navigation-android/pull/5861)
- Fixed map-matching for some corner cases. [#5861](https://github.com/mapbox/mapbox-navigation-android/pull/5861)
- Improved off-route detection in case of a teleport. [#5861](https://github.com/mapbox/mapbox-navigation-android/pull/5861)
- Reduced cache misses in tile store. [#5861](https://github.com/mapbox/mapbox-navigation-android/pull/5861)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.6.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.6.0))
- Mapbox Navigation Native `v106.0.0`
- Mapbox Core Common `v22.0.0`
- Mapbox Java `v6.6.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.6.0))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))
- Mapbox Android Telemetry `v8.1.3`([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/telem-8.1.3-core-5.0.2))

## Mapbox Navigation SDK 2.7.0-alpha.2 - July 1, 2022
### Changelog
[Changes between v2.7.0-alpha.1 and v2.7.0-alpha.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.7.0-alpha.1...v2.7.0-alpha.2)

#### Features

- Added refresh of alternatives routes. [#5923](https://github.com/mapbox/mapbox-navigation-android/pull/5923)
- Moved `MapboxCameraModeButton` to `libnavui-maps` module. Moved `MapboxExtendableButton` to `libnavui-base` module. Added new styles for `MapboxAudioGuidanceButton`, `MapboxCameraModeButton` and `MapboxExtendableButton` views. Those styles can be used to change the default button shape to "Oval," "Square" or "Circle" [#5962](https://github.com/mapbox/mapbox-navigation-android/pull/5962)
- Added `IncidentInfo#affectedRoadNames`. [#6008](https://github.com/mapbox/mapbox-navigation-android/pull/6008)

#### Bug fixes and improvements

- :warning: Changed the default log level from `Debug` to `Info`. To change the level for logs produced by Mapbox SDKs use `LogConfiguration.setLoggingLevel(LoggingLevel)`. [#5987](https://github.com/mapbox/mapbox-navigation-android/pull/5987)
- Fixed reroute request interruption when setting the `NavigationRerouteController` [#5950](https://github.com/mapbox/mapbox-navigation-android/pull/5950).
- Fixed setting trim offsets to route line trail layers. [#5982](https://github.com/mapbox/mapbox-navigation-android/pull/5982)
- Fixed a Drop-In UI issue where legacy shields were displayed instead of Mapbox designed ones with some of the map styles. [#5984](https://github.com/mapbox/mapbox-navigation-android/pull/5984)
- Updated `NavigationView` to support edge-to-edge display. [#5976](https://github.com/mapbox/mapbox-navigation-android/pull/5976)

#### Known issues

:bangbang: We are observing an [intermittent native crash](https://github.com/mapbox/mapbox-navigation-android/issues/5985) with this pre-release version when starting an active guidance session and then stopping the session by clicking on device back button. The root cause is still unknown and it only seems to affect specific devices. We are working to understand the issue and a fix is expected ahead of the final v2.7 release.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.7.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.7.0-beta.1))
- Mapbox Navigation Native `v108.0.0`
- Mapbox Core Common `v22.1.0-beta.1`
- Mapbox Java `v6.6.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.6.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.2`

## Mapbox Navigation SDK 2.6.0-rc.2 - July 1, 2022
### Changelog
[Changes between v2.6.0-rc.1 and v2.6.0-rc.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.6.0-rc.1...v2.6.0-rc.2)

#### Bug fixes and improvements
- Fixed a Drop-In UI issue where legacy shields were displayed instead of Mapbox designed ones with some of the map styles. [#5984](https://github.com/mapbox/mapbox-navigation-android/pull/5984)
- Fixed setting trim offsets to route line trail layers. [#5982](https://github.com/mapbox/mapbox-navigation-android/pull/5982)
- Updated `NavigationView` to support edge-to-edge display. [#5976](https://github.com/mapbox/mapbox-navigation-android/pull/5976)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.6.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.6.0))
- Mapbox Navigation Native `v106.0.0`
- Mapbox Core Common `v22.0.0`
- Mapbox Java `v6.6.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.6.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.2`

## Mapbox Navigation SDK 2.7.0-alpha.1 - June 24, 2022
### Changelog
[Changes between v2.6.0-rc.1 and v2.7.0-alpha.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.6.0-rc.1...v2.7.0-alpha.1)

#### Bug fixes and improvements

- Updated `MapboxSpeechApi` to use persistent cache to decrease the bandwidth consumption. [#5790](https://github.com/mapbox/mapbox-navigation-android/pull/5790)
- Updated `DefaultResourceLoader` offline behaviour to return resources from the disk cache when available. [#5970](https://github.com/mapbox/mapbox-navigation-android/pull/5970)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.6.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.6.0))
- Mapbox Navigation Native `v107.0.0`
- Mapbox Core Common `v22.0.0`
- Mapbox Java `v6.5.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.2`

## Mapbox Navigation SDK 2.6.0-rc.1 - June 23, 2022
### Changelog
[Changes between v2.6.0-beta.3 and v2.6.0-rc.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.6.0-beta.3...v2.6.0-rc.1)

#### Features
- Added `ViewBinderCustomization.customActionButtons` that allows installation of custom action buttons in `NavigationView` ActionBinder [#5937](https://github.com/mapbox/mapbox-navigation-android/pull/5937)

#### Bug fixes and improvements
- Fixed user location indicator's velocity when `NavigationLocationProvider` is used together with `keyPoints`. [#5925](https://github.com/mapbox/mapbox-navigation-android/pull/5925)
- Fixed the issue with the close icon in the trip notification occasionally using wrong color when including ui-dropin dependency. [#5956](https://github.com/mapbox/mapbox-navigation-android/pull/5956)
- Added more callbacks to `NavigationViewListener` to allow for observing events related to fetching a route. [#5948](https://github.com/mapbox/mapbox-navigation-android/pull/5948)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.6.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.6.0))
- Mapbox Navigation Native `v106.0.0`
- Mapbox Core Common `v22.0.0`
- Mapbox Java `v6.5.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.2`

## Mapbox Navigation SDK 2.6.0-beta.3 - June 17, 2022
### Changelog
[Changes between v2.6.0-beta.2 and v2.6.0-beta.3](https://github.com/mapbox/mapbox-navigation-android/compare/v2.6.0-beta.2...v2.6.0-beta.3)

#### Features
- Overloaded `PredictiveCacheController` `constructor` adding `predictiveCacheGuidanceLocationOptions` so that Maps / Navigation controller options can be configured separately. Noting that when using the secondary constructor `predictiveCacheLocationOptions` is used as `predictiveCacheGuidanceLocationOptions` to retain backwards compatibility. [#5927](https://github.com/mapbox/mapbox-navigation-android/pull/5927)
- Added an api for interacting with `NavigationView`. This API is experimental with the intention to become stable. [#5919](https://github.com/mapbox/mapbox-navigation-android/pull/5919)
- Added support for `NavigationViewListener`. This listener can be registered with `NavigationView` to observe changes to: navigation state, destination setting/clearing, Map `Style`, camera modes, camera viewport size, and audio guidance mute/un-mute state. [#5922](https://github.com/mapbox/mapbox-navigation-android/pull/5922)

#### Bug fixes and improvements
- Fixed an issue where the default `NavigationCamera` transition to the `Following` state did not respect `NavigationCameraTransitionOptions#maxDuration`. [#5921](https://github.com/mapbox/mapbox-navigation-android/pull/5921)
- Added empty frames to Drop-In UI on the left and right side of the screen, thereby allowing users to inject custom views. [#5930](https://github.com/mapbox/mapbox-navigation-android/pull/5930)
- Improvements in route line layer management to eliminate blinking when alternative routes are recomputed and redrawn after passing a fork, in addition to improving performance by reducing route line layer redraws. [#5859](https://github.com/mapbox/mapbox-navigation-android/pull/5859)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.6.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.6.0))
- Mapbox Navigation Native `v106.0.0`
- Mapbox Core Common `v22.0.0`
- Mapbox Java `v6.5.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.2`

## Mapbox Navigation SDK 2.6.0-beta.2 - June 9, 2022
### Changelog
[Changes between v2.6.0-beta.1 and v2.6.0-beta.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.6.0-beta.1...v2.6.0-beta.2)

#### Features
- Added `MapboxNavigationApp#installComponents()` and `MapboxNavigation#installComponents()` APIs that offer simplified integration of voice, route line and route arrow APIs. These extensions allow to instantiate wrappers that automatically integrate `MapboxNavigation` with the selected components, taking care of data and lifecycle management. See documentation for `RouteLineComponent`, `RouteArrowComponent` and `AudioGuidanceButtonComponent`. [#5874](https://github.com/mapbox/mapbox-navigation-android/pull/5874)
- Added support for user feedbacks with custom types and subtypes. [#5915](https://github.com/mapbox/mapbox-navigation-android/pull/5915)

#### Bug fixes and improvements
- :warning: Moved `MapboxAudioGuidanceButton` from Drop-in UI to Voice module. [#5874](https://github.com/mapbox/mapbox-navigation-android/pull/5874)
- Changed `Onboard` router to fail fast and deliver an appropriate message when EV route requests are made, which are not supported yet. [#5905](https://github.com/mapbox/mapbox-navigation-android/pull/5905)
- Fixed an issue where offline route requests sometimes crashed the SDK. [#5905](https://github.com/mapbox/mapbox-navigation-android/pull/5905)
- :warning: Changed  default `NavigationCamera` transitions to use the `flyTo` animation when transitioning to `NavigationCameraState#FOLLOWING` mode. [#5871](https://github.com/mapbox/mapbox-navigation-android/pull/5871)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.6.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.6.0-rc.1))
- Mapbox Navigation Native `v105.0.0`
- Mapbox Core Common `v22.0.0-rc.2`
- Mapbox Java `v6.5.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.2`

## Mapbox Navigation SDK 2.5.1 - June 2, 2022
### Changelog
[Changes between v2.5.0 and v2.5.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.5.0...v2.5.1)

#### Bug fixes and improvements
- Fixed an issue where offline route requests sometimes crashed the SDK. [#5894](https://github.com/mapbox/mapbox-navigation-android/pull/5894)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.5.0))
- Mapbox Navigation Native `v101.0.1`
- Mapbox Core Common `v21.3.1`
- Mapbox Java `v6.5.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.6.0-beta.1 - June 2, 2022
### Changelog
[Changes between v2.6.0-alpha.2 and v2.6.0-beta.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.6.0-alpha.2...v2.6.0-beta.1)

#### Features
- :warning: Expired data in the current primary route is cleaned up if 3 consecutive refresh attempts fail. Congestion annotations become `"unknown"`. Numeric congestion annotations become `null`. Expired incidents disappear. [#5767](https://github.com/mapbox/mapbox-navigation-android/pull/5767).

#### Bug fixes and improvements
- Improved enhanced locations bearing changes calculation on corners with high frequency input signal. [#5878](https://github.com/mapbox/mapbox-navigation-android/pull/5878)
- Fixed off-road detection in unmapped underground garages. [#5878](https://github.com/mapbox/mapbox-navigation-android/pull/5878)
- :warning: Changed `SaveHistoryCallback` to fire on the main thread instead of a worker thread. [#5878](https://github.com/mapbox/mapbox-navigation-android/pull/5878)
- Fixed an issue where the hosting `LifecycleOwner` used to dictate the lifecycle of the `NavigationView` was taken from the `Activity` even if the view was embedded in a `Fragment`. [#5818](https://github.com/mapbox/mapbox-navigation-android/pull/5818)
- Added an option to use different `ViewModelStoreOwner`s with the `NavigationView` (for example the one hosted by a `Fragment`). [#5818](https://github.com/mapbox/mapbox-navigation-android/pull/5818)

#### Known issues
- :bangbang: Expiration of congestion annotations and incidents doesn't work for alternative routes, which can cause inconsistency and a false fact that alternative route is faster.
- Improvements in route line layer management to eliminate blinking when alternative routes are recomputed and redrawn after passing a fork, in addition to improving performance by reducing route line layer redraws. [#5859](https://github.com/mapbox/mapbox-navigation-android/pull/5859)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.6.0-beta.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.6.0-beta.2))
- Mapbox Navigation Native `v104.0.0`
- Mapbox Core Common `v22.0.0-beta.1`
- Mapbox Java `v6.5.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.5.0 - May 26, 2022

:bangbang: We have identified a crash in this release https://github.com/mapbox/mapbox-navigation-android/issues/5876.
The team is working on a patch. We do not recommend you using this version of the SDK in production.

### Changelog
[Changes between v2.4.1 and v2.5.0](https://github.com/mapbox/mapbox-navigation-android/compare/v2.4.1...v2.5.0)

#### Features
- Exposed `NavigationRoute#id` and `RouteProgress#routeAlternativeId` which together can be used to immediately find an alternative route that a user might've turned into and generated an off-route event. [#5653](https://github.com/mapbox/mapbox-navigation-android/pull/5653)
- Exposed `MapboxNavigation#getAlternativeMetadataFor` function which returns metadata associated with alternative routes that are tracked in the current navigation session. This metadata can be used with `MapboxRouteLineApi#setNavigationRoutes` to hide portions of the alternative routes until their deviation point with the primary route. This is especially helpful in preventing alternative routes from resurfacing under the puck if the vanishing route line feature is enabled. [#5653](https://github.com/mapbox/mapbox-navigation-android/pull/5653)
- Added `MapboxNavigation#setTripNotificationInterceptor` to allow for notification customizations. This makes the notification compatible with the `CarAppExtender`. [#5669](https://github.com/mapbox/mapbox-navigation-android/pull/5669)
- :warning: Navigation Camera will no longer animate to `pitch 0` when approaching following maneuvers: "continue", "merge", "on ramp", "off ramp" and "fork". Original behavior can be restored by setting an empty list to `FollowingFrameOptions#pitchNearManeuvers#excludedManeuvers`.
- Added support for excluding maneuvers from the 'pitch to 0' camera updates when `FollowingFrameOptions#pitchNearManeuvers` is enabled. See `FollowingFrameOptions#pitchNearManeuvers#excludedManeuvers`. [#5717](https://github.com/mapbox/mapbox-navigation-android/pull/5717)
- Introduced persistent cache for assets downloaded by `MapboxJunctionApi`, `MapboxSignboardApi` and `MapboxRouteShieldApi` to decrease the bandwidth consumption. [#5750](https://github.com/mapbox/mapbox-navigation-android/pull/5750)
- Added `RestStop#name` field which contains a name of the service/rest area, when available. [#5768](https://github.com/mapbox/mapbox-navigation-android/pull/5768)
- Exposed `NavigationRoute#origin` that describes the type of router that generated the particular route. [#5766](https://github.com/mapbox/mapbox-navigation-android/pull/5766)
- Added `RestStop#name` values for updates coming from EHorizon. [#5807](https://github.com/mapbox/mapbox-navigation-android/pull/5807)
- Updated feature `route refresh`: now also supports refreshing `incidents` along the route found in `RouteLeg#incidents` and `RoadObjectsOnRouteObserver`. [#5749](https://github.com/mapbox/mapbox-navigation-android/pull/5749)
- Added `NavigationOptions.enableSensors` which enables analyzing data from sensors for better location prediction in case of a weak GPS signal [#5800](https://github.com/mapbox/mapbox-navigation-android/pull/5800)
- Added `MapboxReplayer#eventRealtimeOffset` to allow adjusting simulated locations timestamps for more accurate replays. [#5748](https://github.com/mapbox/mapbox-navigation-android/pull/5748)
- Added `ReplayRouteOptions#frequency` to allow adjusting the simulated location frequency. [#5724](https://github.com/mapbox/mapbox-navigation-android/pull/5724)
- Added `TollCollection#name` field which contains a name of the toll collection point, when available. [#5784](https://github.com/mapbox/mapbox-navigation-android/pull/5784)

#### Bug fixes and improvements
- :warning: Realigned all `NavigationCamera` transitions to not fallback to `Idle` if a transition is canceled to keep the behavior consistent and predictable (state transitions used to be cancelable while frame transitions weren't). If you need the `NavigationCamera` to reset to `Idle` on external interactions or cancellations, use `NavigationBasicGesturesHandler` or `NavigationScaleGestureHandler` [#5607](https://github.com/mapbox/mapbox-navigation-android/pull/5607)
- Improved reroute experience for a default controller. `MapboxRerouteController` now immediately switches to an alternative route when a user turns to it, without making an unnecessary route request. [#5645](https://github.com/mapbox/mapbox-navigation-android/pull/5645)
- Added `NavigationOptions#EHorizonOptions#AlertServiceOptions` which allow to control which road objects are picked up from the eHorizon graph. :warning: Since Restricted Areas can be resource intensive to pick up, they are now disabled by default. [#5693](https://github.com/mapbox/mapbox-navigation-android/pull/5693)
- :warning: Added more runtime styling options to `ManeuverViewOptions` and deprecated individual styling methods [#5733](https://github.com/mapbox/mapbox-navigation-android/pull/5733)
- :warning: Removed support for style changes of standalone UI components based on `NavigationView` `attributes`. Introduced runtime styling support instead. [#5730](https://github.com/mapbox/mapbox-navigation-android/pull/5730)
- Parallelized some work in `MapboxNavigation#requestRoutes` to decrease the time in which `NavigationRouterCallback` returns. [#5718](https://github.com/mapbox/mapbox-navigation-android/pull/5718)
- :warning: Refactored the designs for `MapboxTripProgressView`. [#5744](https://github.com/mapbox/mapbox-navigation-android/pull/5744)
- Fixed an issue where [replacing the default logger module](https://docs.mapbox.com/android/navigation/guides/get-started/modularization/#logger) was throwing a runtime exception during library loading. [#5738](https://github.com/mapbox/mapbox-navigation-android/pull/5738)
  - extension `DirectionsRoute#toNavigationRoute()` by `DirectionsRoute#toNavigationRoute(RouterOrigin)`.
  - extension `List<DirectionsRoute>#toNavigationRoutes()` by `List<DirectionsRoute>#toNavigationRoutes(RouterOrigin)`;
  - method `NavigationRoute#create(String, String)` by `NavigationRoute#create(String, String, RouterOrigin)`;
  - method `NavigationRoute#create(DirectionsResponse, RouteOptions)` by `NavigationRoute#create(DirectionsResponse, RouteOptions, RouterOrigin)`;
- :warning: Deprecated `NavigationRoute` creation and mapping functions in favor of equivalents that take `RouterOrigin` as a parameter. If not provided, `RouterOrigin.Custom()` is used. [#5738](https://github.com/mapbox/mapbox-navigation-android/pull/5738)
- Changed the internal vanishing route line feature implementation to use the `line-trim-offset` property of `LineLayer` to improve the performance of updates. The optimization is not available when `MapboxRouteLineOptions#styleInactiveRouteLegsIndependently` is enabled. [#5697](https://github.com/mapbox/mapbox-navigation-android/pull/5697)
- Improved behavior of enhanced location teleports on parallel roads, especially forks. [#5768](https://github.com/mapbox/mapbox-navigation-android/pull/5768)
- Now it's possible to see the same route in `NavigationRouteAlternativesObserver` but coming from an offboard router if the current route was built onboard which can be used to always prefer an offboard-generated route over an onboard-generated one. This is a good practice because offboard-generated routes take live road conditions into account, have more precise ETAs, and can also be refreshed as the user drives and conditions change. Check `NavigationRouteAlternativesObserver` documentation for example usage. [#5768](https://github.com/mapbox/mapbox-navigation-android/pull/5768)
- Fixed feature to customize alternative route line colors based on route property. [#5802](https://github.com/mapbox/mapbox-navigation-android/pull/5802)
- :warning: `VoiceInstructionsObserver` doesn't trigger the last available voice instruction on registration with `MapboxNavigation#registerVoiceInstructionsObserver` anymore. The invocations of `VoiceInstructionsObserver` are critical to drive the correct timing of the instructions to be read out, that's why delivering the outdated value on registration could have led to incorrect guidance instructions. Use `RouteProgress#voiceInstructions` to get the last available voice instruction.[#5746](https://github.com/mapbox/mapbox-navigation-android/issues/5746)
- Fixed simulated route feasibility calculations. This will improve the accuracy of replay for curved roads. [#5748](https://github.com/mapbox/mapbox-navigation-android/pull/5748)
- Improved the accuracy of simulated locations speeds and the coordinate distance. This also fixed issues where the simulated driver would stall or jump near route turns. [#5724](https://github.com/mapbox/mapbox-navigation-android/pull/5724)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.5.0))
- Mapbox Navigation Native `v101.0.0`
- Mapbox Core Common `v21.3.1`
- Mapbox Java `v6.5.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.6.0-alpha.2 - May 25, 2022
### Changelog
[Changes between v2.6.0-alpha.1 and v2.6.0-alpha.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.6.0-alpha.1...v2.6.0-alpha.2)

#### Features
- `RouteLineTrimOffset` exposed in `RouteLineDynamicData` to override the vanishing point offset produced by the `MapboxRouteLineApi`. [#5858](https://github.com/mapbox/mapbox-navigation-android/pull/5858)
- Added capabilities for Drop-In UI to render in landscape mode. [#5823](https://github.com/mapbox/mapbox-navigation-android/pull/5823)
- Added support for sharing multiple instances of `MapboxNavigationObserver`. [#5829](https://github.com/mapbox/mapbox-navigation-android/pull/5829)
- Added support for patches in Tile store. Tile store may download a patch to update an existing tile instead of downloading a new one. [#5861](https://github.com/mapbox/mapbox-navigation-android/pull/5861)

#### Bug fixes and improvements
- Reduced memory consumptions on startup by not decoding tiles in predictive cache and latest version controller. [#5848](https://github.com/mapbox/mapbox-navigation-android/pull/5847)
- Fixed an issue where the vanishing point of the primary route line was not always reset when new routes were drawn following a previous active navigation session. [#5842](https://github.com/mapbox/mapbox-navigation-android/pull/5842)
- Fixed `LocationMatcherResult#location#time`. Now it follows Android conventions, i.e. contains UTC time in milliseconds instead of elapsed time. [#5861](https://github.com/mapbox/mapbox-navigation-android/pull/5861)
- Fixed map-matching for some corner cases. [#5861](https://github.com/mapbox/mapbox-navigation-android/pull/5861)
- Improved off-route detection in case of a teleport. [#5861](https://github.com/mapbox/mapbox-navigation-android/pull/5861)
- Reduced cache misses in tile store. [#5861](https://github.com/mapbox/mapbox-navigation-android/pull/5861)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.6.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.6.0-beta.1))
- Mapbox Navigation Native `v103.0.1`
- Mapbox Core Common `v22.0.0-beta.1`
- Mapbox Java `v6.5.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.5.0-rc.3 - May 24, 2022
### Changelog
[Changes between v2.5.0-rc.2 and v2.5.0-rc.3](https://github.com/mapbox/mapbox-navigation-android/compare/v2.5.0-rc.2...v2.5.0-rc.3)

#### Bug fixes and improvements
- Reduced memory consumptions on startup by not decoding tiles in predictive cache and latest version controller. [#5848](https://github.com/mapbox/mapbox-navigation-android/pull/5847)
- Fixed an issue where the vanishing point of the primary route line was not always reset when new routes were drawn following a previous active navigation session. [#5842](https://github.com/mapbox/mapbox-navigation-android/pull/5842)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.5.0))
- Mapbox Navigation Native `v101.0.0`
- Mapbox Core Common `v21.3.1`
- Mapbox Java `v6.5.0-beta.6` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0-beta.6))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.6.0-alpha.1 - May 19, 2022
### Changelog
[Changes between v2.5.0-rc.2 and v2.6.0-alpha.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.5.0-rc.1...v2.6.0-alpha.1)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.5.0))
- Mapbox Navigation Native `v100.0.0`
- Mapbox Core Common `v21.3.1`
- Mapbox Java `v6.5.0-beta.6` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0-beta.6))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.5.0-rc.2 - May 19, 2022
### Changelog
[Changes between v2.5.0-rc.1 and v2.5.0-rc.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.5.0-rc.1...v2.5.0-rc.2)

#### Bug fixes and improvements
- Fixed an issue with the vanishing point being rendered ahead of the location indicator (especially on long routes) and other rendering artifact on the route line when the vanishing feature is enabled. [#5816](https://github.com/mapbox/mapbox-navigation-android/pull/5816)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.5.0))
- Mapbox Navigation Native `v100.0.0`
- Mapbox Core Common `v21.3.1`
- Mapbox Java `v6.5.0-beta.6` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0-beta.6))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.5.0-rc.1 - May 13, 2022
### Changelog
[Changes between v2.5.0-beta.3 and v2.5.0-rc.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.5.0-beta.3...v2.5.0-rc.1)

#### Features
- Added `TollCollection#name` field which contains a name of the toll collection point, when available. [#5784](https://github.com/mapbox/mapbox-navigation-android/pull/5784)
- Added `ReplayRouteOptions#frequency` to allow adjusting the simulated location frequency. [#5724](https://github.com/mapbox/mapbox-navigation-android/pull/5724)
- Added `MapboxReplayer#eventRealtimeOffset` to allow adjusting simulated locations timestamps for more accurate replays. [#5748](https://github.com/mapbox/mapbox-navigation-android/pull/5748)
- Added `NavigationOptions.enableSensors` which enables analyzing data from sensors for better location prediction in case of a weak GPS signal [#5800](https://github.com/mapbox/mapbox-navigation-android/pull/5800)
- Updated feature `route refresh`: now also supports refreshing `incidents` along the route found in `RouteLeg#incidents` and `RoadObjectsOnRouteObserver`. [#5749](https://github.com/mapbox/mapbox-navigation-android/pull/5749)
- Added `RestStop#name` values for updates coming from EHorizon. [#5807](https://github.com/mapbox/mapbox-navigation-android/pull/5807)

#### Bug fixes and improvements
- Improved the accuracy of simulated locations speeds and the coordinate distance. This also fixed issues where the simulated driver would stall or jump near route turns. [#5724](https://github.com/mapbox/mapbox-navigation-android/pull/5724)
- Fixed simulated route feasibility calculations. This will improve the accuracy of replay for curved roads. [#5748](https://github.com/mapbox/mapbox-navigation-android/pull/5748)
- :warning: `VoiceInstructionsObserver` doesn't trigger the last available voice instruction on registration with `MapboxNavigation#registerVoiceInstructionsObserver` anymore. The invocations of `VoiceInstructionsObserver` are critical to drive the correct timing of the instructions to be read out, that's why delivering the outdated value on registration could have led to incorrect guidance instructions. Use `RouteProgress#voiceInstructions` to get the last available voice instruction.[#5746](https://github.com/mapbox/mapbox-navigation-android/issues/5746)
- Fixed occasional absence of the first voice instruction. [#5807](https://github.com/mapbox/mapbox-navigation-android/pull/5807)
- Fixed an issue where prettified Mapbox Directions service responses weren't parsed correctly. [#5807](https://github.com/mapbox/mapbox-navigation-android/pull/5807)
- Fixed feature to customize alternative route line colors based on route property. [#5802](https://github.com/mapbox/mapbox-navigation-android/pull/5802)

#### Known issues
- Precision of the point at which the route is supposed to change behind the location indicator is negatively impacted for long routes. This can present itself by the route vanishing/changing in front the the location indicator.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.5.0))
- Mapbox Navigation Native `v100.0.0`
- Mapbox Core Common `v21.3.1`
- Mapbox Java `v6.5.0-beta.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0-beta.5))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.5.0-beta.3 - May 5, 2022
### Changelog
[Changes between v2.5.0-beta.2 and v2.5.0-beta.3](https://github.com/mapbox/mapbox-navigation-android/compare/v2.5.0-beta.2...v2.5.0-beta.3)

#### Features
- Exposed `NavigationRoute#origin` that describes the type of router that generated the particular route. [#5766](https://github.com/mapbox/mapbox-navigation-android/pull/5766)
- Added `RestStop#name` field which contains a name of the service/rest area, when available. [#5768](https://github.com/mapbox/mapbox-navigation-android/pull/5768)
- Introduced persistent cache for assets downloaded by `MapboxJunctionApi`, `MapboxSignboardApi` and `MapboxRouteShieldApi` to decrease the bandwidth consumption. [#5750](https://github.com/mapbox/mapbox-navigation-android/pull/5750)

#### Bug fixes and improvements
- Now it's possible to see the same route in `NavigationRouteAlternativesObserver` but coming from an offboard router if the current route was built onboard which can be used to always prefer an offboard-generated route over an onboard-generated one. This is a good practice because offboard-generated routes take live road conditions into account, have more precise ETAs, and can also be refreshed as the user drives and conditions change. Check `NavigationRouteAlternativesObserver` documentation for example usage. [#5768](https://github.com/mapbox/mapbox-navigation-android/pull/5768)
- Improved behavior of enhanced location teleports on parallel roads, especially forks. [#5768](https://github.com/mapbox/mapbox-navigation-android/pull/5768)
- Changed the internal vanishing route line feature implementation to use the `line-trim-offset` property of `LineLayer` to improve the performance of updates. The optimization is not available when `MapboxRouteLineOptions#styleInactiveRouteLegsIndependently` is enabled. [#5697](https://github.com/mapbox/mapbox-navigation-android/pull/5697)

#### Known issues
- `RestStop#name` are only available through `RoadObjectsOnRouteObserver` and not through `EHorizonObserver` yet.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.5.0))
- Mapbox Navigation Native `v98.0.0`
- Mapbox Core Common `v21.3.1`
- Mapbox Java `v6.5.0-beta.4` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0-beta.4))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.4.1 - April 28, 2022
### Changelog
[Changes between v2.4.0 and v2.4.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.4.0...v2.4.1)

#### Bug fixes and improvements

- Fixed an issue where route refresh was not updating the ETA values found in the `RouteProgress` object. [#5755](https://github.com/mapbox/mapbox-navigation-android/pull/5755)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.4.3` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.4.3))
- Mapbox Navigation Native `v94.0.3`
- Mapbox Core Common `v21.2.1`
- Mapbox Java `v6.4.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.4.1))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.5.0-beta.2 - April 28, 2022
### Changelog
[Changes between v2.5.0-beta.1 and v2.5.0-beta.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.5.0-beta.1...v2.5.0-beta.2)

#### Bug fixes and improvements

- Fixed an issue where route refresh was not updating the ETA values found in the `RouteProgress` object. [#5755](https://github.com/mapbox/mapbox-navigation-android/pull/5755)
- :warning: Deprecated `NavigationRoute` creation and mapping functions in favor of equivalents that take `RouterOrigin` as a parameter. If not provided, `RouterOrigin.Custom()` is used. [#5738](https://github.com/mapbox/mapbox-navigation-android/pull/5738)
  - method `NavigationRoute#create(DirectionsResponse, RouteOptions)` by `NavigationRoute#create(DirectionsResponse, RouteOptions, RouterOrigin)`;
  - method `NavigationRoute#create(String, String)` by `NavigationRoute#create(String, String, RouterOrigin)`;
  - extension `List<DirectionsRoute>#toNavigationRoutes()` by `List<DirectionsRoute>#toNavigationRoutes(RouterOrigin)`;
  - extension `DirectionsRoute#toNavigationRoute()` by `DirectionsRoute#toNavigationRoute(RouterOrigin)`.
- Fixed an issue where [replacing the default logger module](https://docs.mapbox.com/android/navigation/guides/get-started/modularization/#logger) was throwing a runtime exception during library loading. [#5738](https://github.com/mapbox/mapbox-navigation-android/pull/5738)
- :warning: Refactored the designs for `MapboxTripProgressView`. [#5744](https://github.com/mapbox/mapbox-navigation-android/pull/5744)
- Fixed the crash of default `RerouteController` when it immediately switches to an existing alternative. [#5753](https://github.com/mapbox/mapbox-navigation-android/pull/5753)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.5.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.5.0-rc.1))
- Mapbox Navigation Native `v97.0.0`
- Mapbox Core Common `v21.3.1`
- Mapbox Java `v6.5.0-beta.3` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0-beta.3))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.5.0-beta.1 - April 22, 2022
### Changelog
[Changes between v2.5.0-alpha.3 and v2.5.0-beta.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.5.0-alpha.3...v2.5.0-beta.1)

#### Features
- Added support for excluding maneuvers from the 'pitch to 0' camera updates when `FollowingFrameOptions#pitchNearManeuvers` is enabled. See `FollowingFrameOptions#pitchNearManeuvers#excludedManeuvers`. [#5717](https://github.com/mapbox/mapbox-navigation-android/pull/5717)
- :warning: Navigation Camera will no longer animate to `pitch 0` when approaching following maneuvers: "continue", "merge", "on ramp", "off ramp" and "fork". Original behavior can be restored by setting an empty list to `FollowingFrameOptions#pitchNearManeuvers#excludedManeuvers`.

#### Bug fixes and improvements
- Parallelized some work in `MapboxNavigation#requestRoutes` to decrease the time in which `NavigationRouterCallback` returns. [#5718](https://github.com/mapbox/mapbox-navigation-android/pull/5718)
- :warning: Removed support for style changes of standalone UI components based on `NavigationView` `attributes`. Introduced runtime styling support instead. [#5730](https://github.com/mapbox/mapbox-navigation-android/pull/5730)
- :warning: Added more runtime styling options to `ManeuverViewOptions` and deprecated individual styling methods [#5733](https://github.com/mapbox/mapbox-navigation-android/pull/5733)

#### Known issues
- Adding a custom `Logger` instance by [replacing the default logger module](https://docs.mapbox.com/android/navigation/guides/get-started/modularization/#logger) causes a runtime exception during library loading.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.5.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.5.0-beta.1))
- Mapbox Navigation Native `v96.0.0`
- Mapbox Core Common `v21.3.0-rc.2`
- Mapbox Java `v6.5.0-beta.2` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.5.0-beta.2))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.5.0-alpha.3 - April 15, 2022
### Changelog
[Changes between v2.5.0-alpha.2 and v2.5.0-alpha.3](https://github.com/mapbox/mapbox-navigation-android/compare/v2.5.0-alpha.2...v2.5.0-alpha.3)

#### Features
- Added `MapboxNavigation#setTripNotificationInterceptor` to allow for notification customizations. This makes the notification compatible with the `CarAppExtender`. [#5669](https://github.com/mapbox/mapbox-navigation-android/pull/5669)

#### Bug fixes and improvements
- Added `NavigationOptions#EHorizonOptions#AlertServiceOptions` which allow to control which road objects are picked up from the eHorizon graph. :warning: Since Restricted Areas can be resource intensive to pick up, they are now disabled by default. [#5693](https://github.com/mapbox/mapbox-navigation-android/pull/5693)
- Fixed an issue where a call to `MapboxNavigation#stopTripSession` would clear the routes reference and led to a `RoutesObserver` notification with empty routes collection. [#5685](https://github.com/mapbox/mapbox-navigation-android/pull/5685)
- Fixed an issue where `AlternativeRouteMetadata` would get cleared after route refresh (whenever routes update reason was `RoutesExtra#ROUTES_UPDATE_REASON_REFRESH`). [#5691](https://github.com/mapbox/mapbox-navigation-android/pull/5691)
- Fixed a race condition where internal route refresh logic could overwrite the result of a call to `MapboxNavigation#setRoutes`. [#5685](https://github.com/mapbox/mapbox-navigation-android/pull/5685)

#### Known issues
- Adding a custom `Logger` instance by [replacing the default logger module](https://docs.mapbox.com/android/navigation/guides/get-started/modularization/#logger) causes a runtime exception during library loading.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.5.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.5.0-beta.1))
- Mapbox Navigation Native `v96.0.0`
- Mapbox Core Common `v21.3.0-rc.2`
- Mapbox Java `v6.4.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.4.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.4.0 - April 14, 2022
### Changelog
[Changes between v2.4.0 and v2.3.0](https://github.com/mapbox/mapbox-navigation-android/compare/v2.3.0...v2.4.0)

- :exclamation: Starting with version 2.4 (2.4.0-beta.3), we are implementing a grace period of 30-seconds for all navigation sessions started by Nav SDK. A session will be counted only after this time period has surpassed. This allows you to reduce the cost of using the SDK during development and testing of your applications, as well as in production. Grace period is especially helpful to decrease the cost of short Free Drive session that are just a transition between Active Guidance sessions, or when a session is aborted right after it was started.
- :warning: Deprecated `RouterOrigin.Custom(obj: Any?)`. The SDK doesn't keep `RouteOrigin.Custom#obj` anymore, it always becomes `null` when the `obj` returns in `RoutesObserver`. [#5500](https://github.com/mapbox/mapbox-navigation-android/pull/5500)
- :warning: Restricted Areas are intentionally not being picked up from the eHorizon graph in Free Drive as the operation can be very resource intensive. An option to re-enable this feature will be exposed in future releases.

#### Features
- Introduced a `NavigationRoute` object and related functions like `NavigationRouterCallback`, `MapboxNavigation#setNavigationRoutes`, etc. `NavigationRoute` is a domain specific wrapper on top of `DirectionsRoute` which provides information (and enforces its presence) about the original `DirectionsResponse` that this route is part of, as well as what `RouteOptions` were used to generate it.
  Most of the Navigation SDK APIs that rely only on `DirectionsRoute` are now marked as `Deprecated` since there might be features introduced in the future that would require the `NavigationRoute` instances instead.
  There are `NavigationRoute#toDirectionsRoute()` and `DirectionsRoute#toNavigationRoute()` compatibility extensions available, however, the latter is lossy since `DirectionsRoute` cannot carry as much information as `NavigationRoute` and the recommended migration path is to use `NavigationRouterCallback` instead of the old `RouterCallback` to request routes.
  This change might require action when integrating if you have hardcoded routes in your test suites that do not have all necessary fields to support `DirectionsRoute` to `NavigationRoute` mapping (mostly `DirectionsRoute#routeIndex`) - in these cases make sure to provide the missing data or regenerate the routes using `MapboxNavigation#requestRoutes`.
  [#5411](https://github.com/mapbox/mapbox-navigation-android/pull/5411)
- Added a new `RailwayCrossing` type to `RoadObject`s. [#5552](https://github.com/mapbox/mapbox-navigation-android/pull/5552)
- Added point exclusion option for onboard router. [#5552](https://github.com/mapbox/mapbox-navigation-android/pull/5552)
- Improved capabilities to replay route alternatives with the `ReplayProgressObserver` by respecting the current distance traveled. This fixes the jump to the beginning of the alternative route upon the switch. [#5586](https://github.com/mapbox/mapbox-navigation-android/pull/5586)
- Added extension functions for overriding the route line traffic expression or color. [#5597](https://github.com/mapbox/mapbox-navigation-android/pull/5597)
- Added `RerouteOptionsAdapter`. It allows to modify `RouteOptions` on reroute for default implementation of `RerouteController` via `MapboxNavigation#setRerouteOptionsAdapter`. [#5573](https://github.com/mapbox/mapbox-navigation-android/pull/5573)
- Added `LocationMatcherResult#isDegradedMapMatching` which allows to understand if current matched location was produced using limited map matching approach(e.g. due to lack of map data). [#5606](https://github.com/mapbox/mapbox-navigation-android/pull/5606)

#### Bug fixes and improvements
- Added `MapboxNavigationApp.setup` overload, that accepts `NavigationOptionsProvider` instead of prebuilt `NavigationOptions`. [#5490](https://github.com/mapbox/mapbox-navigation-android/pull/5490)
- Fixed `MapboxNavigationApp` issue, that caused unexpected `MapboxNavigation` destroy, when one of the attached lifecycles is destroyed and the other one is stopped. [#5518](https://github.com/mapbox/mapbox-navigation-android/pull/5518)
- Fixed uncaught exception in the `ShieldsCache` class. [#5524](https://github.com/mapbox/mapbox-navigation-android/pull/5524)
- Use native logger backend instead of the Android instance directly. [#5520](https://github.com/mapbox/mapbox-navigation-android/pull/5520)
- Fixed an issue where a route refresh would only refresh annotations for the current leg of a route instead of for all remaining legs of a route. [#5552](https://github.com/mapbox/mapbox-navigation-android/pull/5552)
- Fixed an issue where alternative routes would hold a reference to the original route's `RouteOptions` instead of the correct ones that were used to generate the alternative. [#5552](https://github.com/mapbox/mapbox-navigation-android/pull/5552)
- Fixed an issue related to highlighting buildings via the MapboxBuildingsApi by adding the building-extrusion layer as one of the layers queried. [#5433](https://github.com/mapbox/mapbox-navigation-android/pull/5433)
- Adopted Common SDK log messages parsing logic so it's consistent across Mapbox SDKs. As an example, this is how the logs would look like `D/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`. [#5604](https://github.com/mapbox/mapbox-navigation-android/pull/5604)
- Added a workaround for [`ConnectivityManager`'s occasional security exception on Android 11 and older](https://issuetracker.google.com/issues/175055271). [#5587](https://github.com/mapbox/mapbox-navigation-android/pull/5587)
- Fixed an issue where off-route wouldn't be reported if we were navigating in a fallback mode (without routing tiles on device). [#5587](https://github.com/mapbox/mapbox-navigation-android/pull/5587)
- `RouteProgressState#INITIALIZED` might now be reported for each leg start, not only for the route start. [#5587](https://github.com/mapbox/mapbox-navigation-android/pull/5587)
- Fixed `HistoryEventMapper#mapNavigationRoute` for when `SetRouteHistoryRecord` has empty `routeRequest`. [#5614](https://github.com/mapbox/mapbox-navigation-android/pull/5614)
- Fixed an issue where route refresh failure led to a parsing error and runtime crash instead of failure callback. [#5617](https://github.com/mapbox/mapbox-navigation-android/pull/5617)
- Fixed `ReplayRouteInterpolator` speed adjustment on turns. A driver doesn't slow down on minor curvatures on a motorway. [5618](https://github.com/mapbox/mapbox-navigation-android/pull/5618)
- Fixed an issue where line `RoadObject`s matched with `RoadObjectMatcher` which consisted of only one edge and multiple lines where calculated incorrectly. [#5629](https://github.com/mapbox/mapbox-navigation-android/pull/5629)
- Moved alternative route line processing to a worker thread, relieving a little bit of load from the main thread during each recalculation. [#5634](https://github.com/mapbox/mapbox-navigation-android/pull/5634)
- Added `AsyncAudioFocusDelegate` to `MapboxVoiceInstructionsPlayer` to allow clients to interact with the audio focus in an asynchronous way. [#5652](https://github.com/mapbox/mapbox-navigation-android/pull/5652)
- Added `AudioFocusOwner` so that the owner can be specified when requesting the audio focus `AsyncAudioFocusDelegate#requestFocus`. [#5652](https://github.com/mapbox/mapbox-navigation-android/pull/5652)
- Added `ttsStreamType` to `VoiceInstructionsPlayerOptions` so that the stream type for playing TTS can be specified, allowing to fix an issue with `KEY_PARAM_STREAM` not being updated and used properly. Defaults to `AudioManager.STREAM_MUSIC`. [#5652](https://github.com/mapbox/mapbox-navigation-android/pull/5652)
- Fixed an issue found in `v2.4.0-beta.3` and later where compressed tiles were incorrectly decoded from the cache. This could have led to degraded map matching or unavailable resources. [#5707](https://github.com/mapbox/mapbox-navigation-android/pull/5707)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.4.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.4.2))
- Mapbox Navigation Native `v94.0.3`
- Mapbox Core Common `v21.2.1`
- Mapbox Java `v6.4.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.4.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.5.0-alpha.2 - April 7, 2022
### Changelog
[Changes between v2.5.0-alpha.1 and v2.5.0-alpha.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.5.0-alpha.1...v2.5.0-alpha.2)

#### Features
- Exposed `MapboxNavigation#getAlternativeMetadataFor` function which returns metadata associated with alternative routes that are tracked in the current navigation session. This metadata can be used with `MapboxRouteLineApi#setNavigationRoutes` to hide portions of the alternative routes until their deviation point with the primary route. This is especially helpful in preventing alternative routes from resurfacing under the puck if the vanishing route line feature is enabled. [#5653](https://github.com/mapbox/mapbox-navigation-android/pull/5653)
- Exposed `NavigationRoute#id` and `RouteProgress#routeAlternativeId` which together can be used to immediately find an alternative route that a user might've turned into and generated an off-route event. [#5653](https://github.com/mapbox/mapbox-navigation-android/pull/5653)

#### Bug fixes and improvements
- Improved reroute experience for a default controller. `MapboxRerouteController` now immediately switches to an alternative route when a user turns to it, without making an unnecessary route request. [#5645](https://github.com/mapbox/mapbox-navigation-android/pull/5645)
- Added `AsyncAudioFocusDelegate` to `MapboxVoiceInstructionsPlayer` to allow clients to interact with the audio focus in an asynchronous way. [#5652](https://github.com/mapbox/mapbox-navigation-android/pull/5652)
- Added `AudioFocusOwner` so that the owner can be specified when requesting the audio focus `AsyncAudioFocusDelegate#requestFocus`. [#5652](https://github.com/mapbox/mapbox-navigation-android/pull/5652)
- Added `ttsStreamType` to `VoiceInstructionsPlayerOptions` so that the stream type for playing TTS can be specified, allowing to fix an issue with `KEY_PARAM_STREAM` not being updated and used properly. Defaults to `AudioManager.STREAM_MUSIC`. [#5652](https://github.com/mapbox/mapbox-navigation-android/pull/5652)
- Made all reachable polygon entries and exits tracked instead of the closest one for E-horizon `RoadObject`s. [#5653](https://github.com/mapbox/mapbox-navigation-android/pull/5653)
- Fixed an issue where enhanced location couldn't snap to correct road edge for a long time after leaving a tunnel. [#5653](https://github.com/mapbox/mapbox-navigation-android/pull/5653)
- Fixed `MapboxSpeedLimitView` sizing when rendering `UpdateSpeedLimitError` value [#5666](https://github.com/mapbox/mapbox-navigation-android/pull/5666)

#### Other changes
- Up until this point, `RoutesObserver` fired nearly immediately with new route references after `MapboxNavigation@setNavigationRoutes` was called. Now, the Nav SDK first fully processes the routes (for example, to compute the `AlternativeRouteMetadata`) which results in a small delay between routes being set and actually returned by the `RoutesObserver`.

#### Known issues
- Occasionally, the first `RouteProgress` update after setting new route might fail to be delivered due to the scheduling of route processing jobs.
- When alternative routes that take advantage of the `AlternativeRouteMetadata` are reset because new become available, or changed manually by interacting with the map, there could be a brief flash of the full alternative route geometry which quickly recovers to a valid state.
- Calling `MapboxNavigation#stopTripSession` clears the routes reference and leads to a `RoutesObserver` notification with empty routes collection.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.4.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.4.0))
- Mapbox Navigation Native `v95.0.0`
- Mapbox Core Common `v21.2.0`
- Mapbox Java `v6.4.0-beta.4` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.4.0-beta.4))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.4.0-rc.2 - April 7, 2022

#### Bug fixes and improvements
- Added `AsyncAudioFocusDelegate` to `MapboxVoiceInstructionsPlayer` to allow clients to interact with the audio focus in an asynchronous way. [#5652](https://github.com/mapbox/mapbox-navigation-android/pull/5652)
- Added `AudioFocusOwner` so that the owner can be specified when requesting the audio focus `AsyncAudioFocusDelegate#requestFocus`. [#5652](https://github.com/mapbox/mapbox-navigation-android/pull/5652)
- Added `ttsStreamType` to `VoiceInstructionsPlayerOptions` so that the stream type for playing TTS can be specified, allowing to fix an issue with `KEY_PARAM_STREAM` not being updated and used properly. Defaults to `AudioManager.STREAM_MUSIC`. [#5652](https://github.com/mapbox/mapbox-navigation-android/pull/5652)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.4.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.4.0))
- Mapbox Navigation Native `v94.0.0`
- Mapbox Core Common `v21.2.0`
- Mapbox Java `v6.4.0-beta.4` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.4.0-beta.4))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.5.0-alpha.1 - April 1, 2022
### Changelog
[Changes between v2.4.0-rc.3 and v2.5.0-alpha.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.4.0-rc.3...v2.5.0-alpha.1)

#### Bug fixes and improvements
- :warning: Realigned all `NavigationCamera` transitions to not fallback to `Idle` if a transition is canceled to keep the behavior consistent and predictable (state transitions used to be cancelable while frame transitions weren't). If you need the `NavigationCamera` to reset to `Idle` on external interactions or cancellations, use `NavigationBasicGesturesHandler` or `NavigationScaleGestureHandler` [#5607](https://github.com/mapbox/mapbox-navigation-android/pull/5607)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.4.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.4.0))
- Mapbox Navigation Native `v94.0.0`
- Mapbox Core Common `v21.2.0`
- Mapbox Java `v6.4.0-beta.4` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.4.0-beta.4))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.4.0-rc.1 - March 31, 2022

#### Bug fixes and improvements
- Fixed `HistoryEventMapper#mapNavigationRoute` for when `SetRouteHistoryRecord` has empty `routeRequest`. [#5614](https://github.com/mapbox/mapbox-navigation-android/pull/5614)
- Fixed an issue where route refresh failure led to a parsing error and runtime crash instead of failure callback. [#5617](https://github.com/mapbox/mapbox-navigation-android/pull/5617)
- Fixed `ReplayRouteInterpolator` speed adjustment on turns. A driver doesn't slow down on minor curvatures on a motorway. [5618](https://github.com/mapbox/mapbox-navigation-android/pull/5618)
- Fixed an issue where `RouteProgress` might not have been delivered in active guidance when `MapboxNavigation#setRoutes` calls were dispatched in quick succession (session was locked in an invalid state). [#5629](https://github.com/mapbox/mapbox-navigation-android/pull/5629)
- Improved the time between the router getting access to a raw Directions API response and Nav SDK delivering a `NavigationRoute` instance in `NavigationRouterCallback`. [#5629](https://github.com/mapbox/mapbox-navigation-android/pull/5629)
- Fixed an issue where line `RoadObject`s matched with `RoadObjectMatcher` which consisted of only one edge and multiple lines where calculated incorrectly. [#5629](https://github.com/mapbox/mapbox-navigation-android/pull/5629)
- Moved alternative route line processing to a worker thread, relieving a little bit of load from the main thread during each recalculation. [#5634](https://github.com/mapbox/mapbox-navigation-android/pull/5634)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.4.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.4.0))
- Mapbox Navigation Native `v94.0.0`
- Mapbox Core Common `v21.2.0`
- Mapbox Java `v6.4.0-beta.4` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.4.0-beta.4))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.4.0-beta.3 - March 25, 2022
### Changelog
[Changes between v2.4.0-beta.2 and v2.4.0-beta.3](https://github.com/mapbox/mapbox-navigation-android/compare/v2.4.0-beta.2...v2.4.0-beta.3)

- :exclamation: Starting with version 2.4 (2.4.0-beta.3), we are implementing a grace period of 30-seconds for all navigation sessions started by Nav SDK. A session will be counted only after this time period has surpassed. This allows you to reduce the cost of using the SDK during development and testing of your applications, as well as in production. Grace period is especially helpful to decrease the cost of short Free Drive session that are just a transition between Active Guidance sessions, or when a session is aborted right after it was started.

#### Features
- Added `RerouteOptionsAdapter`. It allows to modify `RouteOptions` on reroute for default implementation of `RerouteController` via `MapboxNavigation#setRerouteOptionsAdapter`. [#5573](https://github.com/mapbox/mapbox-navigation-android/pull/5573)
- Added `LocationMatcherResult#isDegradedMapMatching` which allows to understand if current matched location was produced using limited map matching approach(e.g. due to lack of map data). [#5606](https://github.com/mapbox/mapbox-navigation-android/pull/5606)

#### Bug fixes and improvements
- Adopted Common SDK log messages parsing logic so it's consistent across Mapbox SDKs. As an example, this is how the logs would look like `D/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`. [#5604](https://github.com/mapbox/mapbox-navigation-android/pull/5604)
- :warning: `NavigationRoute`'s constructor has been hidden in favor of `NavigationRoute#create` static factories. [#5587](https://github.com/mapbox/mapbox-navigation-android/pull/5587)
- Added a workaround for [`ConnectivityManager`'s occasional security exception on Android 11 and older](https://issuetracker.google.com/issues/175055271). [#5587](https://github.com/mapbox/mapbox-navigation-android/pull/5587)
- Fixed an issue where off-route wouldn't be reported if we were navigating in a fallback mode (without routing tiles on device). [#5587](https://github.com/mapbox/mapbox-navigation-android/pull/5587)
- `RouteProgressState#INITIALIZED` might now be reported for each leg start, not only for the route start. [#5587](https://github.com/mapbox/mapbox-navigation-android/pull/5587)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.4.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.4.0-rc.1))
- Mapbox Navigation Native `v93.0.0`
- Mapbox Core Common `v21.2.0-rc.1`
- Mapbox Java `v6.4.0-beta.3` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.4.0-beta.3))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.4.0-beta.2 - March 18, 2022
### Changelog
[Changes between v2.4.0-beta.1 and v2.4.0-beta.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.4.0-beta.1...v2.4.0-beta.2)

#### Features
- Added a new `RailwayCrossing` type to `RoadObject`s. [#5552](https://github.com/mapbox/mapbox-navigation-android/pull/5552)
- Added point exclusion option for onboard router. [#5552](https://github.com/mapbox/mapbox-navigation-android/pull/5552)
- Improved capabilities to replay route alternatives with the `ReplayProgressObserver` by respecting the current distance traveled. This fixes the jump to the beginning of the alternative route upon the switch. [#5586](https://github.com/mapbox/mapbox-navigation-android/pull/5586)
- Added extension functions for overriding the route line traffic expression or color. [#5597](https://github.com/mapbox/mapbox-navigation-android/pull/5597)

#### Bug fixes and improvements
- Fixed an issue where a route refresh would only refresh annotations for the current leg of a route instead of for all remaining legs of a route. [#5552](https://github.com/mapbox/mapbox-navigation-android/pull/5552)
- Fixed an issue where alternative routes would hold a reference to the original route's `RouteOptions` instead of the correct ones that were used to generate the alternative. [#5552](https://github.com/mapbox/mapbox-navigation-android/pull/5552)
- Fixed an issue related to highlighting buildings via the MapboxBuildingsApi by adding the building-extrusion layer as one of the layers queried. [#5433](https://github.com/mapbox/mapbox-navigation-android/pull/5433)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.4.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.4.0-beta.1))
- Mapbox Navigation Native `v90.0.1`
- Mapbox Core Common `21.2.0-beta.1`
- Mapbox Java `6.4.0-beta.2` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.4.0-beta.2))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.4.0-beta.1 - March 11, 2022
### Changelog
[Changes between v2.4.0-alpha.2 and v2.4.0-beta.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.4.0-alpha.2...v2.4.0-beta.1)

- :warning: Deprecated `RouterOrigin.Custom(obj: Any?)`. The SDK doesn't keep `RouteOrigin.Custom#obj` anymore, it always becomes `null` when the `obj` returns in `RoutesObserver`. [#5500](https://github.com/mapbox/mapbox-navigation-android/pull/5500)

#### Features
- Exposed option in `MapboxRouteLineOptions` to change the default icon pitch alignment for waypoint icons. [#5531](https://github.com/mapbox/mapbox-navigation-android/pull/5531)

#### Bug fixes and improvements
- Changed internal road class calculations used by the `MapboxRouteLineApi` to fail gracefully and log instead of crashing when invalid route `geometry_index` is provided in the route object.. [#5542](https://github.com/mapbox/mapbox-navigation-android/pull/5542)

## Mapbox Navigation SDK 2.4.0-alpha.2 - March 4, 2022
### Changelog
[Changes between v2.4.0-alpha.1 and v2.4.0-alpha.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.4.0-alpha.1...v2.4.0-alpha.2)

#### Bug fixes and improvements
- Fixed `MapboxNavigationApp` issue, that caused unexpected `MapboxNavigation` destroy, when one of the attached lifecycles is destroyed and the other one is stopped. [#5518](https://github.com/mapbox/mapbox-navigation-android/pull/5518)
- Fixed uncaught exception in the `ShieldsCache` class. [#5524](https://github.com/mapbox/mapbox-navigation-android/pull/5524)
- Use native logger backend instead of the Android instance directly. [#5520](https://github.com/mapbox/mapbox-navigation-android/pull/5520)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.3.0))
- Mapbox Navigation Native `v88.0.0`
- Mapbox Core Common `v21.1.1`
- Mapbox Java `v6.4.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.4.0-beta.1))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.3.0 - March 2, 2022
### Changelog
[Changes between v2.2.2 and v2.3.0](https://github.com/mapbox/mapbox-navigation-android/compare/v2.2.2...v2.3.0)

#### Features
- Added new `StatusView` component for displaying SDK status messages. [#5403](https://github.com/mapbox/mapbox-navigation-android/pull/5403)

#### Bug fixes and improvements
- Added geo deeplink parsing utility `GeoDeeplinkParser`. [#5103](https://github.com/mapbox/mapbox-navigation-android/pull/5103)
- Added options to the `MapboxRouteLineOptions` class to control the icon anchor and icon offset for the route line related waypoint icons including the origin and destination points. [#5409](https://github.com/mapbox/mapbox-navigation-android/pull/5409)
- Fixed an issue where `RouteAlternativesObserver` and `RouteAlternativesRequestCallback` failed to deliver any alternatives due to an unrecognized `"closure"` annotation parameter in the native controller. [#5421](https://github.com/mapbox/mapbox-navigation-android/pull/5421)
- Fixed an issue where a reference to the registered `RouteAlternativesObserver` was not released after unsubscribing or destroying `MapboxNavigation`. [#5421](https://github.com/mapbox/mapbox-navigation-android/pull/5421)
- Improved `RouteAlternativesObserver` to ensure that alternative routes request is fired on each new leg start. [#5421](https://github.com/mapbox/mapbox-navigation-android/pull/5421)
- Added `avoidManeuverSeconds` param to `RouteAlternativesOptions`. [#5394](https://github.com/mapbox/mapbox-navigation-android/pull/5394)
- Fixed an issue found in `v2.3.0-alpha.1` where deprecated fields in `Road` type were always returning `null`s instead of the correct values. [#5396](https://github.com/mapbox/mapbox-navigation-android/pull/5396)
- Fixed an issue where `MapboxPrimaryManeuver` defaults to using exit signs based on VIENNA convention instead of MUTCD. [#5413](https://github.com/mapbox/mapbox-navigation-android/pull/5413)
- Fixed an issue where `MapboxRoadNameView` would continue to show previous road name in case the current `Road` has no data to show. [#5417](https://github.com/mapbox/mapbox-navigation-android/pull/5417)
- Refactored `Status` class to POJO and decreased its constructor visibility. Introduced `StatusFactory` class for public use. [#5432](https://github.com/mapbox/mapbox-navigation-android/pull/5432)
- Fixed an issue where setting a custom `MapboxRouteLineOptions#vanishingRouteLineUpdateIntervalNano` that happened to be longer than the typical rate of `RouteProgress` updates delivered via `mapboxRouteLineApi#updateWithRouteProgress` would result in the traveled portion of the route not vanishing at all. [#5435](https://github.com/mapbox/mapbox-navigation-android/pull/5435)
- Fixed an issue where `MapboxManeuverView` or `MapboxRoadNameView` displayed wrong shields. [#5426](https://github.com/mapbox/mapbox-navigation-android/pull/5426)
- Updated the turn icons used in Mapbox Android Nav SDK. [#5430](https://github.com/mapbox/mapbox-navigation-android/pull/5430)
- Added `MapboxNavigationApp.isSetup` to ensure views do not reset `MapboxNavigation`. Added `MapboxNavigationApp.getObserver` to be able to access registered observers. [#5358](https://github.com/mapbox/mapbox-navigation-android/pull/5358)
- Refactored maneuver implementation to return a new `Maneuver` instance whenever the data in an existing `Maneuver` object needs to be modified. [#5453](https://github.com/mapbox/mapbox-navigation-android/pull/5453)
- Updated `MapboxManeuverView` / `MapboxRoadNameView` to prefer Mapbox designed shields over legacy ones. [#5445](https://github.com/mapbox/mapbox-navigation-android/pull/5445)
- Fixed `Nav Telemetry` to not run if `Telemetry events` is disabled. [#5455](https://github.com/mapbox/mapbox-navigation-android/pull/5455)
- Updated `MapboxNavigation#provideFeedbackMetadataWrapper` to throw `IllegalStateException` if `Telemetry events` is disabled. [#5455](https://github.com/mapbox/mapbox-navigation-android/pull/5455)
- Fixed an issue where some resource requests dispatched by Nav SDK (namely junction images and shields) were not wired through the same HTTP client instance that can be interacted with using `HttpServiceFactory.getInstance()`. Note: `MapboxSpeechApi` requests are still wired through an independent HTTP client. [#5459](https://github.com/mapbox/mapbox-navigation-android/pull/5459)
- Updated maneuver icons for notifications. [#5449](https://github.com/mapbox/mapbox-navigation-android/pull/5449)
- Fixed an issue where `off-route` was always false if there are no routing tiles available. [#5457](https://github.com/mapbox/mapbox-navigation-android/pull/5457)
- Added a default timeout for `Online` route requests (60 seconds). [#5457](https://github.com/mapbox/mapbox-navigation-android/pull/5457)
- Fixed an issue where an alternative route was equal a current route. [#5457](https://github.com/mapbox/mapbox-navigation-android/pull/5457)
- Added delay before re-attempting to request alternatives in case the first attempt resulted in the deviation point behind the current position. [#5457](https://github.com/mapbox/mapbox-navigation-android/pull/5457)
- Fixed using `POST` for long requests [#5480](https://github.com/mapbox/mapbox-navigation-android/pull/5480)
- (**Correction** - this workaround only actually became available in `v2.4.0-beta.3`) Added a workaround for [`ConnectivityManager`'s occasional security exception on Android 11 and older](https://issuetracker.google.com/issues/175055271). [#5492](https://github.com/mapbox/mapbox-navigation-android/pull/5492)
- Fixed `MapboxNavigationApp` issue, that caused unexpected `MapboxNavigation` destroy, when one of the attached lifecycles is destroyed and the other one is stopped. [#5518](https://github.com/mapbox/mapbox-navigation-android/pull/5518)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.3.0))
- Mapbox Navigation Native `v88.0.0`
- Mapbox Core Common `v21.1.1`
- Mapbox Java `v6.3.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.3.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.4.0-alpha.1 - February 24, 2022
### Changelog
[Changes between v2.3.0-rc.3 and v2.4.0-alpha.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.3.0-rc.3...v2.4.0-alpha.1)

#### Features
- Introduced a `NavigationRoute` object and related functions like `NavigationRouterCallback`, `MapboxNavigation#setNavigationRoutes`, etc. `NavigationRoute` is a domain specific wrapper on top of `DirectionsRoute` which provides information (and enforces its presence) about the original `DirectionsResponse` that this route is part of, as well as what `RouteOptions` were used to generate it.
  Most of the Navigation SDK APIs that rely only on `DirectionsRoute` are now marked as `Deprecated` since there might be features introduced in the future that would require the `NavigationRoute` instances instead.
  There are `NavigationRoute#toDirectionsRoute()` and `DirectionsRoute#toNavigationRoute()` compatibility extensions available, however, the latter is lossy since `DirectionsRoute` cannot carry as much information as `NavigationRoute` and the recommended migration path is to use `NavigationRouterCallback` instead of the old `RouterCallback` to request routes.
  This change might require action when integrating if you have hardcoded routes in your test suites that do not have all necessary fields to support `DirectionsRoute` to `NavigationRoute` mapping (mostly `DirectionsRoute#routeIndex`) - in these cases make sure to provide the missing data or regenerate the routes using `MapboxNavigation#requestRoutes`.
  [#5411](https://github.com/mapbox/mapbox-navigation-android/pull/5411)

#### Bug fixes and improvements
- Added `MapboxNavigationApp.setup` overload, that accepts `NavigationOptionsProvider` instead of prebuilt `NavigationOptions`. [#5490](https://github.com/mapbox/mapbox-navigation-android/pull/5490)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.3.0))
- Mapbox Navigation Native `v88.0.0`
- Mapbox Core Common `v21.1.1`
- Mapbox Java `v6.4.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.4.0-beta.1))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.3.0-rc.3 - February 24, 2022
This release is a re-tag of the `v2.3.0-rc.2` to correct the commit which should represent the state of the codebase. There are no functional differences between these releases.
### Changelog
[Changes between v2.3.0-rc.2 and v2.3.0-rc.3](https://github.com/mapbox/mapbox-navigation-android/compare/v2.3.0-rc.2...v2.3.0-rc.3)

#### Bug fixes and improvements
- (**Correction** - this workaround only actually became available in `v2.4.0-beta.3`) Added a workaround for [`ConnectivityManager`'s occasional security exception on Android 11 and older](https://issuetracker.google.com/issues/175055271). [#5492](https://github.com/mapbox/mapbox-navigation-android/pull/5492)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.3.0))
- Mapbox Navigation Native `v88.0.0`
- Mapbox Core Common `v21.1.1`
- Mapbox Java `v6.3.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.3.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.3.0-rc.2 - February 23, 2022
### Changelog
[Changes between v2.3.0-rc.1 and v2.3.0-rc.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.3.0-rc.1...v2.3.0-rc.2)

#### Bug fixes and improvements
- (**Correction** - this workaround only actually became available in `v2.4.0-beta.3`) Added a workaround for [`ConnectivityManager`'s occasional security exception on Android 11 and older](https://issuetracker.google.com/issues/175055271). [#5492](https://github.com/mapbox/mapbox-navigation-android/pull/5492)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.3.0))
- Mapbox Navigation Native `v88.0.0`
- Mapbox Core Common `v21.1.1`
- Mapbox Java `v6.3.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.3.0))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.3.0-rc.1 - February 17, 2022

#### Bug fixes and improvements
- Fixed using `POST` for long requests [#5480](https://github.com/mapbox/mapbox-navigation-android/pull/5480)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.3.0))
- Mapbox Navigation Native `v88.0.0`
- Mapbox Core Common `v21.1.0`
- Mapbox Java `v6.3.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.3.0-beta.1))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.3.0-beta.3 - February 11, 2022
### Changelog
[Changes between v2.3.0-beta.2 and v2.3.0-beta.3](https://github.com/mapbox/mapbox-navigation-android/compare/v2.3.0-beta.2...v2.3.0-beta.3)

#### Bug fixes and improvements
- Added `MapboxNavigationApp.isSetup` to ensure views do not reset `MapboxNavigation`. Added `MapboxNavigationApp.getObserver` to be able to access registered observers. [#5358](https://github.com/mapbox/mapbox-navigation-android/pull/5358)
- Refactored maneuver implementation to return a new `Maneuver` instance whenever the data in an existing `Maneuver` object needs to be modified. [#5453](https://github.com/mapbox/mapbox-navigation-android/pull/5453)
- Updated `MapboxManeuverView` / `MapboxRoadNameView` to prefer Mapbox designed shields over legacy ones. [#5445](https://github.com/mapbox/mapbox-navigation-android/pull/5445)
- Fixed `Nav Telemetry` to not run if `Telemetry events` is disabled. [#5455](https://github.com/mapbox/mapbox-navigation-android/pull/5455)
- Updated `MapboxNavigation#provideFeedbackMetadataWrapper` to throw `IllegalStateException` if `Telemetry events` is disabled. [#5455](https://github.com/mapbox/mapbox-navigation-android/pull/5455)
- Fixed an issue where some resource requests dispatched by Nav SDK (namely junction images and shields) were not wired through the same HTTP client instance that can be interacted with using `HttpServiceFactory.getInstance()`. Note: `MapboxSpeechApi` requests are still wired through an independent HTTP client. [#5459](https://github.com/mapbox/mapbox-navigation-android/pull/5459)
- Updated maneuver icons for notifications. [#5449](https://github.com/mapbox/mapbox-navigation-android/pull/5449)
- Fixed an issue where `off-route` was always false if there are no routing tiles available. [#5457](https://github.com/mapbox/mapbox-navigation-android/pull/5457)
- Added a default timeout for `Online` route requests (60 seconds). [#5457](https://github.com/mapbox/mapbox-navigation-android/pull/5457)
- Fixed an issue where an alternative route was equal a current route. [#5457](https://github.com/mapbox/mapbox-navigation-android/pull/5457)
- Added delay before re-attempting to request alternatives in case the first attempt resulted in the deviation point behind the current position. [#5457](https://github.com/mapbox/mapbox-navigation-android/pull/5457)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.3.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.3.0-rc.1))
- Mapbox Navigation Native `v87.0.5`
- Mapbox Core Common `v21.1.0-rc.1`
- Mapbox Java `v6.3.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.3.0-beta.1))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.3.0-beta.2 - February 2, 2022
### Changelog
[Changes between v2.3.0-beta.1 and v2.3.0-beta.2](https://github.com/mapbox/mapbox-navigation-android/compare/v2.3.0-beta.1...v2.3.0-beta.2)

#### Bug fixes and improvements
- Refactored `Status` class to POJO and decreased its constructor visibility. Introduced `StatusFactory` class for public use. [#5432](https://github.com/mapbox/mapbox-navigation-android/pull/5432)
- Fixed an issue where setting a custom `MapboxRouteLineOptions#vanishingRouteLineUpdateIntervalNano` that happened to be longer than the typical rate of `RouteProgress` updates delivered via `mapboxRouteLineApi#updateWithRouteProgress` would result in the traveled portion of the route not vanishing at all. [#5435](https://github.com/mapbox/mapbox-navigation-android/pull/5435)
- Fixed an issue where `MapboxManeuverView` or `MapboxRoadNameView` displayed wrong shields. [#5426](https://github.com/mapbox/mapbox-navigation-android/pull/5426)
- Updated the turn icons used in Mapbox Android Nav SDK. [#5430](https://github.com/mapbox/mapbox-navigation-android/pull/5430)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.3.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.3.0-rc.1))
- Mapbox Navigation Native `v87.0.2`
- Mapbox Core Common `v21.1.0-rc.1`
- Mapbox Java `v6.3.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.3.0-beta.1))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.3.0-beta.1 - January 28, 2022
#### Features
- Added new `StatusView` component for displaying SDK status messages. [#5403](https://github.com/mapbox/mapbox-navigation-android/pull/5403)

#### Bug fixes and improvements
- Added options to the `MapboxRouteLineOptions` class to control the icon anchor and icon offset for the route line related waypoint icons including the origin and destination points. [#5409](https://github.com/mapbox/mapbox-navigation-android/pull/5409)
- Fixed an issue where `RouteAlternativesObserver` and `RouteAlternativesRequestCallback` failed to deliver any alternatives due to an unrecognized `"closure"` annotation parameter in the native controller. [#5421](https://github.com/mapbox/mapbox-navigation-android/pull/5421)
- Fixed an issue where a reference to the registered `RouteAlternativesObserver` was not released after unsubscribing or destroying `MapboxNavigation`. [#5421](https://github.com/mapbox/mapbox-navigation-android/pull/5421)
- Improved `RouteAlternativesObserver` to ensure that alternative routes request is fired on each new leg start. [#5421](https://github.com/mapbox/mapbox-navigation-android/pull/5421)
- Added `avoidManeuverSeconds` param to `RouteAlternativesOptions`. [#5394](https://github.com/mapbox/mapbox-navigation-android/pull/5394)
- Fixed an issue found in `v2.3.0-alpha.1` where deprecated fields in `Road` type were always returning `null`s instead of the correct values. [#5396](https://github.com/mapbox/mapbox-navigation-android/pull/5396)
- Fixed an issue where `MapboxPrimaryManeuver` defaults to using exit signs based on VIENNA convention instead of MUTCD. [#5413](https://github.com/mapbox/mapbox-navigation-android/pull/5413)
- Fixed an issue where `MapboxRoadNameView` would continue to show previous road name in case the current `Road` has no data to show. [#5417](https://github.com/mapbox/mapbox-navigation-android/pull/5417)

## Mapbox Navigation SDK 2.2.1 - January 24, 2022
### Changelog
[Changes between v2.2.0 and v2.2.1](https://github.com/mapbox/mapbox-navigation-android/compare/v2.2.0...v2.2.1):

#### Bug fixes and improvements
- Fixed an issue where the _Onboard_ Router (used in offline scenarios) couldn't dispatch routes with tiles versions after and including `2021_12_25-03_00_00`.

## Mapbox Navigation SDK 2.2.0 - January 20, 2022

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Known issues
- :exclamation: The _Onboard_ Router (used in offline scenarios) can't dispatch routes with tiles versions after and including `2021_12_25-03_00_00`. If a tiles version is left unspecified, Navigation SDK will download the latest version. Therefore, in order to work around this, the tiles version needs to be manually specified to any version before `2021_12_25-03_00_00` (e.g. `2021_02_14-03_00_00`). This can be done specifying the tiles version in the routing tiles options `RoutingTilesOptions#tilesVersion` and adding those options to the navigation options `NavigationOptions#routingTilesOptions`.

### Changelog
[Changes between v2.1.2 and v2.2.0](https://github.com/mapbox/mapbox-navigation-android/compare/v2.1.2...v2.2.0):

#### Features
- Added support for Mapbox designed route shields. [#5145](https://github.com/mapbox/mapbox-navigation-android/pull/5145)
  - To access the newly introduced `MapboxRouteShieldApi` independently of the `MapboxManeuverApi`, add the dependency to your project:
    ```
    implementation "com.mapbox.navigation:ui-shields:{nav_sdk_version}"
    ```
- Added methods in `MapboxRouteLineView` to change the visibility of the route traffic layers. [#5283](https://github.com/mapbox/mapbox-navigation-android/pull/5283)
#### Bug fixes and improvements
- Exposed `ManeuverExitOptions`, `ManeuverPrimaryOptions`, `ManeuverSecondaryOptions`, `ManeuverSubOptions`, `MapboxExitProperties` to define the style for `MapboxPrimaryManeuver`, `MapboxSecondaryManeuver`, `MapboxSubManeuver` and associated `MapboxExitText` for each of these maneuvers. [#5357](https://github.com/mapbox/mapbox-navigation-android/pull/5357)
- Refactored the exit text logic to handle the drawable size based on the size of text associated in `MapboxExitText`. [#5357](https://github.com/mapbox/mapbox-navigation-android/pull/5357)
- Deprecated `MapboxRoadNameLabelView` and introduced `MapboxRoadNameView` to render mapbox designed shields in the current road name label. [#5310](https://github.com/mapbox/mapbox-navigation-android/pull/5310)
- Fixed an issue where calculation of dangerous maneuver avoidance (`RerouteOptions#avoidManeuverSeconds`) during a reroute was resulting in much smaller radius than expected. [#5307](https://github.com/mapbox/mapbox-navigation-android/pull/5307)
- Fixed a crash when `ReplayLocationEngine`'s location callback removes itself during getting a location update. [#5305](https://github.com/mapbox/mapbox-navigation-android/pull/5305)
- Fixed `MapboxTripProgressView` landscape layout to handle `tripProgressViewBackgroundColor` attribute. [#5318](https://github.com/mapbox/mapbox-navigation-android/pull/5318)
- Fixed an issue where the `onPause` is not called when the app is backgrounded and the implementation is using `MapboxNavigationApp.attachAllActivities`. [#5329](https://github.com/mapbox/mapbox-navigation-android/pull/5329)
- Fixed a crash when non-`driving-traffic` profile uses extension `RouteOptions.Builder#applyDefaultNavigationOptions`. [#5322](https://github.com/mapbox/mapbox-navigation-android/pull/5322)
- Refactored extension `RouteOptions.Builder#applyDefaultNavigationOptions`, might be set profile param explicitly. [#5322](https://github.com/mapbox/mapbox-navigation-android/pull/5322)
- Exposed a new API `MapboxManeuverView.updateLaneGuidanceIconStyle` that would allow changing the style of `MapboxLaneGuidanceAdapter` at runtime. [#5334](https://github.com/mapbox/mapbox-navigation-android/pull/5334)
- Fixed a crash when `MapboxNavigationViewportDataSourceDebugger.enabled` is repeatedly set to true. [#5347](https://github.com/mapbox/mapbox-navigation-android/pull/5347)
- Implemented vanishing route line feature from 1.x for exposing an option to adjust/limit the frequency of the vanishing route line updates. The MapboxRouteLineOptions.vanishingRouteLineUpdateIntervalNano can reduce the frequency of vanishing route line updates when the value of the option increases. [#5344](https://github.com/mapbox/mapbox-navigation-android/pull/5344)
- Fixed: crashes and waypoints accumulation during rerouting. [#5261](https://github.com/mapbox/mapbox-navigation-android/pull/5261)
- Added `MapboxDistanceUtil` which moves some of the implementation from `MapboxDistanceFormatter` so that the calculation of the distance rounding and the accompanying text is separated from the SpannableString construction done in  `MapboxDistanceFormatter`. [#5182](https://github.com/mapbox/mapbox-navigation-android/pull/5182)
- Switched `targetSdkVersion` to `31`. [#5259](https://github.com/mapbox/mapbox-navigation-android/pull/5259)
- Fixed empty profile in feedback events. [#5256](https://github.com/mapbox/mapbox-navigation-android/pull/5256)
- Added support for orientation changes for granular `LifecycleOwner`s to the `MapboxNavigationApp`. [#5234](https://github.com/mapbox/mapbox-navigation-android/pull/5234)
- Refactored `RouteProgress.voiceInstructions` property to always keep the last value and never become `null`. [#5126](https://github.com/mapbox/mapbox-navigation-android/pull/5126)
- Added `MapboxNavigationApp` and `MapboxNavigationObserver` to handle `MapboxNavigation` lifecycles. [#5112](https://github.com/mapbox/mapbox-navigation-android/pull/5112)
- Added a new object `NavigationStyles` that exposes links to default navigation day and night styles. [#5153](https://github.com/mapbox/mapbox-navigation-android/pull/5153)
- Fixed notification appearance on Android 12.[#5159](https://github.com/mapbox/mapbox-navigation-android/pull/5159)
- Add checkIsLanguageAvailable flag to VoiceInstructionsPlayerOptions. [#5166](https://github.com/mapbox/mapbox-navigation-android/pull/5166)
- Fixed an issue where the origin of new alternative routes was always reported as `Onboard`. [#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Added support for `include` route request parameter for onboard router. [#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Added support for `exclude_cash_only_tolls` route request parameter for onboard router. [#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Fixed issue where off-route might not have been detected on complex overpass roads. [#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Fixed an issue where toll and motorway route exclusions were not respected for onboard router. [#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Fixed an issue where `IncidentInfo#id` might've been incorrect when the incident was part of the route response.[#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Added support for `ShieldSprites`, `ShieldSprite` and `ShieldSpriteAttribute` for mapbox designed route shields.[#5184](https://github.com/mapbox/mapbox-navigation-android/pull/5184)
- Exposed a callback for `MapboxNavigation#requestAlternativeRoutes` to track progress of the on-demand alternative routes request.[#5189](https://github.com/mapbox/mapbox-navigation-android/pull/5189)
- Fixed an issue where route request failure (due to incorrect parameters) led to a parsing error and runtime crash instead of failure callback. [#5139](https://github.com/mapbox/mapbox-navigation-android/pull/5139)
- Fixed an issue where reroute controller attempted to add request parameters unsuitable for the selected profile. [#5140](https://github.com/mapbox/mapbox-navigation-android/pull/5140)
- Added `RerouteOptions` to define reroute params for default `RereouteController`. [#5056](https://github.com/mapbox/mapbox-navigation-android/pull/5056)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK v10.2.0 ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.2.0))
- Mapbox Navigation Native v83.0.0
- Mapbox Core Common v21.0.1
- Mapbox Java v6.2.0 ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.2.0))
- Mapbox Android Core v5.0.1
- Mapbox Android Telemetry v8.1.1

## Mapbox Navigation SDK 2.1.2 - January 18, 2022

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Fixed decoding of OpenLR point along line. [5369](https://github.com/mapbox/mapbox-navigation-android/pull/5369)
- Fixed multi language support in onboard router. [5369](https://github.com/mapbox/mapbox-navigation-android/pull/5369)
- Fixed native memory leak. [5369](https://github.com/mapbox/mapbox-navigation-android/pull/5369)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.2))
- Mapbox Navigation Native `v79.0.4`
- Mapbox Core Common `v20.0.3`
- Mapbox Java `v6.1.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.1.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.3.0-alpha.1 - January 14, 2021

### Known issues
- The `RouteAlternativesObserver` and `RouteAlternativesRequestCallback` fail to deliver any alternatives due to an unrecognized parameter in the native controller.

### Changelog
#### Bug fixes and improvements
- Add geo deeplink parsing utility `GeoDeeplinkParser`. [#5103](https://github.com/mapbox/mapbox-navigation-android/pull/5103)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.3.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.3.0-beta.1))
- Mapbox Navigation Native `v86.0.2`
- Mapbox Core Common `v21.1.0-beta.1`
- Mapbox Java `v6.2.0-beta.2` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.2.0-beta.2))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.2.0-rc.1 - January 13, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Fixed an issue where calculation of dangerous maneuver avoidance (`RerouteOptions#avoidManeuverSeconds`) during a reroute was resulting in much smaller radius than expected. [#5307](https://github.com/mapbox/mapbox-navigation-android/pull/5307)
- Fixed a crash when `ReplayLocationEngine`'s location callback removes itself during getting a location update. [#5305](https://github.com/mapbox/mapbox-navigation-android/pull/5305)
- Fixed `MapboxTripProgressView` landscape layout to handle `tripProgressViewBackgroundColor` attribute. [#5318](https://github.com/mapbox/mapbox-navigation-android/pull/5318)
- Fixed an issue where the `onPause` is not called when the app is backgrounded and the implementation is using `MapboxNavigationApp.attachAllActivities`. [#5329](https://github.com/mapbox/mapbox-navigation-android/pull/5329)
- Fixed a crash when non-`driving-traffic` profile uses extension `RouteOptions.Builder#applyDefaultNavigationOptions`. [#5322](https://github.com/mapbox/mapbox-navigation-android/pull/5322)
- Refactored extension `RouteOptions.Builder#applyDefaultNavigationOptions`, might be set profile param explicitly. [#5322](https://github.com/mapbox/mapbox-navigation-android/pull/5322)
- Exposed a new API `MapboxManeuverView.updateLaneGuidanceIconStyle` that would allow changing the style of `MapboxLaneGuidanceAdapter` at runtime. [#5334](https://github.com/mapbox/mapbox-navigation-android/pull/5334)
- Fixed a crash when `MapboxNavigationViewportDataSourceDebugger.enabled` is repeatedly set to true. [#5347](https://github.com/mapbox/mapbox-navigation-android/pull/5347)
- Implemented vanishing route line feature from 1.x for exposing an option to adjust/limit the frequency of the vanishing route line updates. The MapboxRouteLineOptions.vanishingRouteLineUpdateIntervalNano can reduce the frequency of vanishing route line updates when the value of the option increases. [#5344](https://github.com/mapbox/mapbox-navigation-android/pull/5344)
- Fixed `RoadShield` by reverting the breaking changes and use the new shield callback. [#5302](https://github.com/mapbox/mapbox-navigation-android/pull/5302)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.2.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.2.0))
- Mapbox Navigation Native `v83.0.0`
- Mapbox Core Common `v21.0.1`
- Mapbox Java `v6.2.0-beta.2` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.2.0-beta.2))
- Mapbox Android Core `v5.0.1`
- Mapbox Android Telemetry `v8.1.1`

## Mapbox Navigation SDK 2.0.5 - January 7, 2022
This is a patch release on top of `v2.0.x` which does not include changes introduced in `v2.1.x` and later.

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Fixed a small but limitless memory growth when processing location updates. [#5337](https://github.com/mapbox/mapbox-navigation-android/pull/5337)
- Fixed decoding of OpenLR point along line. [#5337](https://github.com/mapbox/mapbox-navigation-android/pull/5337)
- Fixed an issue of falling back to the same version of routing tiles that we are currently on. [#5337](https://github.com/mapbox/mapbox-navigation-android/pull/5337)
- Fixed OOM Exception because of non-limited internal cached of alternative routes. [#5323](https://github.com/mapbox/mapbox-navigation-android/pull/5323)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.2))
- Mapbox Navigation Native `v69.0.4`
- Mapbox Core Common `v20.0.3`
- Mapbox Java `v6.0.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.2.0-beta.1 - December 17, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Added support for Mapbox designed route shields. [#5145](https://github.com/mapbox/mapbox-navigation-android/pull/5145)
  - To access the newly introduced `MapboxRouteShieldApi` independently of the `MapboxManeuverApi`, add the dependency to your project:
    ```
    implementation "com.mapbox.navigation:ui-shields:{nav_sdk_version}"
    ```
- Added methods in `MapboxRouteLineView` to change the visibility of the route traffic layers. [#5283](https://github.com/mapbox/mapbox-navigation-android/pull/5283)
#### Bug fixes and improvements
- Fixed: crashes and waypoints accumulation during rerouting. [#5261](https://github.com/mapbox/mapbox-navigation-android/pull/5261)
- Added `MapboxDistanceUtil` which moves some of the implementation from `MapboxDistanceFormatter` so that the calculation of the distance rounding and the accompanying text is separated from the SpannableString construction done in  `MapboxDistanceFormatter`. [#5182](https://github.com/mapbox/mapbox-navigation-android/pull/5182)
- Bumped `targetSdkVersion` to `31`. [#5259](https://github.com/mapbox/mapbox-navigation-android/pull/5259)
- Fixed empty profile in feedback events. [#5256](https://github.com/mapbox/mapbox-navigation-android/pull/5256)
- Added support for orientation changes for granular `LifecycleOwner`s to the `MapboxNavigationApp`. [#5234](https://github.com/mapbox/mapbox-navigation-android/pull/5234)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.2.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.2.0))
- Mapbox Navigation Native `v82.0.1`
- Mapbox Core Common `v21.0.1`
- Mapbox Java `v6.2.0-beta.2` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.2.0-beta.2))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.1.1 - December 13, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Fixed billing issue when upgrading Mapbox Maps SDK from v9 to v10. [#5263](https://github.com/mapbox/mapbox-navigation-android/pull/5263)
- Fixed notification appearance on Android 12. [#5159](https://github.com/mapbox/mapbox-navigation-android/pull/5159)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.2))
- Mapbox Navigation Native `v79.0.3`
- Mapbox Core Common `v20.0.3`
- Mapbox Java `v6.1.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.1.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.4 - December 13, 2021
This is a patch release on top of `v2.0.x` which does not include changes introduced in `v2.1.x` and later.

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Fixed billing issue when upgrading Mapbox Maps SDK from v9 to v10. [#5262](https://github.com/mapbox/mapbox-navigation-android/pull/5262)
- Fixed notification appearance on Android 12. [#5159](https://github.com/mapbox/mapbox-navigation-android/pull/5159)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.2))
- Mapbox Navigation Native `v69.0.3`
- Mapbox Core Common `v20.0.3`
- Mapbox Java `v6.0.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.2.0-alpha.3 - December 10, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Refactored `RouteProgress.voiceInstructions` property to always keep the last value and never become `null`. [#5126](https://github.com/mapbox/mapbox-navigation-android/pull/5126)
- Added `MapboxNavigationApp` and `MapboxNavigationObserver` to handle `MapboxNavigation` lifecycles. [#5112](https://github.com/mapbox/mapbox-navigation-android/pull/5112)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.2.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.2.0-beta.1))
- Mapbox Navigation Native `v82.0.0`
- Mapbox Core Common `v21.0.0-rc.2`
- Mapbox Java `v6.2.0-beta.2` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.2.0-beta.2))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.2.0-alpha.1 - December 3, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Added a new object `NavigationStyles` that exposes links to default navigation day and night styles. [#5153](https://github.com/mapbox/mapbox-navigation-android/pull/5153)
- Fixed notification appearance on Android 12.[#5159](https://github.com/mapbox/mapbox-navigation-android/pull/5159)
- Add checkIsLanguageAvailable flag to VoiceInstructionsPlayerOptions. [#5166](https://github.com/mapbox/mapbox-navigation-android/pull/5166)
- Fixed an issue where the origin of new alternative routes was always reported as `Onboard`. [#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Added support for `include` route request parameter for onboard router. [#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Added support for `exclude_cash_only_tolls` route request parameter for onboard router. [#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Fixed issue where off-route might not have been detected on complex overpass roads. [#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Fixed an issue where toll and motorway route exclusions were not respected for onboard router. [#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Fixed an issue where `IncidentInfo#id` might've been incorrect when the incident was part of the route response.[#5167](https://github.com/mapbox/mapbox-navigation-android/pull/5167)
- Added support for `ShieldSprites`, `ShieldSprite` and `ShieldSpriteAttribute` for mapbox designed route shields.[#5184](https://github.com/mapbox/mapbox-navigation-android/pull/5184)
- Exposed a callback for `MapboxNavigation#requestAlternativeRoutes` to track progress of the on-demand alternative routes request.[#5189](https://github.com/mapbox/mapbox-navigation-android/pull/5189)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.2.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.2.0-beta.1))
- Mapbox Navigation Native `v81.0.0`
- Mapbox Core Common `v21.0.0-rc.1`
- Mapbox Java `v6.2.0-beta.2` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.2.0-beta.2))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.1.0 - December 2, 2021
Mapbox Navigation SDK v2.1.0 was released prematurely with an unresolved billing issue. We deleted this versions from the Maven repository and we’re preparing the v2.1.1 release with additional fixes.

We strongly recommend not to release your application with v2.1.0. More information will be available in [this announcement](https://github.com/mapbox/mapbox-navigation-android/discussions/5213).

## Mapbox Navigation SDK 2.0.3 - December 2, 2021
Mapbox Navigation SDK v2.0.3 was released prematurely with an unresolved billing issue. We deleted this versions from the Maven repository and we’re preparing the v2.0.4 release with additional fixes.

We strongly recommend not to release your application with v2.0.3. More information will be available in [this announcement](https://github.com/mapbox/mapbox-navigation-android/discussions/5213).

## Mapbox Navigation SDK 1.6.2 - November 22, 2021
This is a patch release on top of `v1.x` which does not include changes introduced in `v2.x` and later.

### Changelog
#### Bug fixes and improvements
- Added `PendingIntent` Android 12 flags support and fixed other Android 12 incompatibilities by upgrading transitive dependencies. [#5142](https://github.com/mapbox/mapbox-navigation-android/pull/5142)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v9.7.1` ([release notes](https://github.com/mapbox/mapbox-gl-native-android/releases/tag/android-v9.7.1))
- Mapbox Navigation Native `v32.0.0`
- Mapbox Core Common `v9.2.0`
- Mapbox Java `v5.9.0-alpha.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.1))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.2.0-alpha.1 - November 19, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Fixed an issue where route request failure (due to incorrect parameters) led to a parsing error and runtime crash instead of failure callback. [#5139](https://github.com/mapbox/mapbox-navigation-android/pull/5139)
- Fixed an issue where reroute controller attempted to add request parameters unsuitable for the selected profile. [#5140](https://github.com/mapbox/mapbox-navigation-android/pull/5140)
- Added `RerouteOptions` to define reroute params for default `RereouteController`. [#5056](https://github.com/mapbox/mapbox-navigation-android/pull/5056)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v80.0.2`
- Mapbox Core Common `v20.1.0`
- Mapbox Java `v6.2.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.2.0-beta.1))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.2 - November 18, 2021
This is a patch release on top of `v2.0.x` which does not include changes introduced in `v2.1.x` and later.

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Added `PedingIntent` Android 12 flags support. [#5121](https://github.com/mapbox/mapbox-navigation-android/pull/5121)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v69.0.3`
- Mapbox Core Common `v20.0.0`
- Mapbox Java `v6.0.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.1.0-rc.2 - November 18, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Trigger RouteAlternativesObserver on main thread. [#5120](https://github.com/mapbox/mapbox-navigation-android/pull/5120)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v79.0.3`
- Mapbox Core Common `v20.0.0`
- Mapbox Java `v6.1.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.1.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.1.0-rc.1 - November 12, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Added `PedingIntent` Android 12 flags support. [#5105](https://github.com/mapbox/mapbox-navigation-android/pull/5105)
- Fixed an issue where `congestion numeric` traffic annotations where not refreshed correctly. [#5078](https://github.com/mapbox/mapbox-navigation-android/pull/5078)
- Implemented predictive cache with `TilesetDescriptor` so that volatile sources are not loaded unexpectedly. [#5068](https://github.com/mapbox/mapbox-navigation-android/pull/5068)
- Fixed an issue with TTS config on pre-Oreo devices. [#5082](https://github.com/mapbox/mapbox-navigation-android/pull/5082)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v79.0.2`
- Mapbox Core Common `v20.0.0`
- Mapbox Java `v6.1.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.1.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.1 - November 10, 2021
This is a patch release on top of `v2.0.0` which does not include changes introduced in `v2.1.0-beta.1` and later. The `v2.1.0-beta.2` remains the latest and most up-to-date release.

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Refactored code to update traffic data based on congestion numeric for route refresh API calls. [#5091](https://github.com/mapbox/mapbox-navigation-android/pull/5091/files)
- Implemented predictive cache with `TilesetDescriptor` so that volatile sources are not loaded unexpectedly. Warning: `PredictiveCacheController.createStyleMapControllers` method needs to be used in order to cache only non-volatile sources. [#5089](https://github.com/mapbox/mapbox-navigation-android/pull/5089)
- Fixed a downstream issue of offline routing being slow. [#5093](https://github.com/mapbox/mapbox-navigation-android/pull/5093)
- Fixed toll and motorway exclude params functionality for onboard router. [#5093](https://github.com/mapbox/mapbox-navigation-android/pull/5093)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v69.0.3`
- Mapbox Core Common `v20.0.0`
- Mapbox Java `v6.0.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.1.0-beta.2 - November 4, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Added missing mutability methods to results returned by the route line API. [#5062](https://github.com/mapbox/mapbox-navigation-android/pull/5062)
- Fixed index out of bounds exception. [#5060](https://github.com/mapbox/mapbox-navigation-android/pull/5060)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v79.0.0`
- Mapbox Core Common `v20.0.0`
- Mapbox Java `v6.0.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.1))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.1.0-beta.1 - October 28, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- :warning: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta and is subject to changes, including its pricing. Use of the feature is subject to the beta product restrictions in the Mapbox Terms of Service. Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and require customers to place an order to purchase the Mapbox Electronic Horizon feature, regardless of the level of use of the feature. [#4985](https://github.com/mapbox/mapbox-navigation-android/pull/4985)
- Removed deprecated method from `RouteOptionsUpdater`. [#4970](https://github.com/mapbox/mapbox-navigation-android/pull/4970)
- Added a new `Road` object that can be obtained thorough `LocationMatcherResult`. [#4972](https://github.com/mapbox/mapbox-navigation-android/pull/4972)
- Added `MapboxRoadNameLabelApi` and `MapboxRoadNameLabelView` to support road name label feature. [#4974](https://github.com/mapbox/mapbox-navigation-android/pull/4974)
- Fixed an issue where the route line's elements (the primary route, alternatives, or the destination symbol) might intermittently not render or not update. [#4983](https://github.com/mapbox/mapbox-navigation-android/pull/4983)
- Added continuous route alternatives. [#4892](https://github.com/mapbox/mapbox-navigation-android/pull/4892)
- Moved `MapboxRouteLineApiExtensions` to a stable `com.mapbox.navigation.ui.maps.route.line` package. [#5001](https://github.com/mapbox/mapbox-navigation-android/pull/5001)
- Fixed spread legacy `reason` when subscribing on routes updated via `RoutesObserver`. [#5006](https://github.com/mapbox/mapbox-navigation-android/pull/5006)
- Removed unused `NavigationOption#isFromNavigationUi` option. [#5019](https://github.com/mapbox/mapbox-navigation-android/pull/5019)
- Updated standalone components, so that they are no longer tied to `MapboxNavigation` lifecycle. Warning: when such a component is no longer in use, its `cancel` or equivalent function should be called in order to avoid leaks. [#5031](https://github.com/mapbox/mapbox-navigation-android/pull/5031)
- Fixed clear `VoiceInstructionsTextPlayer` `currentPlay` when `shutdown`. [#5032](https://github.com/mapbox/mapbox-navigation-android/pull/5032)
- Changed the `iconKeepUpright` parameter for the layer hosting the waypoints is set to `true` to address issue with some custom icons that may be intended to appear like 3D pins. [#5047](https://github.com/mapbox/mapbox-navigation-android/pull/5047)

#### Known issues
- `RouteAlternativesObserver#onRouteAlternatives` always reports `RouterOrigin` as `Onboard` even if the results are coming from the offboard router. This is can be tracked in [#5127](https://github.com/mapbox/mapbox-navigation-android/issues/5127).
- Taking an alternative route (by turning into it instead of selecting manually) triggers a reroute. This is can be tracked in [#5039](ttps://github.com/mapbox/mapbox-navigation-android/issues/5039).

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v79.0.0`
- Mapbox Core Common `v20.0.0`
- Mapbox Java `v6.0.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0 - October 21, 2021

Today, we are releasing Navigation SDK (Core & UI components) v2.0 🎉 🚗 🚀

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2, see [Navigation SDK documentation](https://docs.mapbox.com/android/beta/navigation/guides/) and [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Removed unnecessary Sensor APIs. [#5020](https://github.com/mapbox/mapbox-navigation-android/pull/5020)
- Removed unused `NavigationOptions#isFromNavigationUi` option. [#5019](https://github.com/mapbox/mapbox-navigation-android/pull/5019)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v69.0.2`
- Mapbox Core Common `v20.0.0`
- Mapbox Java `v6.0.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-rc.8 - October 19, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Moved `MapboxRouteLineApiExtensions` to a stable `com.mapbox.navigation.ui.maps.route.line` package. [#5001](https://github.com/mapbox/mapbox-navigation-android/pull/5001)
- Fixed spread legacy `reason` when subscribing on routes updated via `RoutesObserver`. [#5006](https://github.com/mapbox/mapbox-navigation-android/pull/5006)
- Fixed and issue where after reroute an additional waypoint was added to the new route (equal to the starting point of the original route). [#4999](https://github.com/mapbox/mapbox-navigation-android/pull/4999)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v69.0.2`
- Mapbox Core Common `v20.0.0`
- Mapbox Java `v6.0.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-rc.7 - October 15, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- :warning: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta and is subject to changes, including its pricing. Use of the feature is subject to the beta product restrictions in the Mapbox Terms of Service. Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and require customers to place an order to purchase the Mapbox Electronic Horizon feature, regardless of the level of use of the feature. [#4985](https://github.com/mapbox/mapbox-navigation-android/pull/4985)
- Fixed an issue where the route line's elements (the primary route, alternatives, or the destination symbol) might intermittently not render or not update. [#4983](https://github.com/mapbox/mapbox-navigation-android/pull/4983)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v69.0.1`
- Mapbox Core Common `v20.0.0`
- Mapbox Java `v6.0.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-rc.6 - October 14, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Removed deprecated method from `RouteOptionsUpdater::update`. [#4970](https://github.com/mapbox/mapbox-navigation-android/pull/4970)
- Added a new `Road` object that can be obtained thorough `LocationMatcherResult`. [#4972](https://github.com/mapbox/mapbox-navigation-android/pull/4972)
- Changed `RouteOptionsUpdater::update` signature to accept `LocationMatcherResult`. [#4973](https://github.com/mapbox/mapbox-navigation-android/pull/4973)
- Added API and View to support road name label feature. [#4974](https://github.com/mapbox/mapbox-navigation-android/pull/4974)


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v69.0.1`
- Mapbox Core Common `v20.0.0`
- Mapbox Java `v6.0.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-rc.5 - October 8, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Fixed an issue, which led to `MapboxReplayer` using wrong coroutine scope. Warning: when `MapboxPlayer` is no longer in use, its `finish` functions should be called in order to avoid leaks. [#4929](https://github.com/mapbox/mapbox-navigation-android/pull/4929)
- Removed hardcoded color references from lane drawables. [#4933](https://github.com/mapbox/mapbox-navigation-android/pull/4933)
- Added callback to MapboxNavigation::navigateNextRouteLeg method. [#4938](https://github.com/mapbox/mapbox-navigation-android/pull/4938)
- Added callback to MapboxNavigation::updateSensorEvent method. [#4938](https://github.com/mapbox/mapbox-navigation-android/pull/4938)
- ⚠️ Hidden unnecesarily exposed RouteLayerConstants and added a new RouteLayerConstants#BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID next to RouteLayerConstants#TOP_LEVEL_ROUTE_LINE_LAYER_ID to help position other map layers in reference to the route line layers stack. [#4941](https://github.com/mapbox/mapbox-navigation-android/pull/4941)
- ⚠️ Changed RouteSetValue to not assume only 2 alternative route lines. The data/expression sources for alternatives are now in a collection form. The API currently only support up to 2 alternative routes but this will be expanded in the future. [#4941](https://github.com/mapbox/mapbox-navigation-android/pull/4941)
- Added utilities to capture and encode screenshots, which are now required when providing feedback. [#4951](https://github.com/mapbox/mapbox-navigation-android/pull/4951)
- Changed `RoutesObserver`: `RoutesUpdatedResult` contains `routes` and `reason`, that provides reason why routes are updated. [#4952](https://github.com/mapbox/mapbox-navigation-android/pull/4952)
- Fixed stop session button no longer working when in Replay mode. [#4954](https://github.com/mapbox/mapbox-navigation-android/pull/4954)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0-rc.9` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.9))
- Mapbox Navigation Native `v68.0.0`
- Mapbox Core Common `v19.0.0`
- Mapbox Java `v6.0.0-alpha.7` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.7))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.1.0-alpha.2 - October 7, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Changed `RoutesObserver`: `RoutesUpdatedResult` contains `routes` and `reason`, that provides reason why routes are updated. [#4862](https://github.com/mapbox/mapbox-navigation-android/pull/4862)
- Added callback to MapboxNavigation::navigateNextRouteLeg method. [#4881](https://github.com/mapbox/mapbox-navigation-android/pull/4881)
- Added callback to MapboxNavigation::updateSensorEvent method. [#4881](https://github.com/mapbox/mapbox-navigation-android/pull/4881)
- Removed logging message that under some conditions could cause an ANR due to the construction of the logging message. [#4900](https://github.com/mapbox/mapbox-navigation-android/pull/4900)
- Added a cancel method to the MapboxRouteLineApi and MapboxRouteLineView classes for cancelling the background tasks. [#4911](https://github.com/mapbox/mapbox-navigation-android/pull/4911)
- :warning: Hidden unnecesarily exposed `RouteLayerConstants` and added a new `RouteLayerConstants#BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID` next to `RouteLayerConstants#TOP_LEVEL_ROUTE_LINE_LAYER_ID` to help position other map layers in reference to the route line layers stack. [#4913](https://github.com/mapbox/mapbox-navigation-android/pull/4913)
- :warning: Changed `RouteSetValue` to not assume only 2 alternative route lines. The data/expression sources for alternatives are now in a collection form. The API currently only support up to 2 alternative routes but this will be expanded in the future. [#4913](https://github.com/mapbox/mapbox-navigation-android/pull/4913)
- Fixed an issue, which led to `MapboxReplayer` using wrong coroutine scope. Warning: when `MapboxPlayer` is no longer in use, its `finish` functions should be called in order to avoid leaks. [#4921](https://github.com/mapbox/mapbox-navigation-android/pull/4921)
- Fixed hard code color references from color drawables. [#4928](https://github.com/mapbox/mapbox-navigation-android/pull/4928)
- Added method in MapboxRouteLineApi to set the road classes, replacing the road classes in the RouteLineResources. [#4939](https://github.com/mapbox/mapbox-navigation-android/pull/4939)
- Added utilities to capture and encode screenshots, which are now required when providing feedback. [#4942](https://github.com/mapbox/mapbox-navigation-android/pull/4942)
- Fixed stop session button no longer working when in Replay mode. [#4953](https://github.com/mapbox/mapbox-navigation-android/pull/4953)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0))
- Mapbox Navigation Native `v78.0.0`
- Mapbox Core Common `v20.0.0`
- Mapbox Java `v6.0.0-alpha.9` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.9))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-rc.4 - September 30, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Added a cancel method to the `MapboxRouteLineApi` and `MapboxRouteLineView` classes for cancelling the background tasks. [#4911](https://github.com/mapbox/mapbox-navigation-android/pull/4911)
- Removed logging message that under some conditions could cause an ANR due to the construction of the logging message. [#4900](https://github.com/mapbox/mapbox-navigation-android/pull/4900)
- Fixed an issue where pausing a free drive session did not extend the session's validity correctly. [#4912](https://github.com/mapbox/mapbox-navigation-android/pull/4912)
- Fixed an issue where both `navmaus` and `nav2sesmau` SKUs could be counted when using Nav SDK v2. [#4912](https://github.com/mapbox/mapbox-navigation-android/pull/4912)
- Fixed compatibility issues with 32-bit devices that could've resulted in incorrect location updates. [#4919](https://github.com/mapbox/mapbox-navigation-android/pull/4919)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0-rc.9` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.9))
- Mapbox Navigation Native `v68.0.0`
- Mapbox Core Common `v19.0.0`
- Mapbox Java `v6.0.0-alpha.7` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.7))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.1.0-alpha.1 - September 24, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Exposed `MapboxNavigation#getZLevel` that can be used with `RouteOptions#layers` to influence the layer of the road from where the route starts. [#4800](https://github.com/mapbox/mapbox-navigation-android/pull/4800) [#4849](https://github.com/mapbox/mapbox-navigation-android/pull/4849)
- Exposed a reusable route geometry cache via `DecodeUtils`. [#4784](https://github.com/mapbox/mapbox-navigation-android/pull/4784)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0-rc.8` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.8))
- Mapbox Navigation Native `v66.0.3`
- Mapbox Core Common `v18.0.0`
- Mapbox Java `v6.0.0-alpha.7` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.7))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-rc.3 - September 24, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog

#### Bug fixes and improvements
- :warning: Exposed include-hov/hot parameters in `RouteOptions`. [#4894](https://github.com/mapbox/mapbox-navigation-android/pull/4894)
- :warning: Added `exclude` list option. [#4894](https://github.com/mapbox/mapbox-navigation-android/pull/4894)
- :warning: Removed unnecessary `RouteExclusions` `exclude` extension. [#4894](https://github.com/mapbox/mapbox-navigation-android/pull/4894)
- Increased location tolerance for vanishing route line updates. This resolves rare occasions where the vanishing route line portion would briefly stop updating. [#4888](https://github.com/mapbox/mapbox-navigation-android/pull/4888)
- Added `MapboxNavigation#provideFeedbackMetadataWrapper`: provides metadata to post a deffered feedback [#4889](https://github.com/mapbox/mapbox-navigation-android/pull/4889)
- Added `FeedbackMetadata`: holds data that might be used to post a deferred user's feedback. [#4889](https://github.com/mapbox/mapbox-navigation-android/pull/4889)
- Added `FeedbackMetadataWrapper`: wraps `FeedbackMetadata` and collect additional information to inflate one. [#4889](https://github.com/mapbox/mapbox-navigation-android/pull/4889)
- Added to overloaded `MapboxNavigation#postUserFeedback` with param `feedbackMetadata: FeedbackMetadata?` [#4889](https://github.com/mapbox/mapbox-navigation-android/pull/4889)
- :warning: Renamed `MapMatcherResult` to `LocationMatcherResult` [#4886](https://github.com/mapbox/mapbox-navigation-android/pull/4886)
- :warning: Merged `LocationObserver` and `MapMatcherResultObserver` interfaces into single `LocationObserver` interface [#4886](https://github.com/mapbox/mapbox-navigation-android/pull/4886)
- :warning: Refactored turn lane api and logic to handle more lane combinations. [#4885](https://github.com/mapbox/mapbox-navigation-android/pull/4885)
- Bug fix for calculating traffic on route line multi-leg routes. [#4883](https://github.com/mapbox/mapbox-navigation-android/pull/4883)
- Added `MapboxNavigation.startReplayTripSession` which allows you to use MapboxReplayer after `MapboxNavigation` has been created. [#4843](https://github.com/mapbox/mapbox-navigation-android/pull/4843)
- Added `MapboxTurnIconsApi` for retrieving turn icon drawables [#4864](https://github.com/mapbox/mapbox-navigation-android/pull/4864)
- Bug fix for traffic expressions using soft gradients. [#4866](https://github.com/mapbox/mapbox-navigation-android/pull/4866)
- Fixed an issue where restricted road sections layer was not re-added to the map when drawing data if it was previously manually removed. [#4861](https://github.com/mapbox/mapbox-navigation-android/pull/4861)
- Made route line, route arrow, and building highlighting layers persistent which means that they will survive style changes automatically, without needing to be manually redrawn. This also removes any sort of flickering of the route line when a map style changes. [#4861](https://github.com/mapbox/mapbox-navigation-android/pull/4861)
- :warning: Removed `RouteOptionsUpdater` interface, renamed `MapboxRouteOptionsUpdater` to `RouteOptionsUpdater` [#4852](https://github.com/mapbox/mapbox-navigation-android/pull/4852)
- Fixed an issue where banner instructions were missing after restarting trip session with the same route [#4851](https://github.com/mapbox/mapbox-navigation-android/pull/4851)
- Fixed the issue with music volume not restored after stopping voice instructions playback [#4899](https://github.com/mapbox/mapbox-navigation-android/pull/4899)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0-rc.8` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.8))
- Mapbox Navigation Native `v66.0.3`
- Mapbox Core Common `v18.0.0`
- Mapbox Java `v6.0.0-alpha.7` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.7))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-rc.2.1 - September 21, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog

#### Bug fixes and improvements
- Fixed `IndexOutOfBoundsException` in `MapboxRouteLineUtils` [4860](https://github.com/mapbox/mapbox-navigation-android/pull/4860)
- Fixed native out of range crash [4869](https://github.com/mapbox/mapbox-navigation-android/pull/4869)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0-rc.8` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.8))
- Mapbox Navigation Native `v66.0.3`
- Mapbox Core Common `v18.0.0`
- Mapbox Java `v6.0.0-alpha.6` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.6))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-rc.2 - September 16, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Added `FeedbackHelper` with utilities for working with feedback types and subtypes. [4813](https://github.com/mapbox/mapbox-navigation-android/pull/4813)

#### Bug fixes and improvements
- Fixed an issue where the route was never refreshed because of an internal timer not starting when a new route was set. [#4831](https://github.com/mapbox/mapbox-navigation-android/pull/4831)
- When downloading critical tiles, the Map tiles are preferred over Routing tiles to ensure that the map is visible as soon as possible. [#4826](https://github.com/mapbox/mapbox-navigation-android/pull/4826)
- Fixed an issue where `offRoute` and `BannerInstructions` states were not immediately reset after setting a new route. [#4810](https://github.com/mapbox/mapbox-navigation-android/pull/4810)
- :warning: Renamed `FeedbackEvent.Description` annotation to `FeedbackEvent.SubType`. [4813](https://github.com/mapbox/mapbox-navigation-android/pull/4813)
- Updated `Lane` UI component to handle more use cases with lane combinations. [4815](https://github.com/mapbox/mapbox-navigation-android/pull/4815) [#4820](https://github.com/mapbox/mapbox-navigation-android/pull/4820)
- :warning: Deprecated all matching methods that accept a single object in the `RoadObjectMatcher` in favor of methods that accept a list of matchable objects. [#4826](https://github.com/mapbox/mapbox-navigation-android/pull/4826)
- Fixed an issue when location could be map-matched to a wrong, nearby road near intersections. [#4826](https://github.com/mapbox/mapbox-navigation-android/pull/4826)
- Fixed an issue when occasionally there would be an off-route event emitted when leaving a tunnel. [#4826](https://github.com/mapbox/mapbox-navigation-android/pull/4826)
- Fixed an issue where location and progress updates could be skipped when resetting the session with `MapboxNavigation#resetTripSession`. [#4826](https://github.com/mapbox/mapbox-navigation-android/pull/4826)
- Fixed an issue with the wrong speed limit being returned around an off-route event. [#4826](https://github.com/mapbox/mapbox-navigation-android/pull/4826)
- Fixed an issue where EHorizon would sometimes report road objects that were already passed. [#4826](https://github.com/mapbox/mapbox-navigation-android/pull/4826)
- :warning: Fixed an issue where separate legs of a route were not counted as separate trips. [#4821](https://github.com/mapbox/mapbox-navigation-android/pull/4821)

#### Other changes
- :warning: Removed `MapboxNavigation#retrieveSsmlAnnouncementInstruction`. It did not work correctly and if you plan to introduce a voice cache manually, look for `LegStep#voiceInstructions` in the `RouteLeg` of a `DirectionsRoute`. [#4826](https://github.com/mapbox/mapbox-navigation-android/pull/4826)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0-rc.8` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.8))
- Mapbox Navigation Native `v66.0.1`
- Mapbox Core Common `v18.0.0`
- Mapbox Java `v6.0.0-alpha.6` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.6))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-rc.1 - September 09, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
- :exclamation: Introduced new pricing options. Integrating this or a later version of the Navigation SDK will impact the way you are billed. Read more in our [Pricing Documentation](https://docs.mapbox.com/android/beta/navigation/guides/pricing/). [#4666](https://github.com/mapbox/mapbox-navigation-android/pull/4666)
- :warning: `MapboxNavigation` now enforces having only one instance alive in a process, a new instance cannot be created if there's another one that did not have `#onDestroy` called. Use `MapboxNavigationProvider` for assistance with instance management. [#4666](https://github.com/mapbox/mapbox-navigation-android/pull/4666)

#### Features
- :warning: The implementation for representing restricted roads with a dashed line has been updated to support better scaling at various zoom levels. The customization options for this feature have been updated as well in `MapboxRouteLineOptions`. [#4773](https://github.com/mapbox/mapbox-navigation-android/pull/4773)
- :warning: There's now a `RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID` string ID which can be used to position other map layers on top of all route line layers produced by the `MapboxRouteLineView`. [#4773](https://github.com/mapbox/mapbox-navigation-android/pull/4773)

#### Bug fixes and improvements
- Fixed an issue where navigation notification could show partial data with wrong visuals. [#4792](https://github.com/mapbox/mapbox-navigation-android/pull/4792)
- Updated feedback events for _Free Drive_ and _Active Guidance_. [#4794](https://github.com/mapbox/mapbox-navigation-android/pull/4794)
- Fixed an occasional crash that occurred when trying to parse `BannerInstructions` by referencing `BannerView` from native implementation rather than doing it from the current step using index from nav native. [#4795](https://github.com/mapbox/mapbox-navigation-android/pull/4795)
- Fixed a crash in `MapboxSpeechFileProvider` when OS clears the cache directory while an app is running. [#4790](https://github.com/mapbox/mapbox-navigation-android/pull/4790)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0-rc.7` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.7))
- Mapbox Navigation Native `v65.0.2`
- Mapbox Core Common `v17.1.0`
- Mapbox Java `v6.0.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.5))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-beta.25 - September 03, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Added an option to display a gradient between the colors representing traffic congestion on the route line. See `MapboxRouteLineOptions#displaySoftGradientForTraffic`. [#4752](https://github.com/mapbox/mapbox-navigation-android/pull/4752)
- Added an option to select the initial leg when setting a route with `MapboxNavigation#setRoutes`. [#4739](https://github.com/mapbox/mapbox-navigation-android/pull/4739)
- Added `MapboxNavigation#requestAlternativeRoutes` that allows requesting alternative routes on-demand. [#4774](https://github.com/mapbox/mapbox-navigation-android/pull/4774)
- :warning: Added support for `ANNOTATION_CONGESTION_NUMERIC` to style the congestion on a route line. This also renamed some of the `RouteLineColorResources` methods, for example, `routeModerateColor` has been updated to `routeModerateCongestionColor`. [#4778](https://github.com/mapbox/mapbox-navigation-android/pull/4778)

#### Bug fixes and improvements
- :warning: Replaced `AnimatorListener` argument of `NavigationCamera#request[state]` functions with `TransitionEndListener`, which is now invoked even if the camera is already in or transitioning to the requested state. [#4771](https://github.com/mapbox/mapbox-navigation-android/pull/4771)
- Refactored the internal logic in `MapboxBuildingsApi` to read the location point from route's `waypoint_target`s. If not available it falls back to the points in the coordinates list. [#4767](https://github.com/mapbox/mapbox-navigation-android/pull/4767)
- :warning: `RouteOptions.Builder.applyDefaultNavigationOptions` extension now uses `ANNOTATION_CONGESTION_NUMERIC` by default, instead of `ANNOTATION_CONGESTION`.
- Fixed an issue where all E-Horizon tunnel names had prepended "1" string. [#4785](https://github.com/mapbox/mapbox-navigation-android/pull/4785)
- Fixed an issue where traffic line, or the whole route line, could sometimes disappear and required a significant camera zoom level change to show up again. [#4789](https://github.com/mapbox/mapbox-navigation-android/pull/4789)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0-rc.7` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.7))
- Mapbox Navigation Native `v65.0.1`
- Mapbox Core Common `v17.1.0`
- Mapbox Java `v6.0.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.5))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-beta.24 - August 27, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Exposed a `withForegroundService` parameter for `MapboxNavigation#startTripSession` that allows for running a session without a foreground service. [#4730](https://github.com/mapbox/mapbox-navigation-android/pull/4730)
- Added possibility to push feedback in FreeDrive mode. [#4693](https://github.com/mapbox/mapbox-navigation-android/pull/4693)

#### Bug fixes and improvements
- Added logging of updated legs for successful route refresh. [#4734](https://github.com/mapbox/mapbox-navigation-android/pull/4734)
- Fixed an issue where `RouteProgress#distanceTraveled` was reported incorrectly for multi-leg routes. [#4749](https://github.com/mapbox/mapbox-navigation-android/pull/4749)
- Fixed stuck TTS queue when deprecated onError is called. [#4746](https://github.com/mapbox/mapbox-navigation-android/pull/4746)
- Fixed an occasional crash when setting a new route while on a non-initial leg of the previous route. [#4755](https://github.com/mapbox/mapbox-navigation-android/pull/4755)
- Fixed an internal memory leak so RAM consumption should be greatly reduced. [#4759](https://github.com/mapbox/mapbox-navigation-android/pull/4759)
- Added logging of metadata for successful directions response. [#4761](https://github.com/mapbox/mapbox-navigation-android/pull/4761)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.0.0-rc.7` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.7))
- Mapbox Navigation Native `v64.0.0`
- Mapbox Core Common `v17.1.0`
- Mapbox Java `v6.0.0-alpha.4` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.4))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-beta.9.5 - Aug 24, 2021
This is a patch release on top of `v2.0.0-beta.9.x` which does not include changes introduced in `v2.0.0-beta.10` and later. The `v2.0.0-beta.23` remains the latest and most up-to-date release.

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Reduced memory footprint (both incremental and the peak). [#4740](https://github.com/mapbox/mapbox-navigation-android/pull/4740)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.19` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.19))
- Mapbox Navigation Native `v48.0.8`
- Mapbox Core Common `v11.0.2`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v3.1.1`
- Mapbox Android Telemetry `v6.2.2`

## Mapbox Navigation SDK 2.0.0-beta.23 - August 20, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Added option to set custom text for `Recenter`, `Sound`, and `RouteOverview` buttons. [#4703](https://github.com/mapbox/mapbox-navigation-android/pull/4703)
- Introduced `MapboxBuildingsApi` and `MapboxBuildingView` that would be responsible to query a building on a map given a point and render it using the view. [#4704](https://github.com/mapbox/mapbox-navigation-android/pull/4704)
- Added optional `EventsAppMetadata` field to `NavigationOptions`. [#4712](https://github.com/mapbox/mapbox-navigation-android/pull/4712)
- Exposed `NavigationSessionState` and `NavigationSessionStateObserver`. [#4712](https://github.com/mapbox/mapbox-navigation-android/pull/4712)
- Exposed lane icon drawables with `MapboxLaneIconsApi`. [#4729](https://github.com/mapbox/mapbox-navigation-android/pull/4729)
- Added `SubgraphLocation` for `RoadObject`.  [#4731](https://github.com/mapbox/mapbox-navigation-android/pull/4731)
- Added `roadSurface` property to `EHorizonEdgeMetadata`. [#4733](https://github.com/mapbox/mapbox-navigation-android/pull/4733)

#### Bug fixes and improvements
- Fixed a bug when after a route is set `RouteAlternativesController` started to request alternative routes with no observers. [#4706](https://github.com/mapbox/mapbox-navigation-android/pull/4706)
- Fixed an issue with an occasional `ArrayindexOutOfBoundsException` being thrown when setting a new route. [#4714](https://github.com/mapbox/mapbox-navigation-android/pull/4714)
- Fixed a bug when NavSDK crashed on unknown `RoadObjectLocation`. [#4731](https://github.com/mapbox/mapbox-navigation-android/pull/4731)
- Removed `MapboxBuildingHighlightApi` and `MapboxBuildingArrivalApi`. [#4704](https://github.com/mapbox/mapbox-navigation-android/pull/4704)
- Removed `Snapshotter` feature and it's related public facing apis, views and examples. [#4711](https://github.com/mapbox/mapbox-navigation-android/pull/4711)
- Removed `AppMetadata` from `MapboxNavigation.postUserFeedback`. [#4712](https://github.com/mapbox/mapbox-navigation-android/pull/4712)

## Mapbox Navigation SDK 2.0.0-beta.22 - August 12, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Added feedback event for incorrect speed limit [#4694](https://github.com/mapbox/mapbox-navigation-android/pull/4694)
- Added option to set custom background and text color for `Recenter`, `Sound`, and `RouteOverview` buttons. [#4697](https://github.com/mapbox/mapbox-navigation-android/pull/4697)
- Added `Gate` object that represents information about a particular exit or entrance. [#4692](https://github.com/mapbox/mapbox-navigation-android/pull/4692)

#### Bug fixes and improvements
- Refactored the `MapboxTripNotification` to use same set of turn icons as `ManeuverView` does. [#4683](https://github.com/mapbox/mapbox-navigation-android/pull/4683)
- :warning: Removed paint code generated files. [#4683](https://github.com/mapbox/mapbox-navigation-android/pull/4683)
- Changed the roundabout drawables for maneuvers and notifications. [#4700](https://github.com/mapbox/mapbox-navigation-android/pull/4700)
- Changed `RoadObjectDistanceInfo.distanceToStart` to nullable. [#4692](https://github.com/mapbox/mapbox-navigation-android/pull/4692)
- Updated `PolygonDistanceInfo` and `SubGraphDistanceInfo` to include `entrances` and `exits`. [#4692](https://github.com/mapbox/mapbox-navigation-android/pull/4692)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-rc.6` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.6))
- Mapbox Navigation Native `v62.0.0`
- Mapbox Core Common `v16.2.0`
- Mapbox Java `v6.0.0-alpha.2` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.2))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-beta.21 - July 28, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Added `BuildingHighlightObserver` for exposing map data used to highlight a building. Add example showing extrude all building along with a highlighted one. [#4420](https://github.com/mapbox/mapbox-navigation-android/pull/4420)
- :warning: Removed `accessToken` from `RouteOptions`, the token provided via `NaivgationOptions` is now always used for route requests. This also simplifies serialization and deserialization of route objects. Refs [`mapbox-java` `v6.0.0-alpha2`](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.2). [#4670](https://github.com/mapbox/mapbox-navigation-android/pull/4670)
- Added geometry to `Maneuver` object. [#4653](https://github.com/mapbox/mapbox-navigation-android/pull/4653)
  - Exposed `Factory` to allow developers to create `Maneuver` object.
  - Added `Point` as another argument to `Maneuver`
- Removed access token from `MapboxHistoryReader`. [#4667](https://github.com/mapbox/mapbox-navigation-android/pull/4667), [#4670](https://github.com/mapbox/mapbox-navigation-android/pull/4670)


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-rc.4` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.4))
- Mapbox Navigation Native `v59.0.0`
- Mapbox Core Common `v16.1.0`
- Mapbox Java `v6.0.0-alpha.2` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.2))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-beta.20 - July 22, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Added option to `MapboxRouteLineOptions` to visually differentiate inactive route legs for multi-leg routes. A callback was added to `MapboxRouteLineApi::updateWithRouteProgress`. The result of the callback should be rendered by the `MapboxRouteLineView` class. [#4489](https://github.com/mapbox/mapbox-navigation-android/pull/4489)

#### Bug fixes and improvements
- :warning: Removed `RouteRefreshOptions#enabled`. To enable the route refresh feature (that updates congestion and other annotations on the currently active route) you need to enable it via `RouteOptions#enableRefresh` when making a route request. [#4655](https://github.com/mapbox/mapbox-navigation-android/pull/4655)
- Refactored `MapboxSpeechApi`s to make them non-cancelling. [#4646](https://github.com/mapbox/mapbox-navigation-android/pull/4646)
- Fixed an issue where `Tunnel#length` information was missing for some of the tunnels. [#4656](https://github.com/mapbox/mapbox-navigation-android/pull/4656)
- Use `HistoryReader` for navigation replay. :warning: `mapbox-java` `v6.0.0-alpha.1` brought new features but also some breaking changes to `DirectionsRoute` and `MapboxDirections` objects that impact replay. Read more about the changes in the [release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.1). [#4601](https://github.com/mapbox/mapbox-navigation-android/pull/4601)
  - Delete `ReplayHistoryDTO` in favor of `MapboxHistoryReader`.
  - Add `eventTimestamp` to all `HistoryEvent` classes.
  - Delete `CustomEventMapper` in favor of `ReplayHistoryEventMapper`.
  - Create new `ReplayHistoryMapper` which is extendable and customizable.
- :warning: Upgraded `mapbox-java` to `v6.0.0-alpha.1` which brings new features but also some breaking changes to `DirectionsRoute` and `MapboxDirections` objects. Read more about the changes in the [release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.1). [#4526](https://github.com/mapbox/mapbox-navigation-android/pull/4526)
- Added `RouterOrigin` which indicates where a route was fetched from. [#4639](https://github.com/mapbox/mapbox-navigation-android/pull/4639)
  - Added `RouterOrigin` param to:
    - `RouterCallback#onRoutesReady` method;
    - `RouterCallback#onCanceled` method;
    - `RouterFailure` class;
    - `RerouteState.RouteFetched` class;
    - `RouteAlternativesObserver#onRouteAlternatives` method.
- Updated assets and resources translations. [#4647](https://github.com/mapbox/mapbox-navigation-android/pull/4647)
- The initialization of the route line related layers is always a synchronous call. [#4631](https://github.com/mapbox/mapbox-navigation-android/pull/4631)
- Bug fix for invalid `FeatureCollection` objects derived from routes that have less than two coordinates. [#4638](https://github.com/mapbox/mapbox-navigation-android/pull/4638)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-rc.4` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.4))
- Mapbox Navigation Native `v59.0.0`
- Mapbox Core Common `v16.1.0`
- Mapbox Java `v6.0.0-alpha.1` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.0.0-alpha.1))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-beta.19 - July 15, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Exposed `ManeuverOptions#filterDuplicateManeuvers` which allows to filter out `Maneuver`s that point to the same actual turn from the presented lists. This parameter defaults to `true`. [#4583](https://github.com/mapbox/mapbox-navigation-android/pull/4583)

#### Bug fixes and improvements
- Fixed an issue where the `MapboxNavigationViewportDataSource` could produce an unexpected update when the same route was provided more than once, for example, during a route refresh update. [#4586](https://github.com/mapbox/mapbox-navigation-android/pull/4586)
- :warning: Changed `MapboxDistanceFormatter`'s package from `com.mapbox.navigation.core.internal.formatter` to `com.mapbox.navigation.core.formatter`, making it stable. [#4608](https://github.com/mapbox/mapbox-navigation-android/pull/4608)
- :warning: Changed `MapboxManeuverApi#getManeuvers(route, callback, routeLeg)` to `MapboxManeuverApi#getManeuvers(route, callback, routeLegIndex)` to provide better support for various scenarios. This also fixed a bug where `MapboxManeuverApi` was not producing any more updates after the original route's annotations were refreshed. [#4607](https://github.com/mapbox/mapbox-navigation-android/pull/4607)
- :warning: Removed `ManevuerCallback` since it was called back synchronously anyway. `MapboxManeuverApi#getManeuvers` returns the results directly now. [#4609](https://github.com/mapbox/mapbox-navigation-android/pull/4609)
- Minor performance improvements in the drawing of the route line. Laying out traffic on top of the route is now decoupled from drawing the route itself, which decreases the time in which the core of the route line is first visible. [#4292](https://github.com/mapbox/mapbox-navigation-android/pull/4292)
- Fixed a `ConcurrentModificationException` that could occasionally be thrown when `PredictiveCacheController` was used and the connectivity changed between online/offline. [#4605](https://github.com/mapbox/mapbox-navigation-android/pull/4605)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-rc.3` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.3))
- Mapbox Navigation Native `v57.0.0`
- Mapbox Core Common `v14.2.0`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-beta.9.4 - Jul 14, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Added ability to restore from "backwards snapping" issue: set `noRouteLength` to 150m. [#4610](https://github.com/mapbox/mapbox-navigation-android/pull/4610)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.19` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.19))
- Mapbox Navigation Native `v48.0.7`
- Mapbox Core Common `v11.0.2`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v3.1.1`
- Mapbox Android Telemetry `v6.2.2`

## Mapbox Navigation SDK 2.0.0-beta.18 - July 9, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Deleted `MapboxHistoryRecorder.saveHistory` in favor of `MapboxHistoryRecorder.stopRecording`. [#4587](https://github.com/mapbox/mapbox-navigation-android/pull/4587)
- Added `MapboxHistoryRecorder.startRecording` and `MapboxHistoryRecorder.pushHistory`. [#4587](https://github.com/mapbox/mapbox-navigation-android/pull/4587)
- :warning: Removed auto enabled history recording from `HistoryRecorderOptions`. [#4587](https://github.com/mapbox/mapbox-navigation-android/pull/4587)
- :warning: Removed `RouteRequestCallback` and `Router.Callback` in favor of a single `RouterCallback` with improved failure reason accessibility. [#4577](https://github.com/mapbox/mapbox-navigation-android/pull/4577)

#### Bug fixes and improvements
- Fixed stack overflow error that could happen when `NavigationCamera` state transitions were canceled while running on some Android API levels (definitely 21-24 and maybe more). [#4575](https://github.com/mapbox/mapbox-navigation-android/pull/4575)
- Fixed an issue where navigation might crash with OutOfMemory error. [#4587](https://github.com/mapbox/mapbox-navigation-android/pull/4587)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-rc.3` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.3))
- Mapbox Navigation Native `v56.0.0`
- Mapbox Core Common `v14.2.0`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-beta.17 - July 2, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Added missing `IncidentInfo` properties: `countryCodeAlpha2`, `countryCodeAlpha3`, `lanesBlocked`, `longDescription`, `lanesClearDesc`, `numLanesBlocked`. [#4569](https://github.com/mapbox/mapbox-navigation-android/pull/4569)
- :warning: Refactored `MapboxManeuverApi` to return a list of maneuvers asynchronously without the road shields and either for a raw route reference or for route progress. See `MapboxManeuverApi#getManeuvers` and `MapboxManeuverView#renderManeuvers` for details. [#4482](https://github.com/mapbox/mapbox-navigation-android/pull/4482)
- Refactored road shield downloader to make it more efficient. [#4482](https://github.com/mapbox/mapbox-navigation-android/pull/4482)
- Added options to the `RouteArrowOptions` class for customizing the scaling expressions used for maneuver arrows. [#4551](https://github.com/mapbox/mapbox-navigation-android/pull/4551)
- Added new `HistoryEvent`: `HistoryEventPushHistoryRecord` allows read events set with **type**-**properties** structure. [#4567](https://github.com/mapbox/mapbox-navigation-android/pull/4567)
- :warning: Added new APIs to request and render road shields asynchronously, see `MapboxManeuverApi#getRoadShields` and `MapboxManeuverView#renderManeuverShields` for details. Shields are not automatically downloaded anymore to allow for more granular and faster initial updates. [#4482](https://github.com/mapbox/mapbox-navigation-android/pull/4482)
- Exposed `RouteStepProgress#instructionIndex` that indicates which instruction out of the list of instructions for a step is the currently active one. [#4482](https://github.com/mapbox/mapbox-navigation-android/pull/4482)
- :warning: Refactored `PredictiveCacheController` to handle multiple map instances, add the ability to filter sources to cache. [#4539](https://github.com/mapbox/mapbox-navigation-android/pull/4539)
  - `fun setMapInstance(map: MapboxMap)` is replaced with `fun addMapInstance(map: MapboxMap,  sourceIdsToCache: List<String>)` to allow for more granular caching configuration and support multiple map instances at the same time.
  - `fun removeMapInstance()` is replaced with `fun removeMapInstance(map: MapboxMap)`.
  - `predictiveCacheLocationOptions: PredictiveCacheLocationOptions` is moved from `NavigationOptions` to `PredictiveCacheController`.
  -  Note: the map instance has to be configured with the same `TileStore` instance that was provided to `RoutingTilesOptions.tileStore` to best support predictive caching and offline features.

#### Bug fixes and improvements
- Fixed a bug where in some cases status updates were not generated after switching between offline and online states. [#4558](https://github.com/mapbox/mapbox-navigation-android/pull/4558)
- Fixed an issue where `RouteOptions` weren't appended to onboard router results which could've caused incorrect reroutes that missed silent waypoints or other waypoint modifiers. [#4563](https://github.com/mapbox/mapbox-navigation-android/pull/4563)
- Refactored dependency of `libnavui-resources` by changing from api to implementation. [#4568](https://github.com/mapbox/mapbox-navigation-android/pull/4568)
- :warning: `RouteProgress#bannerInstructions` does not become `null` and keeps its reference for as long as a step is active. Listen to `BannerInstructionsObserver` if you need a specific timing event when the banner first appears. [#4482](https://github.com/mapbox/mapbox-navigation-android/pull/4482)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-rc.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.2))
- Mapbox Navigation Native `v55.0.0`
- Mapbox Core Common `v14.0.1`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.1.0`

## Mapbox Navigation SDK 2.0.0-beta.16 - June 24, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Navigation SDK now respects the `OfflineSwitch` and will not make resource requests if the `setMapboxStackConnected` is set to `false`, it will use cached resources and immediately fallback to local generation of routes, even if connectivity is available. [#4529](https://github.com/mapbox/mapbox-navigation-android/pull/4529)
- `RoadObjectMatcher` API changed. Added `cancelAll()` func, added `onMatchingCancelled(id: String)` callback to a `RoadObjectMatcherListener`, `cancel(roadObjectIds: List<String>)` handles a list of ids, not a single one. [#4542](https://github.com/mapbox/mapbox-navigation-android/pull/4542)
- Migrated to callback-based native getStatus approach. [#4419](https://github.com/mapbox/mapbox-navigation-android/pull/4419)
- Removed `routeGeometryWithBuffer` from `RouteProgress`. [#4419](https://github.com/mapbox/mapbox-navigation-android/pull/4419)
- :warning: Internal `setUnconditionalPollingPatience` and `setUnconditionalPollingInterval` have been moved to `InternalUtils` `object` and have to be called _before_ `MapboxNavigation` is instantiated to actually take effect. [#4419](https://github.com/mapbox/mapbox-navigation-android/pull/4419)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-rc.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.2))
- Mapbox Navigation Native `v54.0.0`
- Mapbox Core Common `v14.0.1`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.0.0`

## Mapbox Navigation SDK 2.0.0-beta.9.3 - Jun 23, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Fixed onboard router issue not supporting multiple exclude parameters. [#4534](https://github.com/mapbox/mapbox-navigation-android/pull/4534)
- When traversing routes that wrap around behind the vehicle (e.g.: cloverleafs and circular on-ramps) the zoom level is now held constant. [#4468](https://github.com/mapbox/mapbox-navigation-android/pull/4468)
- Restored an option to display restricted sections of a route on the route line.  The option is included in the `MapboxRouteLineOptions` as `displayRestrictedRoadSections()`. The option is false by default indicating the restricted section will not be visible on the route line. [#4501](https://github.com/mapbox/mapbox-navigation-android/pull/4501)
- Navigation SDK now respects the `NetworkConnectivity` settings and will not make resource request if the `setMapboxStackConnected` is set to `false`, it will use cached resources and immediately fallback to local generation of routes, even if connectivity is available. [#4529](https://github.com/mapbox/mapbox-navigation-android/pull/4529)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.19` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.19))
- Mapbox Navigation Native `v48.0.6`
- Mapbox Core Common `v11.0.2`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v3.1.1`
- Mapbox Android Telemetry `v6.2.2`

## Mapbox Navigation SDK 2.0.0-beta.15 - June 18, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
* Fixed a bug in `RouteOptionsUpdater` where `snappingClosures` were not taken into account when making a reroute request. [4503](https://github.com/mapbox/mapbox-navigation-android/pull/4503)
* Fixed missing `Gantry` objects in `ElectronicHorizon`. [#4518](https://github.com/mapbox/mapbox-navigation-android/pull/4518)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-rc.1))
- Mapbox Navigation Native `v53.0.1`
- Mapbox Core Common `v14.0.1`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v5.0.0`
- Mapbox Android Telemetry `v8.0.0`

## Mapbox Navigation SDK 2.0.0-beta.14 - June 11, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
* :warning: Removed `ArrivalOptions` in favor of triggering when the `RouteProgress.currentState` is equal to `RouteProgressState.COMPLETE`. You can use `RouteProgress` to implement custom arrival behavior based on time or distance. [4459](https://github.com/mapbox/mapbox-navigation-android/pull/4459)
* :warning: Removed `MapboxNavigation#toggleHistory` in favor of `HistoryRecorderOptions` (found in the `NavigationOptions`), `MapboxHistoryRecorder`, `MapboxHistoryReader` for saving and reading native history files. [4488](https://github.com/mapbox/mapbox-navigation-android/pull/4488)
* When traversing routes that wrap around behind the vehicle (e.g.: cloverleafs and circular on-ramps) the zoom level is now held constant by the `MapboxNavigationViewportDataSource`. [4468](https://github.com/mapbox/mapbox-navigation-android/pull/4468)

#### Known issues
When using this release, the merged Manifest comes with an unnecessary `WRITE_SETTINGS` permission declaration. You can ignore that permission and not request it or add this to your Manifest file as a workaround:
```
<uses-permission android:name="android.permission.WRITE_SETTINGS" tools:node="remove"/>
```
This permission declaration will be removed in future releases.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.20` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.20))
- Mapbox Navigation Native `v51.0.0`
- Mapbox Core Common `v12.0.0`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v4.0.2`
- Mapbox Android Telemetry `v7.0.3`

## Mapbox Navigation SDK 2.0.0-beta.13 - June 4, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
* Removed `getRouteGeometryWithBuffer` API from `RouteProgress`. [#4478](https://github.com/mapbox/mapbox-navigation-android/pull/4478)
* Removed the `updatePrimaryRoute` method from the `MapboxRouteLineApi` class. Instead, `MapboxRouteLineApi::setRoutes` should be used. The first `DirectionsRoute` in the collection will be considered the primary route. [#4467](https://github.com/mapbox/mapbox-navigation-android/pull/4467)
* Added NavigationVersionSwitchObserver which enables listening to navigation tiles version switch. [#4451](https://github.com/mapbox/mapbox-navigation-android/pull/4451)
* Added missing `@JvmOverloads` to `MapboxNavigation#postUserFeedback` API. [#4472](https://github.com/mapbox/mapbox-navigation-android/pull/4472)
* Fixed an issue where routing tiles were not downloading correctly if the `TileStore` instance was not provided and `mapbox_access_token` string resource was not present. This was resulting in a lack of free drive events and location enhancement failures. [#4464](https://github.com/mapbox/mapbox-navigation-android/pull/4464)
* Removed `com.mapbox.navigation.ui.base.model.Expected` in favor of `com.mapobox.bindgen.Expected`. [#4463](https://github.com/mapbox/mapbox-navigation-android/pull/4463)

#### Known issues
:warning: `MapboxNavigation` history recording APIs `retrieveHistory`, `toggleHistory` and `addHistoryEvent` are no-op in this release. [#4478](https://github.com/mapbox/mapbox-navigation-android/pull/4478)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.20` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.20))
- Mapbox Navigation Native `v51.0.0`
- Mapbox Core Common `v12.0.0`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v4.0.2`
- Mapbox Android Telemetry `v7.0.3`

## Mapbox Navigation SDK 2.0.0-beta.9.2 - May 28, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
* When setting routes with the `MapboxRouteLineApi`, if the primary route hasn't changed the traffic will be recalculated while the state of the vanishing route line will remain unchanged. [#4444](https://github.com/mapbox/mapbox-navigation-android/pull/4444)
* Fixed an issue where the route refresh request wasn't respecting `RouteOptions#baseUrl`. [#4427](https://github.com/mapbox/mapbox-navigation-android/pull/4427)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.19` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.19))
- Mapbox Navigation Native `v48.0.5`
- Mapbox Core Common `v11.0.2`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v3.1.1`
- Mapbox Android Telemetry `v6.2.2`

## Mapbox Navigation SDK 2.0.0-beta.12 - May 27, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
* When setting routes with the `MapboxRouteLineApi`, if the primary route hasn't changed the traffic will be recalculated while the state of the vanishing route line will remain unchanged. [#4444](https://github.com/mapbox/mapbox-navigation-android/pull/4444)
* Fixed an issue where the route refresh request wasn't respecting `RouteOptions#baseUrl`. [#4427](https://github.com/mapbox/mapbox-navigation-android/pull/4427)
* Fixed logic to show roundabout for left-side driving. The icons are now correctly flipped. [#4445](https://github.com/mapbox/mapbox-navigation-android/pull/4445)

#### Known issues
When using this release, the merged Manifest comes with an unnecessary `WRITE_SETTINGS` permission declaration. You can ignore that permission and not request it or add this to your Manifest file as a workaround:
```
<uses-permission android:name="android.permission.WRITE_SETTINGS" tools:node="remove"/>
```
This permission declaration will be removed in future releases.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.20` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.20))
- Mapbox Navigation Native `v50.0.0`
- Mapbox Core Common `v12.0.0`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v4.0.2`
- Mapbox Android Telemetry `v7.0.3`

## Mapbox Navigation SDK 2.0.0-beta.11 - May 21, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
* Added `TileStore` to the `RoutingTilesOptions` [#4408](https://github.com/mapbox/mapbox-navigation-android/pull/4408)
* Added ability to periodically observe route alternatives. [#4375](https://github.com/mapbox/mapbox-navigation-android/pull/4375)
* Deleted `FasterRouteObserver` in favor of `RouteAlternativesObserver` and `RouteAlternativesOptions`. [#4375](https://github.com/mapbox/mapbox-navigation-android/pull/4375)
* Exposed `com.mapbox.bindgen.Expected#fold`, `com.mapbox.bindgen.Expected#map`, and other mapping functions. [#4417](https://github.com/mapbox/mapbox-navigation-android/pull/4417)
* Removed `Expected<E, V> ExpectedFactory#createValue()` and replaced it with `Expected<E, None> ExpectedFactory#createNone()` which means that value can never be `null` and improves consumption from Kotlin. [#4417](https://github.com/mapbox/mapbox-navigation-android/pull/4417)
* Removed `RouteProgressState.ROUTE_INVALID`. `RouteProgress` will never be delivered with an invalid update, besides `RouteProgressState.OFF_ROUTE`. [#4391](https://github.com/mapbox/mapbox-navigation-android/pull/4391)

#### Bug fixes and improvements
* Hardened arrival events to trigger only when `RouteProgressState` is `ROUTE_COMPLETE` or `LOCATION_TRACKING` to avoid cases where we are uncertain/stale/rerouting but still reporting arrival. [#4392](https://github.com/mapbox/mapbox-navigation-android/pull/4392)
* Converted SAM interfaces into Functional (SAM) interfaces [#4406](https://github.com/mapbox/mapbox-navigation-android/pull/4406)
* Swapped the `com.mapbox.bindgen.Expected` generic params from `<Value, Error>` to `<Error, Value>` to match platform conventions. [#4417](https://github.com/mapbox/mapbox-navigation-android/pull/4417)
* Fixed either show sub banner or lane guidance. [#4413](https://github.com/mapbox/mapbox-navigation-android/pull/4413)
* Fixed the race condition in `RouteRefreshController` where `setRoute` will not cancel old refresh requests. [#4421](https://github.com/mapbox/mapbox-navigation-android/pull/4421)
* Fixed rotation angle to 0 on `MapboxManeuverTurnIcon` for regular turns. [#4423](https://github.com/mapbox/mapbox-navigation-android/pull/4423)
* Changed terminology used for route arrow border for correctness and consistency. [#4428](https://github.com/mapbox/mapbox-navigation-android/pull/4428)
* Removed the `ROUTE_` prefix from `RouteProgressState` values and updated the documentation. [#4391](https://github.com/mapbox/mapbox-navigation-android/pull/4391)
* Moved the `RouteProgressState.LOCATION_STALE` to `RouteProgress.stale`. [#4391](https://github.com/mapbox/mapbox-navigation-android/pull/4391)

#### Known issues
When using this release, the merged Manifest comes with an unnecessary `WRITE_SETTINGS` permission declaration. You can ignore that permission and not request it or add this to your Manifest file as a workaround:
```
<uses-permission android:name="android.permission.WRITE_SETTINGS" tools:node="remove"/>
```
This permission declaration will be removed in future releases.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.20` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.20))
- Mapbox Navigation Native `v50.0.0`
- Mapbox Core Common `v12.0.0`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v4.0.2`
- Mapbox Android Telemetry `v7.0.2`

## Mapbox Navigation SDK 2.0.0-beta.10 - May 12, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
* Added `RoadObjectMatcher`. ([#4364](https://github.com/mapbox/mapbox-navigation-android/pull/4364))
* Added ability to change the text appearance for primary, secondary and total step distance of upcoming maneuver instructions. ([#4378](https://github.com/mapbox/mapbox-navigation-android/pull/4378))
* Added params to `VoiceInstructionsPlayerOptions`. ([#4373](https://github.com/mapbox/mapbox-navigation-android/pull/4373))
* Added a parser that parses svg to bitmap for signboards. ([#4335](https://github.com/mapbox/mapbox-navigation-android/pull/4335))
* Introduced `MapboxExternalFileResolver` that helps you to resolve fonts for signboards. You can now inject your own resolver as well if you don't wish to use the default. ([#4335](https://github.com/mapbox/mapbox-navigation-android/pull/4335))

#### Bug fixes and improvements
- Refactored `TilesetDescriptorFactory`. ([#4364](https://github.com/mapbox/mapbox-navigation-android/pull/4364))
- Updated `EHorizon` API and refactored `RoadObject`:
  - Moved all `EHorizon` and `RoadObject` **`data classes`** to `base` module. ([#4364](https://github.com/mapbox/mapbox-navigation-android/pull/4364))
  - Made all the constructors `internal`. ([#4364](https://github.com/mapbox/mapbox-navigation-android/pull/4364))
  - Exposed `RoadObjectInstaceFactory` and `EHorizonInstanceFactory` that lives under an `internal` package in the `base` module, which is only there for the `core` module to access it. ([#4364](https://github.com/mapbox/mapbox-navigation-android/pull/4364))
* :warning: Requesting a route via `MapboxNavigation#requestRoutes` does not automatically append any defaults. Defaults are now available under these extensions:
  - `RouteOptions.Builder.applyDefaultNavigationOptions()` that applies the options that are required for the route request to execute or otherwise recommended for the Navigation SDK and all of its features to provide the best car navigation experience.
  - `RouteOptions.Builder.applyLanguageAndVoiceUnitOptions(context: Context)` that applies the options that adapt the returned instructions' language and voice unit based on the device's `Locale`.
    The extensions are very much recommended to be called when building the `RouteOptions` object and then tweaked for a specific use case. ([#4320](https://github.com/mapbox/mapbox-navigation-android/pull/4320))
* Changed `VoiceInstructionsPlayerAttributes` to `sealed class`. ([#4373](https://github.com/mapbox/mapbox-navigation-android/pull/4373))
* Bug fix for calculating mutli-leg routes with traffic congestion changes or restrictions at the first point of a route leg. ([#4383](https://github.com/mapbox/mapbox-navigation-android/pull/4383))
* `MapboxSignboardAPI` now returns `Bitmap` instead of `ByteArray`. ([#4335](https://github.com/mapbox/mapbox-navigation-android/pull/4335))
* `MapboxJunctionAPI` now returns `Bitmap` instead of `ByteArray`. ([#4335](https://github.com/mapbox/mapbox-navigation-android/pull/4335))

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.19` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.19))
- Mapbox Navigation Native `v49.0.1`
- Mapbox Core Common `v11.0.2`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v3.1.1`
- Mapbox Android Telemetry `v6.2.2`

## Mapbox Navigation SDK 2.0.0-beta.9.1 - May 12, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
* Bug fix for calculating mutli-leg routes with traffic congestion changes or restrictions at the first point of a route leg. [#4383](https://github.com/mapbox/mapbox-navigation-android/pull/4383)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.19` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.19))
- Mapbox Navigation Native `v48.0.5`
- Mapbox Core Common `v11.0.2`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v3.1.1`
- Mapbox Android Telemetry `v6.2.2`

## Mapbox Navigation SDK 2.0.0-beta.9 - May 7, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
- Added uncertain location puck drawable. [#4358](https://github.com/mapbox/mapbox-navigation-android/pull/4358)
- Made `PlayerAttributes` an abstract class. [#4365](https://github.com/mapbox/mapbox-navigation-android/pull/4365)

#### Bug fixes and improvements
- Changed restricted roads to be represented as part of the traffic line and will vanish along with the rest of the line when the vanishing route line feature is enabled. [#4360](https://github.com/mapbox/mapbox-navigation-android/pull/4360)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.19` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.19))
- Mapbox Navigation Native `v48.0.5`
- Mapbox Core Common `v11.0.2`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v3.1.1`
- Mapbox Android Telemetry `v6.2.2`

## Mapbox Navigation SDK 2.0.0-beta.8 - April 30, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
* Refactored `AudioFocusDelegate` to a public interface and is now optional to `MapboxVoiceInstructionsPlayer`. `MediaPlayer` stream type can now be customized and results of audio focus can now be handled. [#4278](https://github.com/mapbox/mapbox-navigation-android/pull/4278)
* Made route refresh interval configurable. [#4321](https://github.com/mapbox/mapbox-navigation-android/pull/4321)
* Exposed `NavigationCameraTransitionOptions` via `NavigationCamera` state requests. Those options provide high-level constraints for all of the transitions that `NavigationCamera` executes and can be used to, for example, adjust the duration of the transitions. [#4332](https://github.com/mapbox/mapbox-navigation-android/pull/4332)
#### Bug fixes and improvements
* Removed `@JvmOverloads` from all Mapbox Views. [#4323](https://github.com/mapbox/mapbox-navigation-android/pull/4323)
* Changed where the `MapboxNavigationViewportDataSourceDebugger`'s debug layer is positioned on the map. By default, the layer will be placed on top of the stack unless a reference ID is provided. This prevents a crash that was occurring if the previously hardcoded layer ID wasn't present in the style. [#4334](https://github.com/mapbox/mapbox-navigation-android/pull/4334)
* Fixed replay's simulated driver issue where it will nearly stop on the freeway, by normalizing bearing for speed profile calculations. [#4338](https://github.com/mapbox/mapbox-navigation-android/pull/4338)
* Fixed an issue where the user location was incorrectly positioned in the center of the screen in the following state when the pitch was zero, `maximizeViewableRouteGeometryWhenPitchZero` was set, and no other points were available for framing. Now the user location is correctly tied to the bottom edge of the padding if that's the only geometry to frame. The flag was also renamed to `maximizeViewableGeometryWhenPitchZero`. [#4343](https://github.com/mapbox/mapbox-navigation-android/pull/4343)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.18` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.18))
- Mapbox Navigation Native `v48.0.5`
- Mapbox Core Common `v11.0.2`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v3.1.1`
- Mapbox Android Telemetry `v6.2.2`

## Mapbox Navigation SDK 2.0.0-beta.7 - April 23, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
* Expose TilesetDescriptorFactory [#4283](https://github.com/mapbox/mapbox-navigation-android/pull/4283)
* Add building highlight on arrival [#4078](https://github.com/mapbox/mapbox-navigation-android/pull/4078)
* Add EHorizon isOneWay property [#4295](https://github.com/mapbox/mapbox-navigation-android/pull/4295)
#### Bug fixes and improvements
* Bug fix for route line related concurrent modification of collection. [#4298](https://github.com/mapbox/mapbox-navigation-android/pull/4298)
* Made route reference checks more robust in the `MapboxNavigationViewportDataSource` and improved logs to avoid situations where a route reference is mismatched and could produce incorrect frames or crash. [#4302](https://github.com/mapbox/mapbox-navigation-android/pull/4302)
* Map `ResourceOptions` contains tile store instance (TileStore API). Tile store usage is enabled by default, `ResourceOptions.tileStoreEnabled` flag is introduced to disable it. This changed the integration with the `PredictiveCacheControler` which contains update integration docs. [#4310](https://github.com/mapbox/mapbox-navigation-android/pull/4310)
* Removed Timber across SDK [#4300](https://github.com/mapbox/mapbox-navigation-android/pull/4300)
* Fixed an issue with the `NavigationCamera` occasionally spinning around when a transition to a state (typically `overview`) finished. [#4293](https://github.com/mapbox/mapbox-navigation-android/pull/4293)
* Fixed an issue with the `NavigationCamera` transitions occasionally finishing at an incorrect visual target. This could occur when a gesture interaction with the map preceded the transition. See [mapbox-maps-android/issues/277](https://github.com/mapbox/mapbox-maps-android/issues/277) for details and workarounds. [#4296](https://github.com/mapbox/mapbox-navigation-android/pull/4296)
* Introduced `MapboxNavigationViewportDataSourceOptions.overviewFrameOptions.geometrySimplification` which by default simplifies the route geometry used for overview framing by a factor of 25 to improve performance of frame generation, especially for longer routes. [#4286](https://github.com/mapbox/mapbox-navigation-android/pull/4286)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

- Mapbox Maps SDK `v10.0.0-beta.18` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/android-v10.0.0-beta.18))
- Mapbox Navigation Native `v48.0.4`
- Mapbox Core Common `v11.0.2`
- Mapbox Java `5.9.0-alpha.5` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v5.9.0-alpha.5))
- Mapbox Android Core `v3.1.1`
- Mapbox Android Telemetry `v6.2.2`

## Mapbox Navigation SDK 2.0.0-beta.6 - April 16, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
* Added a callback to the `ArrivalObserver` specifically for waypoint experiences. [#4238](https://github.com/mapbox/mapbox-navigation-android/pull/4238)

#### Bug fixes and improvements
* Refactored the route line API to improve performance. [#4271](https://github.com/mapbox/mapbox-navigation-android/pull/4271)
* Significantly changed the way how the `MapboxNavigationViewportDataSource` generates the camera frames that are later animated by the `NavigationCamera`. You can read more about details in the `MapboxNavigationViewportDataSource` class documentation. [#4072](https://github.com/mapbox/mapbox-navigation-android/pull/4072)
  Highlights:
  - When in the `FOLLOWING` state, the first point of the framed geometry will be placed at the bottom edge of the provided padding, centered horizontally. This typically refers to the user's location provided to the viewport data source, if available.
  - **If you use the `MapboxNavigationViewportDataSource`, you should explicitly define `CameraOptions.padding` in all other camera transitions that your app is executing.**
  - New features:
    - calculating zoom level based on intersection density
    - automatic pitching to 0 near maneuvers
    - maximizing the view of the maneuver's geometry in pitch 0
    - bearing smoothing based on the direction to the upcoming maneuver
    - `MapboxNavigationViewportDataSourceDebugger` for visualizing frames and geometries
* Fixed a bug for restricted road sections not getting cleared when calling API. [#4254](https://github.com/mapbox/mapbox-navigation-android/pull/4254)
* Set `TilesConfig` `inMemoryTileCache` size to 1GB (1024 x 1024 x 1024). [#4272](https://github.com/mapbox/mapbox-navigation-android/pull/4272)
* Fixed crash when routing new waypoints. [#4263](https://github.com/mapbox/mapbox-navigation-android/pull/4263)
* Added check if `RouteOptions` has valid UUID to refresh route. [#4260](https://github.com/mapbox/mapbox-navigation-android/pull/4260)
* Updated `ArrivalObserver` making the `onWaypointArrival` and `onFinalDestinationArrival` callbacks consistent, and can be specified through the `ArrivalController` as `ArrivalOptions`. [#4250](https://github.com/mapbox/mapbox-navigation-android/pull/4250)

## Mapbox Navigation SDK 1.6.1 - April 14, 2021
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes and improvements
- :warning: Changed `DynamicCamera`'s default tilt and zoom constraints to improve performance and readability of the map when in active navigation. [#4262](https://github.com/mapbox/mapbox-navigation-android/pull/4262)

## Mapbox Navigation SDK 1.6.0 - April 14, 2021
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Features
- Added additional route overview method to NavigationMapboxMap to display section of route not yet traveled rather than the full route. [#4247](https://github.com/mapbox/mapbox-navigation-android/pull/4247)

#### Bug fixes and improvements
- Show maneuver icon and text in the notification tray after changing b/w day and night mode. [#4256](https://github.com/mapbox/mapbox-navigation-android/pull/4256)
- Bug fix for restoring route line on map when bringing application into foreground just after a reroute. [#4236](https://github.com/mapbox/mapbox-navigation-android/pull/4236)

## Mapbox Navigation SDK 2.0.0-beta.5 - April 9, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
* :warning: Changed `MapboxNavigation#requestRoutes` to **not** automatically set the result as the primary route for the navigation experience. When calling `MapboxNavigation#requestRoutes` make sure to also call `MapboxNavigation#setRoutes` with a result. This change allows for dispatching and managing multiple route requests concurrently, including cancelling with `MapboxNavigation#cancelRouteRequest`. [#4184](https://github.com/mapbox/mapbox-navigation-android/pull/4184)
* Exposed `MapboxSoundButton`. [#4117](https://github.com/mapbox/mapbox-navigation-android/pull/4117)
* Exposed `MapboxRecenterButton`. [#4226](https://github.com/mapbox/mapbox-navigation-android/pull/4226)
* Added support for signboard styling based on the stylesheet. See `MapboxSignboardOptions`. [#4217](https://github.com/mapbox/mapbox-navigation-android/pull/4217)
* Added length, startGeometryIndex, endGeometryIndex to `entrance`/`exit` road objects when the source is the Directions API. [#4235](https://github.com/mapbox/mapbox-navigation-android/pull/4235)
* :warning: Expanded `RouteArrowApi` functionality to include ability to add (and remove) multiple arrows to a map which also changes the method signatures. [#3980](https://github.com/mapbox/mapbox-navigation-android/pull/3980)
* Added support for the composite sources in the `PredictiveCacheController`. [#4241](https://github.com/mapbox/mapbox-navigation-android/pull/4241)

#### Bug fixes and improvements
* Significantly improved the rendering time of the route line and the time to select an alternative. [#4244](https://github.com/mapbox/mapbox-navigation-android/pull/4244) [#4222](https://github.com/mapbox/mapbox-navigation-android/pull/4222) [#4209](https://github.com/mapbox/mapbox-navigation-android/pull/4209)
* :warning: Made `UnitType` in `MapboxDistanceFormatter` type-safe. [#4224](https://github.com/mapbox/mapbox-navigation-android/pull/4224)

## Mapbox Navigation SDK 2.0.0-beta.4 - April 1, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
* Added option to display road closures on the route traffic line. [#4215](https://github.com/mapbox/mapbox-navigation-android/pull/4215)
* Added option in `RouteLineResources` to enable the display of restricted road sections of the route line. The restriction related color option is in `RouteLineColorResources`. [#4148](https://github.com/mapbox/mapbox-navigation-android/pull/4148)
* Added multiple exclusion criteria and notification of exclusion violations. [#4195](https://github.com/mapbox/mapbox-navigation-android/pull/4195)

#### Bug fixes and improvements
* Improved documentation for the suggested `MapboxRouteLineOptions.Builder#withRouteLineBelowLayerId` value. [#4202](https://github.com/mapbox/mapbox-navigation-android/pull/4202)
* Fixed bearings calculation on reroute. [#4169](https://github.com/mapbox/mapbox-navigation-android/pull/4169)
* Added `suspend` and callback versions of some methods in the `MapboxRouteLineApi` class. [#4106](https://github.com/mapbox/mapbox-navigation-android/pull/4106)
* Split `TunnelEntrance` and `RestrictedArea` alerts, made `RoadObjects` info values non-null. [#4162](https://github.com/mapbox/mapbox-navigation-android/pull/4162)

#### Known issues
**When integrating this release, you might run into a crash on startup caused by cached data incompatibility. To resolve the issue, clear the application cache.**

## Mapbox Navigation SDK 1.5.1 - March 31, 2021
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes and improvements
- Fixed speed limit. [#4143](https://github.com/mapbox/mapbox-navigation-android/pull/4143)
- Added default text placeholder to `TripNotification` view. [#4156](https://github.com/mapbox/mapbox-navigation-android/pull/4156)
- Fixed `TripNotification` view to always show `Stop Session` label. [#4173](https://github.com/mapbox/mapbox-navigation-android/pull/4173)
- Fixed bearings calculation on reroute. [#4199](https://github.com/mapbox/mapbox-navigation-android/pull/4199)

## Mapbox Navigation SDK 2.0.0-beta.3 - March 26, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Features
* Implemented Guidance Views. See `MapboxJunctionApi` and `MapboxJunctionView` for details. [#4157](https://github.com/mapbox/mapbox-navigation-android/pull/4157)
* Added `SnappingClosures` param to route request builder. [#4161](https://github.com/mapbox/mapbox-navigation-android/pull/4161)

#### Bug fixes and improvements
* Added text placeholder for TripNotification View. [#4160](https://github.com/mapbox/mapbox-navigation-android/pull/4160)
* Fixed bug with missing annotations in reroute/faster route. [#4171](https://github.com/mapbox/mapbox-navigation-android/pull/4171)
* Changed the timing of the Navigation Camera's transition to the `Following` state to improve the zoom-in relation to the centering animation. [#4175](https://github.com/mapbox/mapbox-navigation-android/pull/4175)

#### Other changes
* Added an example to show custom styling. [#4178](https://github.com/mapbox/mapbox-navigation-android/pull/4178)

#### Known issues
**When integrating this release, you might run into a crash on startup caused by cached data incompatibility. To resolve the issue, clear the application cache.**

## Mapbox Navigation SDK 2.0.0-beta.2 - March 19, 2021

For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see [2.0 Navigation SDK Migration Guide](https://github.com/mapbox/mapbox-navigation-android/wiki/2.0-Navigation-SDK-Migration-Guide).

### Changelog
#### Bug fixes and improvements
- Fix the issue that origin icon and destination icons are same. [#4115](https://github.com/mapbox/mapbox-navigation-android/pull/4115)
- Fixes maneuver list cut in landscape mode. [#4124](https://github.com/mapbox/mapbox-navigation-android/pull/4124)
- Refactor signboard and alter api. [#4125](https://github.com/mapbox/mapbox-navigation-android/pull/4125)
- Fixes using the incorrect color the traveled route line casing. [#4128](https://github.com/mapbox/mapbox-navigation-android/pull/4128)
- Added scaling options for the alternative route lines in the RouteLineResources class. [#4129](https://github.com/mapbox/mapbox-navigation-android/pull/4129)
- Refactor snapshotter. [#4132](https://github.com/mapbox/mapbox-navigation-android/pull/4132)
- Exposed option to override alternative route color based on route identifier. [#4137](https://github.com/mapbox/mapbox-navigation-android/pull/4137)
- Refactored Voice module to use `MapboxNavigationConsumer` generic callback. [#4138](https://github.com/mapbox/mapbox-navigation-android/pull/4138)
- Refactored moved nav-native -> SDK type mapping and `roadobject` models to core module. [#4141](https://github.com/mapbox/mapbox-navigation-android/pull/4141)
- Introduce route overview button. [#4142](https://github.com/mapbox/mapbox-navigation-android/pull/4142)
- Fix logical error in speed limit view. [#4143](https://github.com/mapbox/mapbox-navigation-android/pull/4143)
- Fixed banner instruction and route progress mismatch when in multi-leg scenarios. [#4154](https://github.com/mapbox/mapbox-navigation-android/pull/4154)

## Mapbox Navigation SDK 1.5.0 - March 13, 2021
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes and improvements
- Improved logic to select turn lanes. [#4006](https://github.com/mapbox/mapbox-navigation-android/pull/4006)
- Changed `SpeechPlayerProvider`: constructor deprecated, builder added, `focusGain` param added. [#4052](https://github.com/mapbox/mapbox-navigation-android/pull/4052)
- Fixed Route refresh: refresh alternative route when selected. [#4056](https://github.com/mapbox/mapbox-navigation-android/pull/4056)

## Mapbox Navigation SDK 1.5.0-alpha.2 - February 19, 2021
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes and improvements
- Fixed `RouteProgress#route` which didn't have its congestion annotations updated after a route refresh. [#4031](https://github.com/mapbox/mapbox-navigation-android/pull/4031)
- Fixed `RouteProgerss#route` which always returned a primary route even if an alternative was selected. [#4031](https://github.com/mapbox/mapbox-navigation-android/pull/4031)

## Mapbox Navigation SDK 1.5.0-alpha.1 - February 12, 2021
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Features
* Added auto-choosing the latest tiles version. [3931](https://github.com/mapbox/mapbox-navigation-android/pull/3931)
* Added Nav-Native SDK version to `TileEndpointConfiguration`. [3918](https://github.com/mapbox/mapbox-navigation-android/pull/3918)

#### Bug fixes and improvements
* Fixed the Route Refresh feature which was failing on each request due to parser incompatibilities. [3986](https://github.com/mapbox/mapbox-navigation-android/pull/3986)
* Changed the default routing tiles version to `2021_01_24-03_00_00`. [3933](https://github.com/mapbox/mapbox-navigation-android/pull/3933)
* Updated default routing tiles version to `2021_01_30-03_00_00`. [3929](https://github.com/mapbox/mapbox-navigation-android/pull/3929)
* Added check for arrow head icon height and width to prevent reported crash. [3922](https://github.com/mapbox/mapbox-navigation-android/pull/3922)
* Fixed stop trip session issue. [3919](https://github.com/mapbox/mapbox-navigation-android/pull/3919)

## Mapbox Navigation SDK 1.4.0 - January 20, 2021
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes and improvements
* Removed the `ACCESS_BACKGROUND_LOCATION` permission requirement. [3913](https://github.com/mapbox/mapbox-navigation-android/pull/3913)

## Mapbox Navigation SDK 1.4.0-rc.1 - January 13, 2021
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes and improvements
* Fixed MapboxRouteOptionsUpdater: IndexOutOfBoundsException with 'bearings' combining. [#3899](https://github.com/mapbox/mapbox-navigation-android/pull/3899)
* Fixed FeedbackFlow to skip Detailed Screen when detailed items aren't presented. [#3882](https://github.com/mapbox/mapbox-navigation-android/pull/3882)
* Removed misleading min size annotation for addRoutes method and updated the documentation. [#3877](https://github.com/mapbox/mapbox-navigation-android/pull/3877)

## Mapbox Navigation SDK 1.3.0 - December 17, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Features
* Added `onFeedbackFlowFinished` method to `FeedbackFlowListener`. [3850](https://github.com/mapbox/mapbox-navigation-android/pull/3850)
* Exposed an option for the user to provide an arrival experience feedback when the navigation session is finished. See `NavigationFeedbackOptions` for `NavigationView` integration and `FeedbackArrivalFragment` for standalone. [3758](https://github.com/mapbox/mapbox-navigation-android/pull/3758)
* Exposed APIs to support caching user feedback events. [3724](https://github.com/mapbox/mapbox-navigation-android/pull/3724)

#### Bug fixes and improvements
* Fixed Exception `Resource not found` when using `FeedbackDetailsFragment`. [3842](https://github.com/mapbox/mapbox-navigation-android/pull/3842)
* Updated the detailed feedback flow to request additional information in a form after arrival instead of immediately when feedback is reported. See `NavigationFeedbackOptions` for `NavigationView` integration and `FeedbackDetailsFragment` for standalone. [3758](https://github.com/mapbox/mapbox-navigation-android/pull/3758)

## Mapbox Navigation SDK 1.4.0-beta.1 - December 17, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes and improvements
* Fixed maneuver view so that shows a generic roundabout icon if there's no angle data. [#3867](https://github.com/mapbox/mapbox-navigation-android/pull/3867)
* Fixed rerouting and requesting faster routes when silent waypoints are present. The waypoints are now persisted and not dropped. [#3581](https://github.com/mapbox/mapbox-navigation-android/pull/3581)

## Mapbox Navigation SDK 1.3.0-alpha.5 - December 9, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Features
* Exposed GeoJsonOptions#withTolerance param for route line sources. [3819](https://github.com/mapbox/mapbox-navigation-android/pull/3819)
* Added speedLimit to MapMatcher. [3839](https://github.com/mapbox/mapbox-navigation-android/pull/3839)
* Added `onFeedbackFlowFinished` method to `FeedbackFlowListener`. [3850](https://github.com/mapbox/mapbox-navigation-android/pull/3850)

#### Bug fixes and improvements
* Updated assets and resources translations. [3856](https://github.com/mapbox/mapbox-navigation-android/pull/3856)
* Updated default routing tiles version to `2020_12_05-03_00_00`. [3857](https://github.com/mapbox/mapbox-navigation-android/pull/3857)

## Mapbox Navigation SDK 1.3.0-alpha.4 - December 2, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Features
* Added an option in NavigationOptions for enable/disable route refresh. [3709](https://github.com/mapbox/mapbox-navigation-android/pull/3709)

#### Bug fixes and improvements
* Fixed Exception `Resource not found` when using `FeedbackDetailsFragment`. [3842](https://github.com/mapbox/mapbox-navigation-android/pull/3842)

## Mapbox Navigation SDK 1.3.0-alpha.3 - November 27, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Features
* Updated the detailed feedback flow to request additional information in a form after arrival instead of immediately when feedback is reported. See `NavigationFeedbackOptions` for `NavigationView` integration and `FeedbackDetailsFragment` for standalone. [#3758](https://github.com/mapbox/mapbox-navigation-android/pull/3758)
* Exposed an option for the user to provide an arrival experience feedback when the navigation session is finished. See `NavigationFeedbackOptions` for `NavigationView` integration and `FeedbackArrivalFragment` for standalone. [#3758](https://github.com/mapbox/mapbox-navigation-android/pull/3758)
* Exposed APIs to support caching user feedback events. [#3724](https://github.com/mapbox/mapbox-navigation-android/pull/3724)
* Exposed current intersection index value in the `RouteProgress`. [#3810](https://github.com/mapbox/mapbox-navigation-android/pull/3810)

#### Bug fixes and improvements
* Updated default routing tiles version to `2020_11_21-03_00_00`. [#3812](https://github.com/mapbox/mapbox-navigation-android/pull/3812)
* Increased map-matching to route geometry precision threshold need to keep updating vanishing point to 3m. [#3803](https://github.com/mapbox/mapbox-navigation-android/pull/3803)

## Mapbox Navigation SDK 1.2.1 - November 26, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Features
* Added options that allow for custom raw location polling rates via `NavigationOptions`. [#3800](https://github.com/mapbox/mapbox-navigation-android/pull/3800) [#3804](https://github.com/mapbox/mapbox-navigation-android/pull/3804)

#### Bug fixes and improvements
* Route line calculations moved off of the main thread in order to improve performance and resolve ANR issue for long routes. [#3789](https://github.com/mapbox/mapbox-navigation-android/pull/3789)
* Added `equals` and `hashCode` support for `LocationEngineRequest` in Mapbox Events library so it's possible to compare `NavigationOptions` classes. [#3820](https://github.com/mapbox/mapbox-navigation-android/pull/3820)

## Mapbox Navigation SDK 1.3.0-alpha.2 - November 18, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Features
* Added options that allow for custom polling rates. [#3800](https://github.com/mapbox/mapbox-navigation-android/pull/3800)

#### Bug fixes and improvements
* A more performant and accurate method of finding an alternative route when the map is clicked. [#3723](https://github.com/mapbox/mapbox-navigation-android/pull/3723)
* Route line calculations moved off of the main thread in order to improve performance and resolve ANR issue for long routes. [#3789](https://github.com/mapbox/mapbox-navigation-android/pull/3789)
* Pass `null` to FreeDriveEvent if `location` is not available [#3791](https://github.com/mapbox/mapbox-navigation-android/pull/3791)

#### Other changes
* Explicitly mark the required min Android SDK version of 21. The Nav SDK v1 series always supported only devices running Lollipop or higher (due to transitive dependencies). [#3781](https://github.com/mapbox/mapbox-navigation-android/pull/3781)

## Mapbox Navigation SDK 1.2.0 - November 18, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes

* Send _Free Drive_ events even if there're no `location` values. [#3796](https://github.com/mapbox/mapbox-navigation-android/pull/3796)

## Mapbox Navigation SDK 1.3.0-alpha.1 - November 12, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes

* Avoid dropping events when destroying MapboxNavigation. [#3770](https://github.com/mapbox/mapbox-navigation-android/pull/3770)
* Fixed Null Pointer Exception when formatting date in Telemetry. [#3770](https://github.com/mapbox/mapbox-navigation-android/pull/3770)

## Mapbox Navigation SDK 1.2.0-rc.2 - November 12, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes

* Avoid dropping events when destroying MapboxNavigation. [#3770](https://github.com/mapbox/mapbox-navigation-android/pull/3770)
* Fixed Null Pointer Exception when formatting date in Telemetry. [#3770](https://github.com/mapbox/mapbox-navigation-android/pull/3770)

## Mapbox Navigation SDK 1.2.0-rc.1 - November 11, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Improvements
* Added support for routing tiles v2. [#3761](https://github.com/mapbox/mapbox-navigation-android/pull/3761)
* Uploaded Free Drive telemetry events to the server. [#3751](https://github.com/mapbox/mapbox-navigation-android/pull/3751)
* Removed electronic horizon experimental note. [#3748](https://github.com/mapbox/mapbox-navigation-android/pull/3748)
* Tracked appropriate telemetry events for waypoints. [#3747](https://github.com/mapbox/mapbox-navigation-android/pull/3747)
* Introduced `NavigationMapboxMap.Builder` and exposed vanishing point update interval setting. [#3745](https://github.com/mapbox/mapbox-navigation-android/pull/3745)

#### Bug fixes

* Fixed an issue where the location was sometimes map-matched to a road heading in the opposite direction if the raw location's accuracy was very poor. [#3761](https://github.com/mapbox/mapbox-navigation-android/pull/3761)
* Included the last segment of `RouteAlertGeometry` represented in the `LineString` generated by `RouteAlertUtils`. [#3744](https://github.com/mapbox/mapbox-navigation-android/pull/3744)

## Mapbox Navigation SDK 1.2.0-beta.1 - November 5, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Features
* Exposed `MapMatcherResultObserver` that provides additional information about the status of the enhanced location update and the confidence of decisions that the map matcher made to generate this position. [#3730](https://github.com/mapbox/mapbox-navigation-android/pull/3730)
* Added `resetTripSession` to `MapboxNavigation` useful to transport the navigator to a new location. [#3701](https://github.com/mapbox/mapbox-navigation-android/pull/3701)

#### Improvements
* Updated `EHorizon` `mpp` functions so they can return multiple Most Probable Paths if they have the same probability. [#3740](https://github.com/mapbox/mapbox-navigation-android/pull/3740)
* For specified road classes, any section where the traffic is `'unknown'` can be represented using the `'low'` traffic congestion color rather than the `'unknown'` traffic color. [#3672](https://github.com/mapbox/mapbox-navigation-android/pull/3672)
* Taking advantage of new the `MapMatcherResultObserver` do an immediate transition in `NavigationMapboxMap` when map matcher reports teleport. [#3731](https://github.com/mapbox/mapbox-navigation-android/pull/3731)
* Improved vanishing point's state when completing the leg of a route or going off-route. [#3727](https://github.com/mapbox/mapbox-navigation-android/pull/3727)

#### Bug fixes

* Fixed an issue where `NavigationView` crashes when trying to build the `DistanceFormatter` object. [#3725](https://github.com/mapbox/mapbox-navigation-android/pull/3725)

## Mapbox Navigation SDK 1.2.0-alpha.5 - October 29, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes
* Avoid an occasional crash when setting a route due a failed attempt to generate a route geometry with a buffer. [#3710](https://github.com/mapbox/mapbox-navigation-android/pull/3710)
* Fixed an issue where an outdated information was appended to a telemetry reroute event. [#3716](https://github.com/mapbox/mapbox-navigation-android/pull/3716)

## Mapbox Navigation SDK 1.1.2 - October 29, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes
* Avoid an occasional crash when setting a route due a failed attempt to generate a route geometry with a buffer. [#3710](https://github.com/mapbox/mapbox-navigation-android/pull/3710)

## Mapbox Navigation SDK 1.0.1 - October 29, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Bug fixes
* Avoid an occasional crash when setting a route due a failed attempt to generate a route geometry with a buffer. [#3710](https://github.com/mapbox/mapbox-navigation-android/pull/3710)

## Mapbox Navigation SDK 1.2.0-alpha.4 - October 28, 2020
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog
#### Improvements
* Removed some synchronized disk access events from upstream library. [#3675](https://github.com/mapbox/mapbox-navigation-android/pull/3675)
* Improved exceptions handling when toggling native history collection. [#3705](https://github.com/mapbox/mapbox-navigation-android/pull/3705)

#### Bug fixes
* Fixed additional edge cases that occured when trying to infer the route line's z-ordering on the map if not explicitly specified. [#3687](https://github.com/mapbox/mapbox-navigation-android/pull/3687) [#3702](https://github.com/mapbox/mapbox-navigation-android/pull/3702)
* Fixed an issue that would make `off-route` and `invalid-route` inconsistent when there were no available road edges to map-match to. The state will now report `off-route` in those situations. [#3705](https://github.com/mapbox/mapbox-navigation-android/pull/3705)

#### Other changes
* Fixed `BannerInstructions#willDisplay` which was marked non-nullable and unusable from Kotlin codebases. [#3679](https://github.com/mapbox/mapbox-navigation-android/pull/3679)
* Upgraded Maps SDK dependency to [`v9.6.0-beta.1`](https://github.com/mapbox/mapbox-gl-native-android/releases/tag/android-v9.6.0-beta.1). This includes performing an immediate puck/camera transition to the current location when resuming the map instead of an animated transition. [#3677](https://github.com/mapbox/mapbox-navigation-android/pull/3677)
* Updated default routing tiles version to `2020_10_18-03_00_00`. [#3686](https://github.com/mapbox/mapbox-navigation-android/pull/3686)

## Mapbox Navigation SDK 1.2.0-alpha.3 - October 21, 2020

Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog

#### Improvements
* Update the algorithm to calculate the vanishing point of the route to use EPSG:3857 projection instead of turf measurements. This greatly increases the precision, even on extremely long routes. It also improves the performance of the `MapRouteLine`. [#3661](https://github.com/mapbox/mapbox-navigation-android/pull/3661)

#### Bug fixes
* Assign `currentHorizon`, `currentType` and `currentPosition` from the main thread to avoid synchronization issues [#3653](https://github.com/mapbox/mapbox-navigation-android/pull/3653)
* Keep the selected route as primary after map style has been changed [#3664](https://github.com/mapbox/mapbox-navigation-android/pull/3664)

#### Other changes
* Remove dependency of UI SDK on the modular notification classes. This fixes an issue where the default `TripNotification` implementation couldn't be replaced with a custom one. [#3665](https://github.com/mapbox/mapbox-navigation-android/pull/3665)

## Mapbox Navigation SDK 1.1.1 - October 15, 2020

Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog

#### Other changes
* Bump mapbox-common-native version to 7.1.1 [#3651](https://github.com/mapbox/mapbox-navigation-android/pull/3651)
* Bump mapbox-android-telemetry version to 6.2.1 [#3652](https://github.com/mapbox/mapbox-navigation-android/pull/3652)
* Bump mapbox-core version to 3.1.0 [#3652](https://github.com/mapbox/mapbox-navigation-android/pull/3652)

## Mapbox Navigation SDK 1.2.0-alpha.1 - October 9, 2020

Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog

#### Features
* Added incident alerts [#3640](https://github.com/mapbox/mapbox-navigation-android/pull/3640)

#### Bug fixes
* Fix replay bearing for small maneuvers [#3636](https://github.com/mapbox/mapbox-navigation-android/pull/3636)

#### Other changes
* Bump mapbox-navigation-native version to 24.0.0 [#1955](https://github.com/mapbox/mapbox-navigation-android/pull/3633)
* Bump mapboxEvents dependency to 6.2.0 and mapboxCore dependency to 3.1.0 [#3621](https://github.com/mapbox/mapbox-navigation-android/pull/3621)
* Bump Maps SDK to 9.6.0-alpha.1 [#3632](https://github.com/mapbox/mapbox-navigation-android/pull/3632)

## Mapbox Navigation SDK 1.1.0 - October 7, 2020

Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/navigation/overview) for more information.

### Changelog

#### Performance improvements
* Improved the time to first banner instructions by updating data from navigator status right after a route is set [#3431](https://github.com/mapbox/mapbox-navigation-android/pull/3431)

#### Bug fixes
* Fixed route / isOffRoute race condition which resulted in occasional missed offroute events [#3424](https://github.com/mapbox/mapbox-navigation-android/pull/3424)
* Fixed incorrect telemetry location timestamp format which resulted in missing feedback events [#3456](https://github.com/mapbox/mapbox-navigation-android/pull/3456)
* Fixed order of the routes ignored when drawing [#3525](https://github.com/mapbox/mapbox-navigation-android/pull/3525)
* Fixed annotation of `NavigationMapRoute.Builder#build` to be `@NonNull` [#3510](https://github.com/mapbox/mapbox-navigation-android/pull/3510)
* Adjusted RTL layout handling which fixed minor issues, especially in the `InstructionView` [#3426](https://github.com/mapbox/mapbox-navigation-android/pull/3426)
* Resolved a bug where a route casing (route line border) was not visible [#3472](https://github.com/mapbox/mapbox-navigation-android/pull/3472)

#### Other changes
* Move post user feedback out of companion [#3529](https://github.com/mapbox/mapbox-navigation-android/pull/3529)
* Merged onboard, offboard and hybrid routers together, leaving only one modular router component [#3498](https://github.com/mapbox/mapbox-navigation-android/pull/3498)
* Updated Mapbox Gradle plugins and contributing docs [#3370](https://github.com/mapbox/mapbox-navigation-android/pull/3370)
* Updated return types, class annotations and docs, exposed route options updater [#3429](https://github.com/mapbox/mapbox-navigation-android/pull/3429)
* Bumped Kotlin version to 1.4.0 and Coroutines version to 1.3.9 [#3445](https://github.com/mapbox/mapbox-navigation-android/pull/3445)
* Fix missing destination marker regression when using the Drop-in UI [#3462](https://github.com/mapbox/mapbox-navigation-android/pull/3462)
* Updated Nav UI day and night styles to new stable production v1 versions [#3520](https://github.com/mapbox/mapbox-navigation-android/pull/3520)

## Mapbox Navigation SDK 1.0.0 - October 2, 2020
Today, we are releasing Navigation SDK (Core & UI components) v.1.0 for Android 🎉 🚀

### Core Components
This upgrade features
- a higher accuracy location engine which functions even in low GPS quality scenarios such as tunnels or overpasses,
- free-drive mode which enables accurate location positioning even without an active route for daily commuting,
- a new modular architecture that allows developers to customize the navigation experience for their use case,
- and MAUs-based billing that provides predictability in costs to developers.

### UI Components
This release as compared to legacy offers all the features but with much finer control and granularity. It mainly serves as a port of the legacy UI SDK implementation to use the `1.0` version of the Navigation SDK (Core components) and its features. The `1.0` UI Components also removes redundant methods & APIs while exposing new ones instead. It brings in new features, including:

- Two different ways of providing feedback during a trip session, thereby helping Mapbox provide better route quality, turn-by-turn experiences, traffic congestion, etc.
- Allowing developers to deemphasize portions of the route line behind the puck, thereby reflecting route progress state.
- Providing UI components that visualize a single building footprint or extrusion. Great to use for marking the final destination in an improved arrival experience.

### Pricing
Applications built with v1.0.0+ are billed based only on [monthly active users](https://docs.mapbox.com/help/glossary/monthly-active-users/), specifically ["Navigation SDKs" MAUs](https://www.mapbox.com/pricing/#navmaus). Navigation SDK MAUs include Directions API, Vector Tiles API, and Raster Tiles API requests with no upfront commitments or annual contracts. A Navigation SDK MAU becomes a billable event only once a user utilizes the Navigation SDK for turn-by-turn directions or for free-drive.

A single user is billed as one MAU across app upgrades as long as the app is not deleted. Deleting and re-installing an app that uses the Navigation SDK would result in an additional MAU. This happens because the SDK does not store personally identifying information.

To see the number of Navigation SDKs MAUs included in the free tier and the cost per Navigation SDKs MAU beyond the free tier, see the Navigation SDKs section of our [pricing page](https://www.mapbox.com/pricing/#navmaus).

### Other docs

Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/beta/navigation/overview) for more information.

Please review the [developer documentation](https://docs.mapbox.com/android/navigation/overview/#installation) to start building with the Mapbox Navigation SDK v1.0 for Android.

Already use an older version of the Navigation SDK? Check out [the migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) to transition your project from the "legacy" Navigation SDK to the 1.0 version.

Let us know if you have any questions or run into issues and please open tickets in https://github.com/mapbox/mapbox-navigation-android/issues/new and we will take it from there! We would love your feedback.

Thank you!

### Mapbox Navigation SDK 1.0.0-rc.8 - September 9, 2020

Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/beta/navigation/overview) for more information.

#### Changelog
Changes since Mapbox Navigation SDK `1.0.0-rc.7`:
* Move post user feedback out of companion [#3529](https://github.com/mapbox/mapbox-navigation-android/pull/3529)
* Fix missing destination marker regression when using the Drop-in UI [#3462](https://github.com/mapbox/mapbox-navigation-android/pull/3462)

#### Known issues
* Vanishing route line (deemphasizing the traveled portion of the route) can be slightly out of sync with the location puck.

### Mapbox Navigation SDK 1.0.0-rc.7 - September 2, 2020

Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/beta/navigation/overview) for more information.

#### Changelog
Changes since Mapbox Navigation SDK `1.0.0-rc.6`:
* Fixed order of the routes ignored when drawing [#3525](https://github.com/mapbox/mapbox-navigation-android/pull/3525)
* Updated Nav UI day and night styles to new stable production v1 versions [#3520](https://github.com/mapbox/mapbox-navigation-android/pull/3520)
* Fixed annotation of `NavigationMapRoute.Builder#build` to be `@NonNull` [#3510](https://github.com/mapbox/mapbox-navigation-android/pull/3510)

#### Known issues
* Vanishing route line (deemphasizing the traveled portion of the route) can be slightly out of sync with the location puck.

### Mapbox Navigation SDK 1.0.0-rc.6 - August 26, 2020
This release accelerates the version naming of the Navigation UI SDK to match the Navigation Core SDK artifact, both are named `1.0.0-rc.6`.
Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/beta/navigation/overview) for more information.

#### Changelog
Changes since the Mapbox Navigation UI `1.0.0-rc.1` and Core `1.0.0-rc.5`:
* Adjusted RTL layout handling which fixed minor issues, especially in the `InstructionView` [#3426](https://github.com/mapbox/mapbox-navigation-android/pull/3426)
* Merged onboard, offboard and hybrid routers together, leaving only one modular router component [#3498](https://github.com/mapbox/mapbox-navigation-android/pull/3498)
* Resolved a bug where a route casing (route line border) was not visible [#3472](https://github.com/mapbox/mapbox-navigation-android/pull/3472)

#### Known issues
* Vanishing route line (deemphasizing the traveled portion of the route) can be slightly out of sync with the location puck.

### Mapbox Navigation UI SDK 1.0.0-rc.1 & Mapbox Navigation Core SDK 1.0.0-rc.5 - August 19, 2020
This version introduces a UI component of the SDK compatible with the 1.0.0 pre-release series.

The `1.0` UI SDK release as compared to legacy offers all the features but with much finer control and granularity. This version mainly serves as a port of the legacy UI SDK implementation to use the `1.0` version of the Navigation Core SDK and its features. The `1.0` UI SDK also removes redundant methods & APIs while exposing new ones instead. The SDK also brings new features, including:

- Two different ways of providing feedback during a trip session, thereby helping Mapbox provide better route quality, turn-by-turn experiences, traffic congestion, etc.

- Allowing developers to deemphasize portions of the route line behind the puck, thereby reflecting route progress state.

- Providing UI components that visualize a single building footprint or extrusion. Great to use for marking the final destination in an improved arrival experience.

Visit our [1.0.0 migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) and the [documentation pages](http://docs.mapbox.com/android/beta/navigation/overview) for more information.

#### Changelog
Changes since the `1.0.0-rc.4` release of the Mapbox Navigation Core SDK:
* Updated Mapbox Gradle plugins and contributing docs [#3370](https://github.com/mapbox/mapbox-navigation-android/pull/3370)
* Fixed route / isOffRoute race condition which resulted in occasional missed offroute events [#3424](https://github.com/mapbox/mapbox-navigation-android/pull/3424)
* Improved the time to first banner instructions by updating data from navigator status right after a route is set [#3431](https://github.com/mapbox/mapbox-navigation-android/pull/3431)
* Updated return types, class annotations and docs, exposed route options updater [#3429](https://github.com/mapbox/mapbox-navigation-android/pull/3429)
* Bumped Kotlin version to 1.4.0 and Coroutines version to 1.3.9 [#3445](https://github.com/mapbox/mapbox-navigation-android/pull/3445)
* Fixed incorrect telemetry location timestamp format which resulted in missing feedback events [#3456](https://github.com/mapbox/mapbox-navigation-android/pull/3456)

#### Known issues
All of the issues are targeted to be resolved before this version becomes stable:
* Casing (a darker border color) of the primary route line is missing.
* Vanishing route line (deemphasizing the traveled portion of the route) can be slightly out of sync with the location puck.

### Mapbox Navigation Core SDK 1.0.0-rc.x series

This upgrade features a higher accuracy location engine which functions even in low GPS quality scenarios such as tunnels or overpasses, free-drive mode which enables accurate location positioning even without an active route for daily commuting, a new modular architecture that allows developers to customize the navigation experience for their use case, and MAUs-based billing that provides predictability in costs to developers.

Please review the [developer documentation](https://docs.mapbox.com/android/beta/navigation/overview/#installation) to start building with the Mapbox Navigation SDK v1.0 for Android.

Already use an older version of the Navigation SDK? Check out [the migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) to transition your project from the "legacy" core Navigation SDK to the 1.0 version.

Let us know if you have any questions or run into issues and please open tickets in https://github.com/mapbox/mapbox-navigation-android/issues/new and we will take it from there! We would love your feedback.

Thank you!

- Mapbox Navigation Core SDK 1.0.0-rc.4 - August 6, 2020
- Mapbox Navigation Core SDK 1.0.0-rc.3 - July 27, 2020
- Mapbox Navigation Core SDK 1.0.0-rc.2 - June 17, 2020
- Mapbox Navigation Core SDK 1.0.0-rc.1 - June 3, 2020

### v0.42.6 - March 16, 2020

* Fix missing event telemetry when Proguard is enabled [#2587](https://github.com/mapbox/mapbox-navigation-android/pull/2587)
* fix textAllCaps conflicting with material design [#2585](https://github.com/mapbox/mapbox-navigation-android/pull/2585)
* Add missing RouteOptions to DirectionsBuilder [#2573](https://github.com/mapbox/mapbox-navigation-android/pull/2573)
* Fix libandroid-navigation and libandroid-navigation-ui Javadoc task [#2534](https://github.com/mapbox/mapbox-navigation-android/pull/2534)

### v0.42.5 - February 11, 2020

* Backport #2315 to base-v0.42.1 [#2318](https://github.com/mapbox/mapbox-navigation-android/pull/2318)
* Ensure to use valid MapRouteArrow and MapRouteLine Layer references during style change [#2315](https://github.com/mapbox/mapbox-navigation-android/pull/2315)
* Cherry pick #2307 (master) into base-v0.42.1 [#2308](https://github.com/mapbox/mapbox-navigation-android/pull/2308)
* Bump mapbox-navigation-native version to 7.0.0 in base-v0.42.1 branch [#2294](https://github.com/mapbox/mapbox-navigation-android/pull/2294)
* Cherry pick #2287 (master) into base-v0.42.1 [#2292](https://github.com/mapbox/mapbox-navigation-android/pull/2292)


### v0.42.4 - November 25, 2019

* Bumped Java SDK dependency to `4.9.0` [#2043](https://github.com/mapbox/mapbox-navigation-android/pull/2043)
* Update translations to latest Transifex [#2273](https://github.com/mapbox/mapbox-navigation-android/pull/2273) [#2277](https://github.com/mapbox/mapbox-navigation-android/pull/2277)
* Persist routes across style changes [#2262](https://github.com/mapbox/mapbox-navigation-android/pull/2262)
* Off-route threshold options [#2276](https://github.com/mapbox/mapbox-navigation-android/pull/2276)

### v0.42.3 - November 11, 2019

* Fix synchronization issue in MapRouteLine / NavigationMapRoute [#2256](https://github.com/mapbox/mapbox-navigation-android/pull/2256)

### v0.42.2 - November 6, 2019

* Disable sideloading functionality in _Dynamic offline_ feature [#2248](https://github.com/mapbox/mapbox-navigation-android/pull/2248)

### v0.42.1 - October 18, 2019

* Roundabout maneuver icon depicts counterclockwise movement when driving on the left [#2228](https://github.com/mapbox/mapbox-navigation-android/pull/2228)

### v0.42.0 - September 20, 2019

Note: This release breaks `SEMVER` / contains API breaking changes. Please consult this [migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/0.42.0-Migration-Guide) for the necessary updates required.

* Change driver feedback UI [#2054](https://github.com/mapbox/mapbox-navigation-android/pull/2054)
* [SEMVER] Fix navigation view memory leak [#2051](https://github.com/mapbox/mapbox-navigation-android/pull/2051)
* [SEMVER] Remove never used apis causing unnecessary memory issues [#2052](https://github.com/mapbox/mapbox-navigation-android/pull/2052)
* [SEMVER] Fix backwards instructions in left-side driving scenarios [#2044](https://github.com/mapbox/mapbox-navigation-android/pull/2044)
* Fix rerouting inside the NavigationUI [#2010](https://github.com/mapbox/mapbox-navigation-android/issues/2010)
* Fix on route selection change listener being called if route is not visible [#2035](https://github.com/mapbox/mapbox-navigation-android/pull/2035)
* [SEMVER] Fix NavigationStepData regression from #1890 [#2015](https://github.com/mapbox/mapbox-navigation-android/pull/2015)
* Bump mapbox-android-sdk version to 8.2.1 [#2013](https://github.com/mapbox/mapbox-navigation-android/pull/2013)
* Bump Mapbox Annotation Plugin version to v8 0.7.0 [#2014](https://github.com/mapbox/mapbox-navigation-android/pull/2014)
* Auto generate license for the SDK [#2002](https://github.com/mapbox/mapbox-navigation-android/pull/2002)
* Update translations to latest Transifex [#2003](https://github.com/mapbox/mapbox-navigation-android/pull/2003)

### v0.41.0 - July 11, 2019

* Fix navigation camera tracking the puck [#1995](https://github.com/mapbox/mapbox-navigation-android/pull/1995)
* Move events from telemetry to nav sdk [#1890](https://github.com/mapbox/mapbox-navigation-android/pull/1890)
* Fix DynamicCamera#CameraPosition.zoom NPE [#1979](https://github.com/mapbox/mapbox-navigation-android/pull/1979)
* Update ComponentNavigationActivity example [#1978](https://github.com/mapbox/mapbox-navigation-android/pull/1978)

### v0.40.0 - June 12, 2019

* Fix notification instruction not updated for arrive maneuver [#1959](https://github.com/mapbox/mapbox-navigation-android/pull/1959)
* Bump mapbox-navigation-native version to 6.2.1 [#1955](https://github.com/mapbox/mapbox-navigation-android/pull/1955)

### v0.39.0 - May 29, 2019

* Add check to avoid ArrayIndexOutOfBoundsExceptions from MapRouteLine#drawWayPoints [#1951](https://github.com/mapbox/mapbox-navigation-android/pull/1951)
* Fix way name truncating too soon [#1947](https://github.com/mapbox/mapbox-navigation-android/pull/1947)
* Fix instruction icon mismatch in between banner and notification [#1946](https://github.com/mapbox/mapbox-navigation-android/pull/1946)

### v0.38.0 - May 16, 2019

* Add option to load offline maps database for NavigationView [#1895](https://github.com/mapbox/mapbox-navigation-android/pull/1895)
* Update Maps SDK to 7.4.0 [#1907](https://github.com/mapbox/mapbox-navigation-android/pull/1907)
* Added walking options [#1934](https://github.com/mapbox/mapbox-navigation-android/pull/1934)
* SoundButton clicklistener wasn't set properly [#1937](https://github.com/mapbox/mapbox-navigation-android/pull/1937)

### v0.37.0 - May 1, 2019

* Added check for legs to route refresh [#1916](https://github.com/mapbox/mapbox-navigation-android/pull/1916)
* Improve PR Template [#1915](https://github.com/mapbox/mapbox-navigation-android/pull/1915)
* Fix NavigationLauncherActivity camera animations [#1913](https://github.com/mapbox/mapbox-navigation-android/pull/1913)
* Fix Navigation Launcher test app Activity incorrect profile regression [#1914](https://github.com/mapbox/mapbox-navigation-android/pull/1914)
* Update Java Services to 4.7.0 [#1906](https://github.com/mapbox/mapbox-navigation-android/pull/1906)
* Add connectivity status check to SpeechPlayer provider [#1901](https://github.com/mapbox/mapbox-navigation-android/pull/1901)
* Fix intermediate way point arrival not triggered [#1908](https://github.com/mapbox/mapbox-navigation-android/pull/1908)
* Check for valid DirectionsRoute in RouteRefresh [#1909](https://github.com/mapbox/mapbox-navigation-android/pull/1909)
* ExampleActivity refactor for simplicity/stability [#1884](https://github.com/mapbox/mapbox-navigation-android/pull/1884)
* Allow default notification color setting via MapboxNavigationOptions [#1899](https://github.com/mapbox/mapbox-navigation-android/pull/1899)

### v0.36.0 - April 17, 2019

Note: This release breaks `SEMVER` / contains API breaking changes. Please consult this [migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/0.36.0-Migration-Guide) for the necessary updates required.

* Prevent RouteLeg list access for current step creation [#1896](https://github.com/mapbox/mapbox-navigation-android/pull/1896)
* Create NavigationViewRouter timeout to unblock routing state [#1888](https://github.com/mapbox/mapbox-navigation-android/pull/1888)
* Bump mapbox-android-sdk version to 7.3.2 [#1894](https://github.com/mapbox/mapbox-navigation-android/pull/1894)
* Add NavigationMapRoute attribute for styling route line cap expression [#1818](https://github.com/mapbox/mapbox-navigation-android/pull/1818)
* Update navigator to 6.1.3 [#1892](https://github.com/mapbox/mapbox-navigation-android/pull/1892)
* [SEMVER] Add NavigationMapboxMap#addCustomMarker for usage of SymbolManager [#1891](https://github.com/mapbox/mapbox-navigation-android/pull/1891)
* Fix notification and banner ETAs not in sync [#1889](https://github.com/mapbox/mapbox-navigation-android/pull/1889)
* Bump mapbox-navigation-native version to 6.1.2 [#1885](https://github.com/mapbox/mapbox-navigation-android/pull/1885)
* Add offline version check to Navigation View Router [#1864](https://github.com/mapbox/mapbox-navigation-android/pull/1864)
* Add offline options to Navigation Launcher [#1862](https://github.com/mapbox/mapbox-navigation-android/pull/1862)
* [SEMVER] Add dynamic offline routing to NavigationView [#1829](https://github.com/mapbox/mapbox-navigation-android/pull/1829)

### v0.35.0 - April 12, 2019

* Github PR template refactor [#1879](https://github.com/mapbox/mapbox-navigation-android/pull/1879)
* Add custom history events for MapboxNavigation [#1881](https://github.com/mapbox/mapbox-navigation-android/pull/1881)
* Updated mapbox java to 4.6.0 [#1877](https://github.com/mapbox/mapbox-navigation-android/pull/1877)
* Bump mapbox-android-sdk version to 7.3.1 [#1880](https://github.com/mapbox/mapbox-navigation-android/pull/1880)
* Bump mapbox-android-telemetry version to 4.3.0 [#1876](https://github.com/mapbox/mapbox-navigation-android/pull/1876)
* Bump mapbox-android-plugin-annotation-v7 version to 0.6.0 [#1867](https://github.com/mapbox/mapbox-navigation-android/pull/1867)
* Add check to only start Route Processor Background Thread once [#1866](https://github.com/mapbox/mapbox-navigation-android/pull/1866)
* Add periodic refresh [#1855](https://github.com/mapbox/mapbox-navigation-android/pull/1855)

### v0.34.0 - April 2, 2019

Note: This release breaks `SEMVER` / contains API breaking changes. Please consult this [migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/0.34.0-Migration-Guide) for the necessary updates required.

* Add remove offline routing tiles by bounding box functionality [#1850](https://github.com/mapbox/mapbox-navigation-android/pull/1850)
* Update LocationComponent to use LocationComponentActivationOptions [#1852](https://github.com/mapbox/mapbox-navigation-android/pull/1852)
* [SEMVER] Add NavigationCamera#update for MapboxMap animations [#1849](https://github.com/mapbox/mapbox-navigation-android/pull/1849)
* Update Maps SDK to 7.3.0 [#1844](https://github.com/mapbox/mapbox-navigation-android/pull/1844)
* Fix proguard [#1816](https://github.com/mapbox/mapbox-navigation-android/pull/1816)
* Setup native crash monitor for test application [#1841](https://github.com/mapbox/mapbox-navigation-android/pull/1841)

### v0.33.2 - March 22, 2019

* Bump mapbox-navigation-native version to 6.0.0 [#1836](https://github.com/mapbox/mapbox-navigation-android/pull/1836)
* Update NavigationCamera resetting state if transition cancelled [#1835](https://github.com/mapbox/mapbox-navigation-android/pull/1835)
* Added metadata to NavigationPerformanceEvent [#1820](https://github.com/mapbox/mapbox-navigation-android/pull/1820)
* Update NavigationView NavigationListener to triggered when initialized [#1807](https://github.com/mapbox/mapbox-navigation-android/pull/1807)

### v0.33.1 - March 20, 2019

* Finish camera reset during MapboxMap cancel events [#1830](https://github.com/mapbox/mapbox-navigation-android/pull/1830)

### v0.33.0 - March 18, 2019

* Add DirectionsRouteType for starting navigation with annotation data [#1819](https://github.com/mapbox/mapbox-navigation-android/pull/1819)
* Added attribute to capture event name in performance trace event [#1800](https://github.com/mapbox/mapbox-navigation-android/pull/1800)
* Add LocationComponent FPS throttle based on map zoom [#1815](https://github.com/mapbox/mapbox-navigation-android/pull/1815)
* Fix null ResponseBody in VoiceInstructionLoader [#1813](https://github.com/mapbox/mapbox-navigation-android/pull/1813)

### v0.32.0 - March 11, 2019

* Adjust NavigationCamera zoom reset behavior [#1802](https://github.com/mapbox/mapbox-navigation-android/pull/1802)
* Update Android Core to 1.2.0 [#1805](https://github.com/mapbox/mapbox-navigation-android/pull/1805)
* Update Maps SDK to 7.2.0 [#1804](https://github.com/mapbox/mapbox-navigation-android/pull/1804)
* Add RawLocationListener for direct updates from LocationEngine [#1803](https://github.com/mapbox/mapbox-navigation-android/pull/1803)
* Fix event simulation flag field wrongly reported [#1799](https://github.com/mapbox/mapbox-navigation-android/pull/1799)
* Fix missing cancel event / not sent [#1796](https://github.com/mapbox/mapbox-navigation-android/pull/1796)
* Add manifest placeholder for enabling / disabling Crashlytics automatically [#1795](https://github.com/mapbox/mapbox-navigation-android/pull/1795)

### v0.31.0 - March 6, 2019

Note: This release breaks `SEMVER` / contains API breaking changes.  Please consult this [migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/0.31.0-Migration-Guide) for the necessary updates required.

* [SEMVER] Remove directionsProfile from NavigationUiOptions [#1787](https://github.com/mapbox/mapbox-navigation-android/pull/1787)
* Add initial GPS event [#1777](https://github.com/mapbox/mapbox-navigation-android/pull/1777)
* Add Android P 440 density support to UrlDensityMap [#1785](https://github.com/mapbox/mapbox-navigation-android/pull/1785)
* Add DepartEventFactory for handling route departure events [#1772](https://github.com/mapbox/mapbox-navigation-android/pull/1772)
* Fix destination marker not drawn on Style reload [#1779](https://github.com/mapbox/mapbox-navigation-android/pull/1779)
* Fix plugged wireless battery monitor check [#1782](https://github.com/mapbox/mapbox-navigation-android/pull/1782)
* Fix navigation cancel button [#1776](https://github.com/mapbox/mapbox-navigation-android/pull/1776)
* Fix NavigationRoute bearing order [#1775](https://github.com/mapbox/mapbox-navigation-android/pull/1775)
* Add Github PR template [#1766](https://github.com/mapbox/mapbox-navigation-android/pull/1766)
* Add Crashlytics to test app [#1775](https://github.com/mapbox/mapbox-navigation-android/pull/1775)

### v0.30.0 - February 18, 2019

* Fix battery charge reporter NPE [#1750](https://github.com/mapbox/mapbox-navigation-android/pull/1750)
* Adjust continueStraight to default for NavigationRoute [#1748](https://github.com/mapbox/mapbox-navigation-android/pull/1748)
* Bump NN version to 5.0.0 [#1744](https://github.com/mapbox/mapbox-navigation-android/pull/1744)
* Ignore primary route update tasks with empty collections [#1742](https://github.com/mapbox/mapbox-navigation-android/pull/1742)

### v0.29.0 - February 13, 2019

Note: This release breaks `SEMVER` / contains API breaking changes.  Please consult this [migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/0.29.0-Migration-Guide) for the necessary updates required.

* Added RouteRefresh as a wrapper class for MapboxDirectionsRefresh [#1738](https://github.com/mapbox/mapbox-navigation-android/pull/1738)
* Bump mapbox sdk services version to 4.5.0 [#1736](https://github.com/mapbox/mapbox-navigation-android/pull/1736)
* Prevent route feature collections IndexOutOfBoundsException in PrimaryRouteUpdateTask [#1735](https://github.com/mapbox/mapbox-navigation-android/pull/1735)
* Update RouteRetrievalEvent [#1731](https://github.com/mapbox/mapbox-navigation-android/pull/1731)
* Add interceptor and event listener support for NavigationRoute [#1734](https://github.com/mapbox/mapbox-navigation-android/pull/1734)
* Add battery event [#1729](https://github.com/mapbox/mapbox-navigation-android/pull/1729)
* Add silent waypoints support into NavigationRoute [#1733](https://github.com/mapbox/mapbox-navigation-android/pull/1733)
* Update Maps SDK 7.1.2 [#1728](https://github.com/mapbox/mapbox-navigation-android/pull/1728)
* Fix issue with mute persistence between navigation sessions / rotation [#1726](https://github.com/mapbox/mapbox-navigation-android/pull/1726)
* Added RouteRetrievalEvent [#1661](https://github.com/mapbox/mapbox-navigation-android/pull/1661)
* [SEMVER] Allow multiple route simulations with NavigationView [#1724](https://github.com/mapbox/mapbox-navigation-android/pull/1724)
* Update Maps SDK 7.1.1 and Annotation Plugin 0.5.0 [#1722](https://github.com/mapbox/mapbox-navigation-android/pull/1722)
* Return false for OnMapClick listeners [#1717](https://github.com/mapbox/mapbox-navigation-android/pull/1717)

### v0.28.0 - January 30, 2019

Note: This release breaks `SEMVER` / contains API breaking changes.  Please consult this [migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/0.28.0-Migration-Guide) for the necessary updates required.

* Delay initialization of FpsDelegate / WayName in NavigationMapboxMap [#1700](https://github.com/mapbox/mapbox-navigation-android/pull/1700)
* Update Maps SDK 7.1.0 [#1712](https://github.com/mapbox/mapbox-navigation-android/pull/1712)
* [SEMVER] Added exit signs to the instruction banner and refactored instruction loader [#1195](https://github.com/mapbox/mapbox-navigation-android/pull/1195)
* [SEMVER] Replace deprecated maker usage in NavigationMapboxMap with SymbolManager [#1707](https://github.com/mapbox/mapbox-navigation-android/pull/1707)
* Add ManeuverView APIs for adjusting primary and secondary color [#1709](https://github.com/mapbox/mapbox-navigation-android/pull/1709)
* Update translations from Transifex [#1704](https://github.com/mapbox/mapbox-navigation-android/pull/1704)
* Provide default LocationComponent style when not found [#1696](https://github.com/mapbox/mapbox-navigation-android/pull/1696)
* Update support lib 28.0.0 to fix issue with ViewModel restoration [#1690](https://github.com/mapbox/mapbox-navigation-android/pull/1690)
* [SEMVER] Update TurnLaneView to use VectorDrawable instead of StyleKit [#1695](https://github.com/mapbox/mapbox-navigation-android/pull/1695)
* Fix activation of default LocationEngine for LocationComponent [#1701](https://github.com/mapbox/mapbox-navigation-android/pull/1701)
* Adjust route overview to account for lifecycle [#1688](https://github.com/mapbox/mapbox-navigation-android/pull/1688)

### v0.27.0 - January 16, 2019

Note: This release breaks `SEMVER` / contains API breaking changes.  Please consult this [migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/0.27.0-Migration-Guide) for the necessary updates required.

* [SEMVER] Consume banner instructions from NN [#1543](https://github.com/mapbox/mapbox-navigation-android/pull/1543)
* Update InstructionLoader API as public / taking BannerText [#1683](https://github.com/mapbox/mapbox-navigation-android/pull/1683)
* Removed multiple instantiations of MapboxOfflineRouter from OfflineRegionDownloadActivity [#1684](https://github.com/mapbox/mapbox-navigation-android/pull/1684)
* Add Streets Source v7 and v8 checks for way name layer [#1679](https://github.com/mapbox/mapbox-navigation-android/pull/1679)
* Add dynamic map FPS adjustment for NavigationMapboxMap [#1669](https://github.com/mapbox/mapbox-navigation-android/pull/1669)
* Add ability to disable auto-increment of RouteLeg index [#1643](https://github.com/mapbox/mapbox-navigation-android/pull/1643)
* Tracking gestures management to true in default NavigationView styles [#1682](https://github.com/mapbox/mapbox-navigation-android/pull/1682)
* Fix AlertView animation leak [#1667](https://github.com/mapbox/mapbox-navigation-android/pull/1667)
* NavigationView do not allow way name to show in overview mode [#1676](https://github.com/mapbox/mapbox-navigation-android/pull/1676)
* [SEMVER] Set ssmlAnouncement when using SpeechAnnouncementListener [#1675](https://github.com/mapbox/mapbox-navigation-android/pull/1675)
* [SEMVER] Update Maps SDK 7.x and events 4.x with new location APIs [#1615](https://github.com/mapbox/mapbox-navigation-android/pull/1615)
* Update dependencies: ConstraintLayout, mockito, leakCanary, robolectric [#1668](https://github.com/mapbox/mapbox-navigation-android/pull/1668)
* Remove unused / package-private RouteStepProgress#nextStep [#1666](https://github.com/mapbox/mapbox-navigation-android/pull/1666)
* Return the actual error coming back from NN when fetching an offline route fails [#1660](https://github.com/mapbox/mapbox-navigation-android/pull/1660)
* Create RouteProgress after leg index check in RouteProcessorRunnable [#1657](https://github.com/mapbox/mapbox-navigation-android/pull/1657)
* Use MapboxMap camera animation for resetting NavigationCamera [#1658](https://github.com/mapbox/mapbox-navigation-android/pull/1658)
* Allow NavigationView to start navigation during existing session [#1655](https://github.com/mapbox/mapbox-navigation-android/pull/1655)
* Happy 2019 🎉🚗 [#1654](https://github.com/mapbox/mapbox-navigation-android/pull/1654)
* Bump Maps and Events dependencies [#1651](https://github.com/mapbox/mapbox-navigation-android/pull/1651)

### v0.26.0 - December 20, 2018

Note: This release breaks `SEMVER` / contains API breaking changes.  Please consult this [migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/0.26.0-Migration-Guide) for the necessary updates required.

* NavigationMapRoute 2.0 #1387
* [SEMVER] Use Android View instead of runtime styling for way name #1621
* Update Maps SDK 6.8.0 #1642
* Add waypoint targets support into NavigationRoute #1640

### v0.25.0 - December 16, 2018

* Update Navigator to 3.4.11 #1635
* Bump mapbox-navigation-native version to 3.4.10 #1631
* Changed the elevation of the InstructionView so that it doesn't encounter overlapping issues. #1089
* Include 'exit roundabout' in Navigation Constants #1628
* Fix voice instruction cache not getting initialized with injected speech player #1627
* Fix turn-by-turn UI voice instructions repeated after a config change #1622
* Check for successful tar response in TarFetchedCallback #1620
* Generate unique filenames for archives in check binary size script #1619
* Move SummaryBottomSheet above RecenterBtn in navigation_view_layout.xml #1616
* Update script-git-version.gradle with try/catch #1617
* Target API 28 and add FOREGROUND_SERVICE permission #1612
* Add check to only cache instructions if the language setup is supported by Voice API #1610
* Attempt to fix the coverage job #1601

### v0.24.1 - December 5, 2018

* Increment leg index upon way point arrival #1604
* Update Navigator 3.4.9 #1603
* Bump mapboxSdkServices version to 4.2.0 #1599
* Setup CI to publish the sample app to Google Play #1597
* Add last commit hash to test app settings #1590

### v0.24.0 - December 3, 2018

Note: This release breaks `SEMVER` / contains API breaking changes.  Please consult this [migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/0.24.0-Migration-Guide) for the necessary updates required.

* Add proguard rule for MapboxTelemetry #1593
* Update Navigator to 3.4.7 #1592
* Bump mapbox-navigation-native version to 3.4.6 #1586
* Update Java Services 4.1.1 #1585
* Bump mapbox-android-sdk and mapbox-android-telemetry versions #1584
* Only allow choosing an offline version with valid data #1583
* Add bearing to origin of offline route requests in OfflineRouteFinder #1582
* Update permissions and preferences for ExampleActivity #1581
* Enable history recording in example activity #1580
* Add write external permission storage to app AndroidManifest #1578

### v0.24.0-beta.2 - November 29, 2018

* Route tile download #1559
* Update OfflineTileVersions visibility and add to MapboxOfflineRouter #1571
* Fix the progressUpdateListener for Tile Pack Unpacking #1567
* Update MapboxOfflineRoute APIs and callback naming #1569
* Fix milestone / progress listener leaks from NavigationView #1552
* Fix JSON file names (repo name) in check binary size script #1564
* Fix malformed binary size JSON #1563
* Integrate navigation state changes from latest events library version #1562
* Ignore StyleKit files for codecov #1561
* Check for DynamicCamera in ExampleViewModel shutdown #1560
* Fix cache is closed crash #1555
* Bump mapboxEvents and mapboxNavigator versions #1558
* Check for valid route with length in MockNavigationActivity #1556
* Update processor tests and remove unused helper code #1536
* Add binary size metric and push to loading dock #1554
* Fix UI tests with DirectionsRoute test fixture #1548
* Add codecov.yml #1551
* Nav Native API update #1547
* Add listener for updates to map way name #1544
* Ignore PendingIntent to re-open package with null Intent #1546
* Always add FeatureCollection for way points in NavigationMapRoute #1542

### v0.24.0-beta.1 - November 15, 2018

* Prevent from requesting voice instructions if the cache has been previously closed and add a check to delete the cache if there are files [#1540](https://github.com/mapbox/mapbox-navigation-android/pull/1540)
* Add offline functionality [#1539](https://github.com/mapbox/mapbox-navigation-android/pull/1539)
* Remove unnecessary proguard rule resolved in Java services 4.1.0 [#1532](https://github.com/mapbox/mapbox-navigation-android/pull/1532)
* [SEMVER] Fix navigation map route array index out of bounds exception and add clean up navigation launcher preferences support [#1530](https://github.com/mapbox/mapbox-navigation-android/pull/1530)
* [SEMVER] Fix voice instructions cache [#1481](https://github.com/mapbox/mapbox-navigation-android/pull/1481)
* Update Navigator to 3.4.0 [#1525](https://github.com/mapbox/mapbox-navigation-android/pull/1525)
* Cancel outstanding Directions API requests onDestroy NavigationViewModel [#1515](https://github.com/mapbox/mapbox-navigation-android/pull/1515)
* [SEMVER] Use most recent raw Location when building snapped Location [#1522](https://github.com/mapbox/mapbox-navigation-android/pull/1522)
* Update translations to latest Transifex [#1513](https://github.com/mapbox/mapbox-navigation-android/pull/1513)
* Update codecov badge to Navigation 😅 [#1510](https://github.com/mapbox/mapbox-navigation-android/pull/1510)
* Update README.md with codecov badge [#1509](https://github.com/mapbox/mapbox-navigation-android/pull/1509)
* Generate and push code coverage to codecov.io in CI [#1506](https://github.com/mapbox/mapbox-navigation-android/pull/1506)

### v0.23.0 - November 7, 2018

Note: This release breaks `SEMVER` / contains API breaking changes.  Please consult this [migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/0.23.0-Migration-Guide) for the necessary updates required.

* [SEMVER] Add RouteProgressState to RouteProgress for current Navigator information [#1502](https://github.com/mapbox/mapbox-navigation-android/pull/1502)
* [SEMVER] Remove LocationValidator and force last Location if found [#1498](https://github.com/mapbox/mapbox-navigation-android/pull/1498)
* Update MapRouteProgressChangeListener to be aware of route visibility [#1482](https://github.com/mapbox/mapbox-navigation-android/pull/1482)
* [SEMVER] Remove MapboxNavigationOptions ignored by Navigator [#1500](https://github.com/mapbox/mapbox-navigation-android/pull/1500)
* Check for valid geocoding response in ExamplePresenter [#1499](https://github.com/mapbox/mapbox-navigation-android/pull/1499)
* Better clarify the Mapbox Navigator dependency [#1496](https://github.com/mapbox/mapbox-navigation-android/pull/1496)
* Add example test Activity for Navigation Test Application [#1317](https://github.com/mapbox/mapbox-navigation-android/pull/1317)
* Bump mapbox-navigation-native version to 3.3.1 [#1495](https://github.com/mapbox/mapbox-navigation-android/pull/1495)
* Add Mapbox Navigator TOS to the license [#1494](https://github.com/mapbox/mapbox-navigation-android/pull/1494)
* Bump Maps SDK and Events versions [#1493](https://github.com/mapbox/mapbox-navigation-android/pull/1493)
* Resume simulation for multi leg routes [#1490](https://github.com/mapbox/mapbox-navigation-android/pull/1490)
* Remove dynamic padding way name adjustment for MapWayname [#1473](https://github.com/mapbox/mapbox-navigation-android/pull/1473)
* Remove unnecessary force first location update from route (origin) [#1488](https://github.com/mapbox/mapbox-navigation-android/pull/1488)
* Add CameraPosition option for initializing NavigationView [#1483](https://github.com/mapbox/mapbox-navigation-android/pull/1483)
* Rebuild RemoteViews for MapboxNavigationNotification on each update [#1474](https://github.com/mapbox/mapbox-navigation-android/pull/1474)
* Update time remaining TextView to auto-size for longer durations [#1479](https://github.com/mapbox/mapbox-navigation-android/pull/1479)
* Fix mock location generation in ReplayRouteLocationConverter [#1476](https://github.com/mapbox/mapbox-navigation-android/pull/1476)
* Update Maps SDK to 6.6.2 [#1475](https://github.com/mapbox/mapbox-navigation-android/pull/1475)
* Bump mapbox-navigation-native version to 3.2.1 [#1470](https://github.com/mapbox/mapbox-navigation-android/pull/1470)
* [SEMVER] Allow access to AlertView and the ability to enable or disable [#1466](https://github.com/mapbox/mapbox-navigation-android/pull/1466)
* Update Transifex to latest German translations [#1476](https://github.com/mapbox/mapbox-navigation-android/pull/1476)

### v0.22.0 - October 24, 2018

Note: This release breaks `SEMVER` / contains API breaking changes.  Please consult this [migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/0.22.0-Migration-Guide) for the necessary updates required.

* [SEMVER] Replace LocationLayerPlugin with LocationComponent [#1438](https://github.com/mapbox/mapbox-navigation-android/pull/1438)
* Makes gradle.properties license listing consisting with repo [#1456](https://github.com/mapbox/mapbox-navigation-android/pull/1456)
* Rebuild MapboxNavigationNotification for each update [#1455](https://github.com/mapbox/mapbox-navigation-android/pull/1455)
* Update progruard rules to ensure Navigator is kept in release builds [#1454](https://github.com/mapbox/mapbox-navigation-android/pull/1454)
* Adjust InstructionView primary and secondary loading logic [#1451](https://github.com/mapbox/mapbox-navigation-android/pull/1451)
* Correct iconSize expression [#1453](https://github.com/mapbox/mapbox-navigation-android/pull/1453)
* Update InstructionView to consume turn lane data from sub BannerText [#1440](https://github.com/mapbox/mapbox-navigation-android/pull/1440)
* Fix MapWayname ProgressChangeListener leak [#1442](https://github.com/mapbox/mapbox-navigation-android/pull/1442)
* Add base HistoryActivity for testing [#1447](https://github.com/mapbox/mapbox-navigation-android/pull/1447)
* Fix sub BannerText loading shields for first time [#1446](https://github.com/mapbox/mapbox-navigation-android/pull/1446)
* make use of fixed duration for non tracking statuses [#1437](https://github.com/mapbox/mapbox-navigation-android/pull/1437)
* Check for valid index before updating steps in NavigationRouteProcessor [#1435](https://github.com/mapbox/mapbox-navigation-android/pull/1435)
* Update UrlDensityMap for more screen densities [#1436](https://github.com/mapbox/mapbox-navigation-android/pull/1436)
* Update Transifex latest translations [#1433](https://github.com/mapbox/mapbox-navigation-android/pull/1433)
* Update Maps SDK 6.6.1 and Events 3.4.0 [#1434](https://github.com/mapbox/mapbox-navigation-android/pull/1434)
* Stop scrolling before notifying InstructionListAdapter [#1432](https://github.com/mapbox/mapbox-navigation-android/pull/1432)
* Add FusedLocationEngine to Test App [#1373](https://github.com/mapbox/mapbox-navigation-android/pull/1373)
* Check for null maneuver type in ManeuverView [#1429](https://github.com/mapbox/mapbox-navigation-android/pull/1429)
* Revert Shield URL density additions [#1428](https://github.com/mapbox/mapbox-navigation-android/pull/1428)
* Add no value checks for FixLocation in MapboxNavigator [#1426](https://github.com/mapbox/mapbox-navigation-android/pull/1426)
* Show turn lanes when sub BannerText is not present [#1427](https://github.com/mapbox/mapbox-navigation-android/pull/14257)
* Log TimeFormatter error in place of IllegalArgumentException [#1425](https://github.com/mapbox/mapbox-navigation-android/pull/1425)
* Filter queried Features for map way name [#1156](https://github.com/mapbox/mapbox-navigation-android/pull/1156)
* Add tunnels functionality [#1392](https://github.com/mapbox/mapbox-navigation-android/pull/1392)
* Load sub-banner Shield Images [#1418](https://github.com/mapbox/mapbox-navigation-android/pull/1418)
* Fix shields not showing up for some display densities [#1414](https://github.com/mapbox/mapbox-navigation-android/pull/1414)
* Bump Android Gradle plugin version to 3.2.1 [#1415](https://github.com/mapbox/mapbox-navigation-android/pull/1415)
* [SEMVER] Added nav-native ETAs [#1412](https://github.com/mapbox/mapbox-navigation-android/pull/1412)
* Delay start navigation until route received in MockNavigationActivity [#1411](https://github.com/mapbox/mapbox-navigation-android/pull/1411)
* Update Maps v6.6.0 release [#1310](https://github.com/mapbox/mapbox-navigation-android/pull/1310)
* [SEMVER] Consume Sub BannerText in InstructionView [#1408](https://github.com/mapbox/mapbox-navigation-android/pull/1408)
* Fix component navigation activity camera issues [#1407](https://github.com/mapbox/mapbox-navigation-android/pull/1407)

### v0.21.0 - October 5, 2018

Note: This release breaks `SEMVER` / contains API breaking changes.

* Add CustomNavigationNotification notification channel [#1388](https://github.com/mapbox/mapbox-navigation-android/pull/1388)
* [SEMVER] Add OnCameraTrackingChangedListener to NavigationMapboxMap [#1386](https://github.com/mapbox/mapbox-navigation-android/pull/1386)
* Bump mapbox-android-plugin-locationlayer version to 0.10.0 [#1382](https://github.com/mapbox/mapbox-navigation-android/pull/1382)
* Camera tracking modes selection with GPS and North as options [#1377](https://github.com/mapbox/mapbox-navigation-android/pull/1377)
* Match min and max pitch values with iOS [#1379](https://github.com/mapbox/mapbox-navigation-android/pull/1379)
* Migrate camera tracking logic to the LocationLayerPlugin [#1372](https://github.com/mapbox/mapbox-navigation-android/pull/1372)
* CP: Use tracking animation multiplier 2x [#1347](https://github.com/mapbox/mapbox-navigation-android/pull/1347)
* Fix multi on click listener NPE in recenter button [#1374](https://github.com/mapbox/mapbox-navigation-android/pull/1374)

### v0.20.0 - September 30, 2018

Note: This release breaks `SEMVER` / contains API breaking changes.

* Update Navigator to 3.1.3 [#1364](https://github.com/mapbox/mapbox-navigation-android/pull/1364)
* Delay adding InstructionView default click listeners until subscribed [#1362](https://github.com/mapbox/mapbox-navigation-android/pull/1362)
* Do not remove OffRouteListeners onArrival [#1363](https://github.com/mapbox/mapbox-navigation-android/pull/1363)

### v0.20.0-beta.1 - September 30, 2018

* Refine InstructionView update APIs [#1355](https://github.com/mapbox/mapbox-navigation-android/pull/1355)
* Puck Gymnastics [#1354](https://github.com/mapbox/mapbox-navigation-android/pull/1354)
* Fix MultiOnClickListener NPE [#1353](https://github.com/mapbox/mapbox-navigation-android/pull/1353)
* [SEMVER] Ignore null locations [#1352](https://github.com/mapbox/mapbox-navigation-android/pull/1352)
* Update nav native to 3.0.1 [#1348](https://github.com/mapbox/mapbox-navigation-android/pull/1348)
* Add separate APIs for updating InstructionView [#1340](https://github.com/mapbox/mapbox-navigation-android/pull/1340)
* Create MultiOnClickListener before View is attached [#1345](https://github.com/mapbox/mapbox-navigation-android/pull/1345)
* Add navigator history functionality [#1342](https://github.com/mapbox/mapbox-navigation-android/pull/1342)
* Navigation native [#1336](https://github.com/mapbox/mapbox-navigation-android/pull/1336)
* Ignore navigation view orientation test [#1331](https://github.com/mapbox/mapbox-navigation-android/pull/1331)
* Add NavigationMapboxMap APIs for showing multiple routes on map [#1320](https://github.com/mapbox/mapbox-navigation-android/pull/1320)
* Add NavigationMapboxMap to set LocationLayer RenderMode [#1319](https://github.com/mapbox/mapbox-navigation-android/pull/1319)
* Update Build Tools 28.0.3 [#1313](https://github.com/mapbox/mapbox-navigation-android/pull/1313)
* Add default roundabout angle value for ManeuverView [#1264](https://github.com/mapbox/mapbox-navigation-android/pull/1264)
* 0.19.0 release README bump [#1306](https://github.com/mapbox/mapbox-navigation-android/pull/1306)
* Bump MAS version to 4.0.0 [#1308](https://github.com/mapbox/mapbox-navigation-android/pull/1308)

### v0.19.0 - September 24, 2018

Note: This release breaks `SEMVER` / contains API breaking changes.

* Fix route requests that include approaches and fix calculate remaining waypoints and waypoint names logic [#1303](https://github.com/mapbox/mapbox-navigation-android/pull/1303)
* Added ability to hide/add listeners to buttons/alert views [#1251](https://github.com/mapbox/mapbox-navigation-android/pull/1251)
* [SEMVER] Allow custom LocationEngine from NavigationViewOptions [#1257](https://github.com/mapbox/mapbox-navigation-android/pull/1257)
* Add debug logging support back [#1298](https://github.com/mapbox/mapbox-navigation-android/pull/1298)
* Make last location from replay route location engine null (by default) until the first location is received or assigned explicitly [#1296](https://github.com/mapbox/mapbox-navigation-android/pull/1296)
* [SEMVER] Do not update MockLocationEngine route on rotation [#1289](https://github.com/mapbox/mapbox-navigation-android/pull/1289)
* Fix mock navigation activity leak and fix location puck flying from current location to mock [#1294](https://github.com/mapbox/mapbox-navigation-android/pull/1294)
* [SEMVER] Add location dispatcher to replay raw GPS traces [#1039](https://github.com/mapbox/mapbox-navigation-android/pull/1089)
* Remove sonarqube integration [#1290](https://github.com/mapbox/mapbox-navigation-android/pull/1290)
* Update Maps SDK to 6.5.0 and Events to 3.2.0 [#1241](https://github.com/mapbox/mapbox-navigation-android/pull/1241)
* [SEMVER] Add onNavigationStopped callback for NavigationNotification [#1283](https://github.com/mapbox/mapbox-navigation-android/pull/1283)
* [SEMVER] Added custom rounding increments for formatting distance [#1231](https://github.com/mapbox/mapbox-navigation-android/pull/1231)
* [SEMVER] Simplify will voice api [#1281](https://github.com/mapbox/mapbox-navigation-android/pull/1281)
* Update RouteEngine for NavigationView duplicate starts [#1277](https://github.com/mapbox/mapbox-navigation-android/pull/1277)
* Fix recenter problem with Remove onMoveListener in onDestroy [#1263](https://github.com/mapbox/mapbox-navigation-android/pull/1263)
* Update Milestone javadoc to explain ignored trigger behavior [#1269](https://github.com/mapbox/mapbox-navigation-android/pull/1269)
* Fix route requests that include waypoint names [#1260](https://github.com/mapbox/mapbox-navigation-android/pull/1260)
* Do not allow multiple ViewModel subscriptions in NavigationView [#1275](https://github.com/mapbox/mapbox-navigation-android/pull/1275)
* Update Transfiex Translations [#1258](https://github.com/mapbox/mapbox-navigation-android/pull/1258)
* Do not add Fragments to backstack in FragmentNavigationActivity [#1256](https://github.com/mapbox/mapbox-navigation-android/pull/1256)
* bump location layer plugin version to 0.8.1 [#1252](https://github.com/mapbox/mapbox-navigation-android/pull/1252)
* fix route leg progress current leg annotation javadoc broken link [#1250](https://github.com/mapbox/mapbox-navigation-android/pull/1250)
* Save and restore map state on rotation for way name and camera tracking [#1215](https://github.com/mapbox/mapbox-navigation-android/pull/1215)
* Add example with MapboxNavigation driving separate UI components [#1219](https://github.com/mapbox/mapbox-navigation-android/pull/1219)
* Update NavigationView to guard against duplicate initializations [#1247](https://github.com/mapbox/mapbox-navigation-android/pull/1247)
* [SEMVER] Add NavigationViewOption for default or custom SpeechPlayer [#1232](https://github.com/mapbox/mapbox-navigation-android/pull/1232)
* Added Burmese, Finnish, Korean, Norwegian guidance
* Add toggles in NavigationMapboxMap for traffic and incident data [#1226](https://github.com/mapbox/mapbox-navigation-android/pull/1226)
* Update Map styles to V4 with incident coverage [#1234](https://github.com/mapbox/mapbox-navigation-android/pull/1234)
* Add initialization logic for null RouteOptions [#1229](https://github.com/mapbox/mapbox-navigation-android/pull/1229)
* add open pending intent which brings the existing task (activity) to the foreground when clicking the notification [#1221](https://github.com/mapbox/mapbox-navigation-android/pull/1221)

### v0.18.0 - August 24, 2018

* Add toggles in NavigationMapboxMap for traffic and incident data [#1226](https://github.com/mapbox/mapbox-navigation-android/pull/1226)
* Update Map styles to V4 with incident coverage [#1234](https://github.com/mapbox/mapbox-navigation-android/pull/1234)
* Add initialization logic for null RouteOptions [#1229](https://github.com/mapbox/mapbox-navigation-android/pull/1229)
* Reopen the app when when clicking the whole notification [#1221](https://github.com/mapbox/mapbox-navigation-android/pull/1221)
* Update Maps SDK to 6.4.0 and Events to 3.1.5 [#1220](https://github.com/mapbox/mapbox-navigation-android/pull/1220)
* Do not scroll Instruction RecyclerView while animating [#1214](https://github.com/mapbox/mapbox-navigation-android/pull/1214)
* Only reset night mode for EmbeddedNavigationActivity when isFinishing [#1213](https://github.com/mapbox/mapbox-navigation-android/pull/1213)
* Add Boolean in MapboxSpeechPlayer to prevent IllegalStateException [#1212](https://github.com/mapbox/mapbox-navigation-android/pull/1212)

### v0.17.0 - August 10, 2018

Note: This release breaks `SEMVER` / contains API breaking changes.

* Bump MAS version to 3.4.1 [#1203](https://github.com/mapbox/mapbox-navigation-android/pull/1203)
* Notify InstructionListAdapter after animation finishes [#1143](https://github.com/mapbox/mapbox-navigation-android/pull/1143)
* Revert MAS version from 3.4.0 to 3.3.0 [#1200](https://github.com/mapbox/mapbox-navigation-android/pull/1200)
* Update Java SDK to 3.4.0 [#1196](https://github.com/mapbox/mapbox-navigation-android/pull/1196)
* [SEMVER] Allow access to NavigationMapboxMap and MapboxNavigation [#1179](https://github.com/mapbox/mapbox-navigation-android/pull/1179)
* Retrieve feedback Strings from Resources [#1194](https://github.com/mapbox/mapbox-navigation-android/pull/1194)
* Update README Snapshot [#1186](https://github.com/mapbox/mapbox-navigation-android/pull/1186)
* Add gradle-versions-plugin to the project [#1187](https://github.com/mapbox/mapbox-navigation-android/pull/1187)
* Add a null check to prevent NPE in NavigationViewModel onDestroy [#1192](https://github.com/mapbox/mapbox-navigation-android/pull/1192)
* [SEMVER] Remove Location filter and check Location#getAccuracy [#1157](https://github.com/mapbox/mapbox-navigation-android/pull/1157)
* Provide example of showing and hiding Fragment with NavigationView [#1113](https://github.com/mapbox/mapbox-navigation-android/pull/1113)
* Added InstanceState to simplify saving the state [#1162](https://github.com/mapbox/mapbox-navigation-android/pull/1162)
* Fix OffRoute engine cleared before service shutdown [#1167](https://github.com/mapbox/mapbox-navigation-android/pull/1167)
* Transifex Updates [#1145](https://github.com/mapbox/mapbox-navigation-android/pull/1145)
* Fix SpeechAnnouncementListener example and add tests [#1166](https://github.com/mapbox/mapbox-navigation-android/pull/1166)
* Update dependencies LLP, ConstraintLayout [#1172](https://github.com/mapbox/mapbox-navigation-android/pull/1172)
* Consolidate InstructionView DistanceFormatters [#1174](https://github.com/mapbox/mapbox-navigation-android/pull/1174)
* Add ETA support for the notification back [#1184](https://github.com/mapbox/mapbox-navigation-android/pull/1184)
* Fix exception when adding routes in NavigationMapRoute [#1150](https://github.com/mapbox/mapbox-navigation-android/pull/1150)
* Check dispatcher on announcement and instruction events in ViewModel [#1152](https://github.com/mapbox/mapbox-navigation-android/pull/1152)
* Update LeakCanary to 1.6.1 [#1181](https://github.com/mapbox/mapbox-navigation-android/pull/1181)
* Re-initialize arrow sources in NavigationMapRoute after style loaded [#1180](https://github.com/mapbox/mapbox-navigation-android/pull/1180)
* Use application Context for CustomNavigationNotification example [#1182](https://github.com/mapbox/mapbox-navigation-android/pull/1182)
* Update README.md with UI build.gradle instructions [#1148](https://github.com/mapbox/mapbox-navigation-android/pull/1148)
* Add maneuver view roundabout angle bottom limit [#1144](https://github.com/mapbox/mapbox-navigation-android/pull/1144)
* Use roundabout degrees for "then" step in InstructionView [#1141](https://github.com/mapbox/mapbox-navigation-android/pull/1141)
* Remove navigation listeners before clearing NavigationEngineFactory [#1140](https://github.com/mapbox/mapbox-navigation-android/pull/1140)
* Prevent to use the map route until the map is ready and the route fetched [#1134](https://github.com/mapbox/mapbox-navigation-android/pull/1134)

### v0.16.0 - July 20, 2018

Note: This release breaks `SEMVER` / contains API breaking changes.

* Fixed error by only accepting SUCCESS state [#1127](https://github.com/mapbox/mapbox-navigation-android/pull/1127)
* Make navigation map null when shutting navigation view down [#1125](https://github.com/mapbox/mapbox-navigation-android/pull/1125)
* Update Maps SDK 6.3.0 and Telem 3.1.4 [#1124](https://github.com/mapbox/mapbox-navigation-android/pull/1124)
* Fix IllegalArgumentException when updating InstructionList [#1123](https://github.com/mapbox/mapbox-navigation-android/pull/1123)
* [SEMVER] Update MapboxNavigationNotification to consider 24 hour time formatting [#1115](https://github.com/mapbox/mapbox-navigation-android/pull/1123)
* Prevent route overview animation with insufficient route data [#1120](https://github.com/mapbox/mapbox-navigation-android/pull/1120)
* Prevent NavigationNotification update after unregistered [#1118](https://github.com/mapbox/mapbox-navigation-android/pull/1118)
* Refactor InstructionListAdapter and limit roundabout degrees in ManeuverView [#1064](https://github.com/mapbox/mapbox-navigation-android/pull/1064)
* Improve catching low point amounts [#1122](https://github.com/mapbox/mapbox-navigation-android/pull/1122)
* Simplify find current banner and voice instructions algorithms [#1117](https://github.com/mapbox/mapbox-navigation-android/pull/1117)
* Update TimeFormatter to include localized Strings [#1106](https://github.com/mapbox/mapbox-navigation-android/pull/1106)
* Add InstructionListener for intercepting Voice / Banner Instructions [#1107](https://github.com/mapbox/mapbox-navigation-android/pull/1107)
* NavigationService refactor and tests [#1066](https://github.com/mapbox/mapbox-navigation-android/pull/1066)
* Add dual navigation map example to the test app [#1092](https://github.com/mapbox/mapbox-navigation-android/pull/1092)
* Update LocationLayerPlugin 0.6.0 [#1102](https://github.com/mapbox/mapbox-navigation-android/pull/1102)
* Fix navigation camera on start null pointer exception [#1094](https://github.com/mapbox/mapbox-navigation-android/pull/1094)
* Fix navigation map route index out of bounds exception [#1093](https://github.com/mapbox/mapbox-navigation-android/pull/1093)
* Ignore arrival checks after route has finished [#1070](https://github.com/mapbox/mapbox-navigation-android/pull/1070)
* Added InstructionView list state to saveInstanceState [#1079](https://github.com/mapbox/mapbox-navigation-android/pull/1079)
* Update Transifex translations [#1088](https://github.com/mapbox/mapbox-navigation-android/pull/1088)
* Rename MapView id included in NavigationView [#1087](https://github.com/mapbox/mapbox-navigation-android/pull/1087)
* Update Transifex translations [#1078](https://github.com/mapbox/mapbox-navigation-android/pull/1078)
* Update navigation view activity (from the test app) naming [#1076](https://github.com/mapbox/mapbox-navigation-android/pull/1076)
* Add end navigation functionality to navigation view [#959](https://github.com/mapbox/mapbox-navigation-android/pull/959)
* Fix voiceLanguage NPE and add tests for NavigationSpeechPlayer [#1054](https://github.com/mapbox/mapbox-navigation-android/pull/1054)
* Fix vector drawables for < API 21 in test Application [#1067](https://github.com/mapbox/mapbox-navigation-android/pull/1067)
* Re-did the navigation notification layout [#1059](https://github.com/mapbox/mapbox-navigation-android/pull/1059)
* Setup AppCompatDelegate night mode to automatic [#1063](https://github.com/mapbox/mapbox-navigation-android/pull/1063)
* Fix upcoming maneuver arrow underneath road labels [#1053](https://github.com/mapbox/mapbox-navigation-android/pull/1053)

### v0.15.0 - June 21, 2018

* Use theme attribute to update MapView map style URL [#1018](https://github.com/mapbox/mapbox-navigation-android/pull/1018)
* Remove setting voiceLanguage / voice unitType in RouteFetcher [#1046](https://github.com/mapbox/mapbox-navigation-android/pull/1046)
* Add distance remaining buffer to show first instruction immediately [#1043](https://github.com/mapbox/mapbox-navigation-android/pull/1043)
* Revert maps SDK version to 6.1.3 [#1044](https://github.com/mapbox/mapbox-navigation-android/pull/1044)
* Update Maps SDK to 6.2.0 [#1042](https://github.com/mapbox/mapbox-navigation-android/pull/1042)
* Update to MAS 3.3.0 and add approaches / waypointNames to NavigationRoute [#996](https://github.com/mapbox/mapbox-navigation-android/pull/996)
* Fix upcoming arrow is drawn over annotations [#1041](https://github.com/mapbox/mapbox-navigation-android/pull/1041)
* Added error logging for API voice errors [#1036](https://github.com/mapbox/mapbox-navigation-android/pull/1036)
* Removed AndroidSpeechPlayer.UtteranceProgressListener and replaced wi… [#1017](https://github.com/mapbox/mapbox-navigation-android/pull/1017)
* Added check for whether a language is supported by API voice before d… [#1004](https://github.com/mapbox/mapbox-navigation-android/pull/1004)
* Updated NavigationRoute.language to take a Locale [#1025](https://github.com/mapbox/mapbox-navigation-android/pull/1025)
* Add route overview button and animation to NavigationView [#967](https://github.com/mapbox/mapbox-navigation-android/pull/967)
* NavigationViewEventDispatcher remove navigation listeners in onDestroy [#1013](https://github.com/mapbox/mapbox-navigation-android/pull/1013)
* Fixed issue where map still had focus when instruction list was visib… [#1014](https://github.com/mapbox/mapbox-navigation-android/pull/1014)
* Remove origin, destination, unit type and locale from nav options [#965](https://github.com/mapbox/mapbox-navigation-android/pull/965)
* Remove metric arrival event reset for multiple waypoints [#1022](https://github.com/mapbox/mapbox-navigation-android/pull/1022)
* Check for valid name property value in MapWayname [#1031](https://github.com/mapbox/mapbox-navigation-android/pull/1031)
* Update NavigationActivity naming to avoid naming collisions [#1020](https://github.com/mapbox/mapbox-navigation-android/pull/1020)
* Hide way name when camera is not tracking Location [#1027](https://github.com/mapbox/mapbox-navigation-android/pull/1027)
* Add check to remove listener from location engine in NavigationService [#1026](https://github.com/mapbox/mapbox-navigation-android/pull/1026)
* Fixed overlapping of button and compass by wrapping button content [#990](https://github.com/mapbox/mapbox-navigation-android/pull/990)
* Add missing arrival ManeuverViewMap pair [#1007](https://github.com/mapbox/mapbox-navigation-android/pull/1007)
* Remove attempt to place route source when style layers are null [#1006](https://github.com/mapbox/mapbox-navigation-android/pull/1006)
* Update LocationLayerPlugin to 0.5.3, MAS 3.2.0 [#1010](https://github.com/mapbox/mapbox-navigation-android/pull/1010)
* Added extra call to onError for cases where a response is received bu… [#997](https://github.com/mapbox/mapbox-navigation-android/pull/997)
* Added InstructionViewCallback to allow views to be alerted when the in… [#988](https://github.com/mapbox/mapbox-navigation-android/pull/988)
* Update repositories block in build.gradle to have google() as the first entry [#1000](https://github.com/mapbox/mapbox-navigation-android/pull/1000)
* Add wayname underneath navigation puck [#953](https://github.com/mapbox/mapbox-navigation-android/pull/953)
* Add upcoming maneuver arrow on the route line [#934](https://github.com/mapbox/mapbox-navigation-android/pull/934)
* Update InstructionView with BannerMilestone only with callback [#969](https://github.com/mapbox/mapbox-navigation-android/pull/969)
* Added onOffRoute call and removed queue from NavigationInstructionPlayer [#986](https://github.com/mapbox/mapbox-navigation-android/pull/986)
* Example cleanup [#987](https://github.com/mapbox/mapbox-navigation-android/pull/987)
* Check distance remaining before running OffRouteDetector logic [#977](https://github.com/mapbox/mapbox-navigation-android/pull/977)
* Add try catch when obtaining FragmentManager in InstructionView [#973](https://github.com/mapbox/mapbox-navigation-android/pull/973)

### v0.14.0 - May 30, 2018

* Always provide DirectionsRoute in NavigationActivity [#980](https://github.com/mapbox/mapbox-navigation-android/pull/980)
* Update Maps SDK to 6.1.3 and Events library to 3.1.2 [#975](https://github.com/mapbox/mapbox-navigation-android/pull/975)
* Add List of Milestones to NavigationViewOptions [#974](https://github.com/mapbox/mapbox-navigation-android/pull/974)
* Remove origin, destination, unit type and locale from nav options [#965](https://github.com/mapbox/mapbox-navigation-android/pull/965)
* Update Maps SDK 6.1.2 [#962](https://github.com/mapbox/mapbox-navigation-android/pull/962)
* Disable debug logging for Telemetry and Image loading [#961](https://github.com/mapbox/mapbox-navigation-android/pull/961)
* Reset EventDispatcher in onDestroy [#954](https://github.com/mapbox/mapbox-navigation-android/pull/954)
* Fix link in CONTRIBUTING.md [#952](https://github.com/mapbox/mapbox-navigation-android/pull/952)
* Fix navigation guidance styles keys [#948](https://github.com/mapbox/mapbox-navigation-android/pull/948)
* Resume navigation state based on MapboxNavigation running [#946](https://github.com/mapbox/mapbox-navigation-android/pull/946)
* Initialize ViewModel EventDispatcher in NavigationView constructor [#945](https://github.com/mapbox/mapbox-navigation-android/pull/945)
* Add NavigationHelper check for valid step points using lineSlice [#944](https://github.com/mapbox/mapbox-navigation-android/pull/944)
* Use last BannerInstruction for arrival event [#943](https://github.com/mapbox/mapbox-navigation-android/pull/943)
* Downgrade min sdk version to 14 [#942](https://github.com/mapbox/mapbox-navigation-android/pull/942)
* Remove onStyleLoaded Callback in NavigationView [#939](https://github.com/mapbox/mapbox-navigation-android/pull/939)
* Update ConstraintLayout, Support Lib, and LocationLayerPlugin dependencies [#938](https://github.com/mapbox/mapbox-navigation-android/pull/938)
* Update translation Strings and add Burmese [#937](https://github.com/mapbox/mapbox-navigation-android/pull/937)
* Check for valid BannerInstructions when milestones are enabled [#936](https://github.com/mapbox/mapbox-navigation-android/pull/936)
* Added null check to make sure a file is actually returned [#925](https://github.com/mapbox/mapbox-navigation-android/pull/925)
* Update to Guidance V3 Map Styles [#917](https://github.com/mapbox/mapbox-navigation-android/pull/917)
* Check NavigationOptions in NavigationService [#916](https://github.com/mapbox/mapbox-navigation-android/pull/916)
* Remove AWS Polly dependency not needed anymore [#914](https://github.com/mapbox/mapbox-navigation-android/pull/914)
* Update localization.md [#913](https://github.com/mapbox/mapbox-navigation-android/pull/913)
* Dynamic abbreviations in banner instructions [#887](https://github.com/mapbox/mapbox-navigation-android/pull/887)

### v0.13.0 - May 2, 2018

* Add missing uturn step maneuver modifier to should flip modifiers set [#908](https://github.com/mapbox/mapbox-navigation-android/pull/909)
* Bump Mapbox SDK Services to 3.1.0 version [#907](https://github.com/mapbox/mapbox-navigation-android/pull/907)
* Reverse maneuver sharp left resource [#905](https://github.com/mapbox/mapbox-navigation-android/pull/905)
* Fixed bug where we weren't checking if the unit type changed before l… [#896](https://github.com/mapbox/mapbox-navigation-android/pull/896)
* Remove use of LiveData for fetching DirectionsRoute and updating Location [#894](https://github.com/mapbox/mapbox-navigation-android/pull/894)
* Update String resources with new Transifex translations [#889](https://github.com/mapbox/mapbox-navigation-android/pull/889)
* Cancel delayed transitions when InstructionView is detached [#885](https://github.com/mapbox/mapbox-navigation-android/pull/885)
* Remove app name String resource from libandroid-navigation [#884](https://github.com/mapbox/mapbox-navigation-android/pull/884)
* Update localization.md [#881](https://github.com/mapbox/mapbox-navigation-android/pull/881)
* Fix Feedback FAB margins [#878](https://github.com/mapbox/mapbox-navigation-android/pull/878)
* Update new Transifex translation Strings [#870](https://github.com/mapbox/mapbox-navigation-android/pull/870)
* Check for null camera engine before returning from MapboxNavigation [#866](https://github.com/mapbox/mapbox-navigation-android/pull/866)
* Update Maps SDK 5.5.2 [#865](https://github.com/mapbox/mapbox-navigation-android/pull/865)
* Added null check for race condition when deleting instruction files [#860](https://github.com/mapbox/mapbox-navigation-android/pull/860)
* Add null start timestamp check for metric events [#857](https://github.com/mapbox/mapbox-navigation-android/pull/857)
* Add NavigationLauncherOption snap-to-route enabled [#856](https://github.com/mapbox/mapbox-navigation-android/pull/856)
* Use BannerText roundabout degrees + Banner and Voice Milestone Tests [#854](https://github.com/mapbox/mapbox-navigation-android/pull/854)
* Added null checks in case the user isn't using voice instructions [#852](https://github.com/mapbox/mapbox-navigation-android/pull/852)
* Add gradle-dependency-graph-generator-plugin to the project [#850](https://github.com/mapbox/mapbox-navigation-android/pull/850)
* Remove draw routes and add direction waypoints calls already being called [#849](https://github.com/mapbox/mapbox-navigation-android/pull/849)
* Add AutoValue Proguard rule [#838](https://github.com/mapbox/mapbox-navigation-android/pull/838)
* Validate route profile passed into RouteViewModel [#829](https://github.com/mapbox/mapbox-navigation-android/pull/829)
* Remove NavigationCamera ProgressChangeListener as public api [#828](https://github.com/mapbox/mapbox-navigation-android/pull/828)
* Upgrade RouteProgress Step Data [#812](https://github.com/mapbox/mapbox-navigation-android/pull/812)
* Integrate API Voice [#751](https://github.com/mapbox/mapbox-navigation-android/pull/751)

### v0.12.0 - April 3, 2018

* Add MapboxNavigationOptions to adjust location validation thresholds [#818](https://github.com/mapbox/mapbox-navigation-android/pull/818)
* Set default driving profile and check for empty profile [#816](https://github.com/mapbox/mapbox-navigation-android/pull/816)
* Update to MAS 3.0.1 [#815](https://github.com/mapbox/mapbox-navigation-android/pull/815)
* Added safety for NONE_SPECIFIED type for voice units [#811](https://github.com/mapbox/mapbox-navigation-android/pull/811)
* Add stick to chosen route when re-routing with UI functionality [#808](https://github.com/mapbox/mapbox-navigation-android/pull/808)
* Remove NavigationView lifecycle observer and add Fragment Example [#806](https://github.com/mapbox/mapbox-navigation-android/pull/806)
* Add 12/24 hour format Navigation View Option [#805](https://github.com/mapbox/mapbox-navigation-android/pull/805)
* Fixed unit type was defaulting to imperial [#804](https://github.com/mapbox/mapbox-navigation-android/pull/804)
* Update ISSUE_TEMPLATE.md [#798](https://github.com/mapbox/mapbox-navigation-android/pull/798)
* Decrease Robo tests time to 5 minutes [#795](https://github.com/mapbox/mapbox-navigation-android/pull/795)
* Send departure event with a valid distance traveled [#789](https://github.com/mapbox/mapbox-navigation-android/pull/789)
* Remove last location check from location validation [#788](https://github.com/mapbox/mapbox-navigation-android/pull/788)
* Add localization instructions [#785](https://github.com/mapbox/mapbox-navigation-android/pull/785)
* Extract NavigationEngine processing logic and add tests [#784](https://github.com/mapbox/mapbox-navigation-android/pull/784)
* Fix OffRoute detection disabled bug [#783](https://github.com/mapbox/mapbox-navigation-android/pull/783)
* Create separate options for Launcher and View [#782](https://github.com/mapbox/mapbox-navigation-android/pull/782)
* Create LocationValidator for checking new location updates [#690](https://github.com/mapbox/mapbox-navigation-android/pull/690)

### v0.11.1 - March 16, 2018

* Adjust sound layout margin [#775](https://github.com/mapbox/mapbox-navigation-android/pull/775)
* Fix distancesFromManeuver not being cleared [#773](https://github.com/mapbox/mapbox-navigation-android/pull/773)
* Allow setting of custom destination marker in theme [#763](https://github.com/mapbox/mapbox-navigation-android/pull/763)
* Fixed back button in NavigationViewActivity [#768](https://github.com/mapbox/mapbox-navigation-android/pull/768)
* Fixed unit type bug [#769](https://github.com/mapbox/mapbox-navigation-android/pull/769)
* Fix MapRoute listener not firing for index 0 [#772](https://github.com/mapbox/mapbox-navigation-android/pull//772)
* Stop scroll of InstructionList before hiding [#766](https://github.com/mapbox/mapbox-navigation-android/pull/766)
* Add baseUrl to NavigationRoute and Turf conversion [#767](https://github.com/mapbox/mapbox-navigation-android/pull/767)
* Force first location update without last location [#756](https://github.com/mapbox/mapbox-navigation-android/pull/756)
* Update EventListener Thread Safe [#762](https://github.com/mapbox/mapbox-navigation-android/pull/762)
* Create camera animation from time between updates [#753](https://github.com/mapbox/mapbox-navigation-android/pull/753)
* NavigationView Landscape Optimizations [#749](https://github.com/mapbox/mapbox-navigation-android/pull/749)

### v0.11.0 - March 7, 2018

* Fix same point being added twice for LatLngBounds [#741](https://github.com/mapbox/mapbox-navigation-android/pull/741)
* Fix Recent Distances from maneuver Off-Route [#739](https://github.com/mapbox/mapbox-navigation-android/pull/739)
* Update MAS Dependencies [#738](https://github.com/mapbox/mapbox-navigation-android/pull/738)
* Update LocationLayerPlugin to 0.4.0 [#734](https://github.com/mapbox/mapbox-navigation-android/pull/734)
* Fix visibility bug with TurnLanes [#733](https://github.com/mapbox/mapbox-navigation-android/pull/733)
* Update Maps SDK 5.5.0 [#732](https://github.com/mapbox/mapbox-navigation-android/pull/732)
* Show first BannerInstruction immediately [#731](https://github.com/mapbox/mapbox-navigation-android/pull/731)
* Fix initialization of directions list [#728](https://github.com/mapbox/mapbox-navigation-android/pull/728)
* Default Dynamic Camera for Navigation UI [#679](https://github.com/mapbox/mapbox-navigation-android/pull/679)

### v0.10.0 - February 26, 2018

* Fix NPE with MapRoute click listener [#721](https://github.com/mapbox/mapbox-navigation-android/pull/721)
* Null check camera tracking [#719](https://github.com/mapbox/mapbox-navigation-android/pull/719)
* Initialize metric session state in constructor [#718](https://github.com/mapbox/mapbox-navigation-android/pull/718)

### v0.10.0-beta.1 - February 16, 2018

* Clear features so DirectionsRoute isn't redrawn when new style loads [#706](https://github.com/mapbox/mapbox-navigation-android/pull/706)
* Fix bug with MapRoute onClick [#703](https://github.com/mapbox/mapbox-navigation-android/pull/703)
* Fix flashing InstructionView list during re-routes [#700](https://github.com/mapbox/mapbox-navigation-android/pull/700)
* Fix FeedbackBottomSheet rotation bug [#699](https://github.com/mapbox/mapbox-navigation-android/pull/699)
* Check Turn / Then Banner on each update [#696](https://github.com/mapbox/mapbox-navigation-android/pull/696)
* Instructions based on locale [#691](https://github.com/mapbox/mapbox-navigation-android/pull/691)
* Cancel animation if AlertView detaches while running [#689](https://github.com/mapbox/mapbox-navigation-android/pull/689)
* Add bearing to RouteEngine requests [#687](https://github.com/mapbox/mapbox-navigation-android/pull/687)
* LocationViewModel obtain best LocationEngine [#685](https://github.com/mapbox/mapbox-navigation-android/pull/685)
* Dependencies Bump [#684](https://github.com/mapbox/mapbox-navigation-android/pull/684)
* Fix issue with startup in Night Mode [#683](https://github.com/mapbox/mapbox-navigation-android/pull/683)
* Cache route options / calculate remaining waypoints [#680](https://github.com/mapbox/mapbox-navigation-android/pull/680)
* Switched setOnMapClickListener() to addOnMapClickListener() [#672](https://github.com/mapbox/mapbox-navigation-android/pull/672)
* Locale distance formatter [#668](https://github.com/mapbox/mapbox-navigation-android/pull/668)
* Off-Route Bug Fixes [#667](https://github.com/mapbox/mapbox-navigation-android/pull/667)
* Update Default Zoom Level [#655](https://github.com/mapbox/mapbox-navigation-android/pull/655)

### v0.9.0 - January 23, 2018

* Update Maps and Services dependencies [#661](https://github.com/mapbox/mapbox-navigation-android/pull/661)
* Add Maneuver type exit rotary constant [#653](https://github.com/mapbox/mapbox-navigation-android/pull/653)
* Moved WaypointNavigationActivity from the SDK to the test app [#652](https://github.com/mapbox/mapbox-navigation-android/pull/652)
* NavigationTelemetry update cue for changing configurations [#648](https://github.com/mapbox/mapbox-navigation-android/pull/648)
* Remove duplicate ViewModel updates [#647](https://github.com/mapbox/mapbox-navigation-android/pull/647)
* Track initialization of NavigationView [#646](https://github.com/mapbox/mapbox-navigation-android/pull/646)
* Update Maps SDK to 5.3.1 [#645](https://github.com/mapbox/mapbox-navigation-android/pull/645)
* Check for null directions route or geometry in SessionState [#643](https://github.com/mapbox/mapbox-navigation-android/pull/643)
* Remove NavigationViewModel as lifecycle observer [#643](https://github.com/mapbox/mapbox-navigation-android/pull/643)
* Exposes the MapboxMap in NavigationView with a getter method [#642](https://github.com/mapbox/mapbox-navigation-android/pull/642)
* Package delivery/ride sharing waypoint demo [#641](https://github.com/mapbox/mapbox-navigation-android/pull/641)
* Removed boolean that was preventing subsequent navigation sessions [#640](https://github.com/mapbox/mapbox-navigation-android/pull/640)
* Add FasterRouteDetector to check for quicker routes while navigating [#638](https://github.com/mapbox/mapbox-navigation-android/pull/638)
* Notification check for valid BannerInstructions before updating [#637](https://github.com/mapbox/mapbox-navigation-android/pull/637)
* Check for at least two coordinates when creating snapped location [#636](https://github.com/mapbox/mapbox-navigation-android/pull/636)
* Add language to NavigationViewOptions with default from RouteOptions [#635](https://github.com/mapbox/mapbox-navigation-android/pull/635)
* Add onDestroy as a method that must be implemented for NavigationView [#632](https://github.com/mapbox/mapbox-navigation-android/pull/632)
* Check for network connection before setting off-route [#631](https://github.com/mapbox/mapbox-navigation-android/pull/631)
* Add NavigationView style attribute for custom LocationLayer [#627](https://github.com/mapbox/mapbox-navigation-android/pull/627)
* Replace setOnScroll (now deprecated) with addOnScroll [#626](https://github.com/mapbox/mapbox-navigation-android/pull/626)
* Check for IndexOutOfBounds when calculating foreground percentage  [#625](https://github.com/mapbox/mapbox-navigation-android/pull/625)
* Fix for listener bug [#620](https://github.com/mapbox/mapbox-navigation-android/pull/620)

### v0.8.0 - December 20, 2017

* Update Maps SDK to 5.3.0 [#617](https://github.com/mapbox/mapbox-navigation-android/pull/617)
* Expose listeners in the NavigationView [#614](https://github.com/mapbox/mapbox-navigation-android/pull/614)
* Null check light / dark theme from NavigationLauncher [#613](https://github.com/mapbox/mapbox-navigation-android/pull/613)
* Add SSML parameter to Polly request [#612](https://github.com/mapbox/mapbox-navigation-android/pull/612)

### v0.8.0-beta.1 - December 15, 2017

* Allow theme setting from NavigationViewOptions [#595](https://github.com/mapbox/mapbox-navigation-android/pull/595)
* Fix issue NavigationView simulation [#594](https://github.com/mapbox/mapbox-navigation-android/pull/594)
* Remove preference setup for unit type in RouteViewModel [#593](https://github.com/mapbox/mapbox-navigation-android/pull/593)
* Create other map issue in feedback adapter [#592](https://github.com/mapbox/mapbox-navigation-android/pull/592)
* Remove specified layer for map route [#590](https://github.com/mapbox/mapbox-navigation-android/pull/590)
* Guard against IndexOutOfBounds when updating last reroute event [#589](https://github.com/mapbox/mapbox-navigation-android/pull/589)
* Set original and current request identifier [#585](https://github.com/mapbox/mapbox-navigation-android/pull/585)
* Add SSML announcement option for VoiceInstructionMilestone [#584](https://github.com/mapbox/mapbox-navigation-android/pull/584)
* Remove duplicate subscriptions to the ViewModels  [#583](https://github.com/mapbox/mapbox-navigation-android/pull/583)
* Return Milestone instead of identifier  [#579](https://github.com/mapbox/mapbox-navigation-android/pull/579)
* DirectionsProfile for reroutes in NavigationView [#575](https://github.com/mapbox/mapbox-navigation-android/pull/575)
* Add custom notification support  [#564](https://github.com/mapbox/mapbox-navigation-android/pull/564)

### v0.7.1 - December 6, 2017

Note: This release breaks `SEMVER` / contains API breaking changes.

* Fix NPE with reroute metric events [#565](https://github.com/mapbox/mapbox-navigation-android/pull/565)
* Adjust metric listener reset [#566](https://github.com/mapbox/mapbox-navigation-android/pull/566)
* Update distance completed in off-route scenario [#568](https://github.com/mapbox/mapbox-navigation-android/pull/568)
* Update Maps SDK to `5.2.1` [#570](https://github.com/mapbox/mapbox-navigation-android/pull/570)

### v0.7.1-beta.1 - December 1, 2017

* Expanded the width of route lines when zoomed out
* Added support for displaying alternative routes on map
* Adds exclude, voiceUnits, and banner instruction info to request/response [#500](https://github.com/mapbox/mapbox-navigation-android/pull/500)
* [SEMVER] Add Imperial / Metric support for UI & Notification [#501](https://github.com/mapbox/mapbox-navigation-android/pull/501)
* Add NavigationView as a lifecycle observer [#506](https://github.com/mapbox/mapbox-navigation-android/pull/506)
* Add Custom themes via XML for light / dark mode [#507](https://github.com/mapbox/mapbox-navigation-android/pull/507)
* Navigation Metrics Refactor [#511](https://github.com/mapbox/mapbox-navigation-android/pull/511)
* Add software layer type programmatically for Maneuver and Lane View [#514](https://github.com/mapbox/mapbox-navigation-android/pull/514)
* Use NavigationViewOptions in NavigationLauncher [#524](https://github.com/mapbox/mapbox-navigation-android/pull/524)
* Lifecycle aware Navigation Metrics [#540](https://github.com/mapbox/mapbox-navigation-android/pull/540)

### v0.7.0 - November 13, 2017

* Updated to Mapbox Java 3.0 [#373](https://github.com/mapbox/mapbox-navigation-android/pull/373)
* Update InstructionView with secondary TextView [#404](https://github.com/mapbox/mapbox-navigation-android/pull/404)
* Fixed issue with bearing values in route requests [#408](https://github.com/mapbox/mapbox-navigation-android/pull/408)
* Updates and docs for NavigationRoute [#413](https://github.com/mapbox/mapbox-navigation-android/pull/413)
* Fixed native crash with initialization of navigation UI [#423](https://github.com/mapbox/mapbox-navigation-android/pull/423)
* Add validation utils class [#424](https://github.com/mapbox/mapbox-navigation-android/pull/424)
* Cancel notification when service is destroyed [#409](https://github.com/mapbox/mapbox-navigation-android/pull/409)
* Adjust API Milestone to handle new routes [#425](https://github.com/mapbox/mapbox-navigation-android/pull/425)
* Replaced maneuver arrows with custom StyleKit [#362](https://github.com/mapbox/mapbox-navigation-android/pull/362)
* Dynamic reroute tolerance [#428](https://github.com/mapbox/mapbox-navigation-android/pull/428)
* Add Telem location engine class name [#401](https://github.com/mapbox/mapbox-navigation-android/pull/401)
* Fixed snap to route object for snapped location [#434](https://github.com/mapbox/mapbox-navigation-android/pull/434)
* Directions list as dropdown [#415](https://github.com/mapbox/mapbox-navigation-android/pull/415)
* Feedback UI [#383](https://github.com/mapbox/mapbox-navigation-android/pull/383)
* Fixed bearing values not matching number of coordinates [#435](https://github.com/mapbox/mapbox-navigation-android/pull/435)
* Updated to new TurfConversion class [#440](https://github.com/mapbox/mapbox-navigation-android/pull/440)
* Removes duplicate check and adds test for new route [#443](https://github.com/mapbox/mapbox-navigation-android/pull/443)
* Show / hide recenter button when direction list is showing / hiding [#441](https://github.com/mapbox/mapbox-navigation-android/pull/441)
* Current step removed from instruction list [#444](https://github.com/mapbox/mapbox-navigation-android/pull/444)
* Change feedback timing [#442](https://github.com/mapbox/mapbox-navigation-android/pull/442)
* Updated Maneuver Icons [#445](https://github.com/mapbox/mapbox-navigation-android/pull/445)
* Fixed ordering of the bearings [#455](https://github.com/mapbox/mapbox-navigation-android/pull/455)
* "Then" Banner Instruction [#456](https://github.com/mapbox/mapbox-navigation-android/pull/456)
* NavigationQueueContainer Class to manage reroute and feedback queues [#457](https://github.com/mapbox/mapbox-navigation-android/pull/457)
* Update Turn lane Views to use StyleKit [#466](https://github.com/mapbox/mapbox-navigation-android/pull/466)
* Upgraded to Gradle 3.0 [#453](https://github.com/mapbox/mapbox-navigation-android/pull/453)
* Fixed up a few issues preventing all direction routes from working [#469](https://github.com/mapbox/mapbox-navigation-android/pull/469)
* AlertView integrated with post-reroute feedback [#470](https://github.com/mapbox/mapbox-navigation-android/pull/470)
* Fix leak when closing app with bottomsheet showing [#472](https://github.com/mapbox/mapbox-navigation-android/pull/472)
* Added issue template [#418](https://github.com/mapbox/mapbox-navigation-android/pull/418)
* Check for null raw location before setting bearing [#476](https://github.com/mapbox/mapbox-navigation-android/pull/476)
* Update location layer to 0.2.0 and re-add as lifecycle observe [#473](https://github.com/mapbox/mapbox-navigation-android/pull/473)
* Check for null or empty String speechUrl before playing [#475](https://github.com/mapbox/mapbox-navigation-android/pull/475)
* Create SpanUtil and SpanItem to more easily format Strings [#477](https://github.com/mapbox/mapbox-navigation-android/pull/477)
* Initialize click listeners after presenter / viewmodel is set [#481](https://github.com/mapbox/mapbox-navigation-android/pull/481)
* Fix bug with bottomsheet not hiding in night mode [#483](https://github.com/mapbox/mapbox-navigation-android/pull/483)
* Adjust Instruction Content Layout XML [#465](https://github.com/mapbox/mapbox-navigation-android/pull/465)
* Add telem absolute distance to destination track support [#427](https://github.com/mapbox/mapbox-navigation-android/pull/427)
* Fix issue where new route was not being detected [#478](https://github.com/mapbox/mapbox-navigation-android/pull/478)
* Fix bug with bottom sheet behavior null onConfigChange [#490](https://github.com/mapbox/mapbox-navigation-android/pull/490)
* Update lane stylekit and then maneuver bias [#492](https://github.com/mapbox/mapbox-navigation-android/pull/492)
* Add missing javadoc for feedback methods in MapboxNavigation [#493](https://github.com/mapbox/mapbox-navigation-android/pull/493)
* Portrait / landscape instruction layouts are different - only cast to View [#494](https://github.com/mapbox/mapbox-navigation-android/pull/494)

### v0.6.3 -October 18, 2017

* Significant reroute metric fixes [#348](https://github.com/mapbox/mapbox-navigation-android/pull/348)
* Avoid index out of bounds when drawing route line traffic [#384](https://github.com/mapbox/mapbox-navigation-android/pull/384)

### v0.6.2 - October 7, 2017

* Fixed an issue with the Location Engine not being activated correctly inside the Navigation-UI lib [#321](https://github.com/mapbox/mapbox-navigation-android/pull/321)
* Fixed bottom sheet not getting placed correctly when the device is rotated [#320](https://github.com/mapbox/mapbox-navigation-android/pull/320)
* Fixed missing reroute UI when a navigation session reroute occurs [#319](https://github.com/mapbox/mapbox-navigation-android/pull/319)
* Added logic to detect if the user did a u-turn which would require a reroute [#312](https://github.com/mapbox/mapbox-navigation-android/pull/312)
* Revert snap to route logic creating a new Location object which was causing location updates to occasionally get stuck at a maneuver point [#308](https://github.com/mapbox/mapbox-navigation-android/pull/308)
* Restructured the project so the studio projects opened from the root folder rather than having it nested inside the `navigation` folder [#302](https://github.com/mapbox/mapbox-navigation-android/pull/302)
* Notifications fixed for Android Oreo [#298](https://github.com/mapbox/mapbox-navigation-android/pull/298)
* OSRM-text-instructions removed [#288](https://github.com/mapbox/mapbox-navigation-android/pull/288)
* General code cleanup [#287](https://github.com/mapbox/mapbox-navigation-android/pull/287)
* Day and night mode and theme switching functionality added inside the Navigation-UI library [#286](https://github.com/mapbox/mapbox-navigation-android/pull/286)
* Metric reroute added - [#296](https://github.com/mapbox/mapbox-navigation-android/pull/296)

### v0.6.1 - September 28, 2017
* Telemetry Updates

### v0.6.0 - September 21, 2017
* First iteration of the Navigation UI
* Optimized Navigation features which were causing slowdowns on long steps - [219](https://github.com/mapbox/mapbox-navigation-android/pull/219)
* Only decode step geometry when needed - [215](https://github.com/mapbox/mapbox-navigation-android/pull/215)
* Introduced metrics
* Cleaned up code and fixed several bugs

### v0.5.0 - August 30, 2017
* use followonstep inside routeprogress for instruction - [#188](https://github.com/mapbox/mapbox-navigation-android/pull/188)
* Persistent notification [#177](https://github.com/mapbox/mapbox-navigation-android/pull/177)
* Fixes crash occurring ocasionally at end of route - [#175](https://github.com/mapbox/mapbox-navigation-android/pull/175)
* Cleaned up RouteProgress object to use AutoValue builders - [#164](https://github.com/mapbox/mapbox-navigation-android/pull/164)
* Run calculations and cleaned up `MapboxNavigation` class - [#151](https://github.com/mapbox/mapbox-navigation-android/pull/151)

### v0.4.0 - August 1, 2017
* Add new alert level concept called, milestones [#84](https://github.com/mapbox/mapbox-navigation-android/pull/84)
* Multiple way point support added [#76](https://github.com/mapbox/mapbox-navigation-android/pull/76)
* Support for congestion along the route [#106](https://github.com/mapbox/mapbox-navigation-android/pull/106)
* Default Milestones and text instructions [#98](https://github.com/mapbox/mapbox-navigation-android/pull/98) and []()
* Several improvements and bug fixes for snap to route logic [#97](https://github.com/mapbox/mapbox-navigation-android/pull/97)
* Only update routeProgress when the user has a speed greater than 0 [#118](https://github.com/mapbox/mapbox-navigation-android/pull/118)
* Add radius to directions route request [#119](https://github.com/mapbox/mapbox-navigation-android/pull/119)
* Remove RouteUtils class [#127](https://github.com/mapbox/mapbox-navigation-android/pull/127)
* Remove hardcoded constant for seconds till reroute [#121](https://github.com/mapbox/mapbox-navigation-android/pull/121)
* Adds support for creating custom instructions for Milestones [#122](https://github.com/mapbox/mapbox-navigation-android/pull/122)
* RouteProgressChange callback will attempt to get instantly invoked when starting if a locations present [#47](https://github.com/mapbox/mapbox-navigation-android/issues/47)
* Upgrade to MAS 2.2.0 [#153](https://github.com/mapbox/mapbox-navigation-android/pull/153)

### v0.3.1 - June 8, 2017
* Use AutoValue inside RouteProgress objects [#74](https://github.com/mapbox/mapbox-navigation-android/pull/74)
* Directly use direction distance measurements instead of calculating them. [#125](https://github.com/mapbox/mapbox-navigation-android/pull/125)

### v0.3 - June 5, 2017
* Support for [other direction profiles](https://github.com/mapbox/mapbox-navigation-android/pull/63) (cycling and walking) added.
* Fixed [issue with step and leg indexes](https://github.com/mapbox/mapbox-navigation-android/pull/52) not getting restarted when reroute occurred.
* Resolved [issue with second navigation session](https://github.com/mapbox/mapbox-navigation-android/issues/68) not kicking off service again (preventing listeners getting invoked).
* [Added missing MapboxNavigationOptions getter](https://github.com/mapbox/mapbox-navigation-android/pull/62) inside the MapboxNavigation class.

### v0.2 - May 15, 2017

* [`MapboxNavigationOptions`](https://github.com/mapbox/mapbox-navigation-android/blob/master/navigation/libandroid-navigation/src/main/java/com/mapbox/services/android/navigation/v5/MapboxNavigationOptions.java) added allowing for setting navigation variables.
* Fixed issue with Alert Levels not happening at correct timing
* Split `RouteProgress` to [include leg and step progress](https://github.com/mapbox/mapbox-navigation-android/issues/20) classes.
* [Reroute logic refactored.](https://github.com/mapbox/mapbox-navigation-android/pull/30)

### v0.1 - April 20, 2017

* Initial release as a standalone package.
