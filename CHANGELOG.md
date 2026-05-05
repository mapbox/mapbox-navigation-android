## Navigation SDK Core Framework 3.24.0-rc.1 - 05 May, 2026
#### Features

#### Bug fixes and improvements

#### Known issues

#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.24.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.24.0-rc.1))
- Mapbox Navigation Native `v324.24.0-rc.1`
- Mapbox Core Common `v24.24.0-rc.1`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))

# Changelog for the Mapbox Navigation SDK Core Framework for Android

## Navigation SDK Core Framework 3.23.0 - 30 April, 2026

#### Bug fixes and improvements
- Fixed a bug that caused lost zone progress when switching to an alternative during driving in a speed zone. [#13915](https://github.com/mapbox/navigation/pull/13915)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.23.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.23.0))
- Mapbox Navigation Native `v324.23.0`
- Mapbox Core Common `v24.23.0`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))

## Navigation SDK Core Framework 3.23.0-rc.1 - 21 April, 2026
#### Features
- Add `maxVoltage` to `EvseGroup.Connector` [#13360](https://github.com/mapbox/navigation/pull/13360)
- Allow passing `ttsEngineParams` at TTS `PlayerOptions` [#13434](https://github.com/mapbox/navigation/pull/13434)
- Insufficient charge detection can now check all remaining route legs via `EvBetterRouteNotificationOptions.checkInsufficientChargeOnRemainingLegs` [#13453](https://github.com/mapbox/navigation/pull/13453)
- Log request URL in online EV calls [#13527](https://github.com/mapbox/navigation/pull/13527)

#### Bug fixes and improvements
- Fixed a crash that could occur when accessing navigation components after MapboxNavigation has been destroyed. [#13154](https://github.com/mapbox/navigation/pull/13154)
- Fix 2-finger pan gestures to respect gestureThresholds. [#13355](https://github.com/mapbox/navigation/pull/13355)
- Fix location updates burst blocking main thread [#13625](https://github.com/mapbox/navigation/pull/13625)
- Added support for exclude=tunnel in onboard router. [#13630](https://github.com/mapbox/navigation/pull/13630)
- Fix positioning issues after navigator recreation triggered by downloading an offline pack. [#13630](https://github.com/mapbox/navigation/pull/13630)
- Bugfix for the speed zone not being handled properly when the AG has started within the zone. [#13657](https://github.com/mapbox/navigation/pull/13657)
- Add support for routes that go through the same speed zone multiple times. [#13680](https://github.com/mapbox/navigation/pull/13680)
- Fix ASZ reported as passed when there are multiple alternatives with the same camera [#13747](https://github.com/mapbox/navigation/pull/13747)
- fix issue where user input state unexpectedly changes to `Idle` after transcript is finalized [#13756](https://github.com/mapbox/navigation/pull/13756)
- Fix for `multiFingerMoveThreshold` messes up `singleFingerMoveThreshold`. [#13789](https://github.com/mapbox/navigation/pull/13789)

#### Known issues

#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.23.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.23.0-rc.1))
- Mapbox Navigation Native `v324.23.0-rc.1`
- Mapbox Core Common `v24.23.0-rc.1`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))

## Navigation SDK Core Framework 3.22.1 - 17 April, 2026
#### Features

#### Bug fixes and improvements
- Fixed an issue where predictive cache controller was not created for the current style in case `createStyleMapControllers` was invoked after the style had already been loaded. [#11973](https://github.com/mapbox/navigation/pull/11973)
- Fixed an issue where alternative routes stopped being updated after a navigator version switch. [#12927](https://github.com/mapbox/navigation/pull/12927)
- Fixed an issue where the map matcher incorrectly snapped to a tunnel after exiting a parking garage. [#12964](https://github.com/mapbox/navigation/pull/12964)
- Fixed an issue where speed estimation was inaccurate after a long device sleep when input locations lacked speed information. [#12964](https://github.com/mapbox/navigation/pull/12964)
- Fixed an issue where charging time was calculated incorrectly for alternative routes when the current route index was passed. [#12964](https://github.com/mapbox/navigation/pull/12964)
- Fixed an issue where route stickiness was applied to roads outside of the current route leg. [#12964](https://github.com/mapbox/navigation/pull/12964)
- Fixed an issue where alternative route fork point detection was inaccurate by introducing a geometry-based detection algorithm. [#12964](https://github.com/mapbox/navigation/pull/12964)
- Added a minimum speed threshold for applying avoid-maneuver-radius for continuou alternative route, so that Nav SDK can suggest alternatives with maneuver being close in case of low speed. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Improved off-road mode detection in parking aisles that are marked as tunnels. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Improved ADAS cache performance. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Fixed a crash that could occur after navigation shutdown. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Improved performance on routes with a large number of road alerts. [#13358](https://github.com/mapbox/navigation/pull/13358)


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.22.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.22.0))
- Mapbox Navigation Native `v324.22.0`
- Mapbox Core Common `v24.22.0`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))

## Navigation SDK Core Framework 3.21.0 - 02 April, 2026
#### Features
- `GeoUtils`: added `getWayId` overloads to retrieve OSM way id(s) by directed edge id or polyline span [#13449](https://github.com/mapbox/navigation/pull/13449)

#### Bug fixes and improvements
- Fixed an issue where the navigator would cycle between alternative routes after missing a turn at a waypoint, when the current position was already on the second leg of both the primary and alternative routes. [#13542](https://github.com/mapbox/navigation/pull/13542)
- Fixed an issue in `PredictiveCacheController` that caused excessive resource consumption when the same map styles were requested for download multiple times. [#13518](https://github.com/mapbox/navigation/pull/13518)
- Added `geometryPointAt(index, precision)` extension on `RouteLeg` that decodes leg geometry one step at a time and stops at the target index, avoiding allocation of all subsequent points. [#13039](https://github.com/mapbox/navigation/pull/13039)
- Fixed an issue where requesting road cameras using `RoadCamerasProvider` could potential stall the main thread. [#13245](https://github.com/mapbox/navigation/pull/13245)
- Fix redundant leg geometry decoding in `SlowTrafficSegmentsFinder` by caching decoded points per route instance, and eliminate iterator allocations in congestion range lookup. [#13057](https://github.com/mapbox/navigation/pull/13057)
- Added a minimum speed threshold for applying avoid-maneuver-radius for continuou alternative route, so that Nav SDK can suggest alternatives with maneuver being close in case of low speed. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Improved off-road mode detection in parking aisles that are marked as tunnels. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Improved ADAS cache performance. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Fixed a crash that could occur after navigation shutdown. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Improved performance on routes with a large number of road alerts. [#13358](https://github.com/mapbox/navigation/pull/13358)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.21.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.21.0))
- Mapbox Navigation Native `v324.21.0`
- Mapbox Core Common `v24.21.0`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))


## Navigation SDK Core Framework 3.21.0-rc.1 - 25 March, 2026

#### Bug fixes and improvements
- Added `geometryPointAt(index, precision)` extension on `RouteLeg` that decodes leg geometry one step at a time and stops at the target index, avoiding allocation of all subsequent points. [#13039](https://github.com/mapbox/navigation/pull/13039)
- Fixed an issue where requesting road cameras using `RoadCamerasProvider` could potential stall the main thread. [#13245](https://github.com/mapbox/navigation/pull/13245)
- Fix redundant leg geometry decoding in `SlowTrafficSegmentsFinder` by caching decoded points per route instance, and eliminate iterator allocations in congestion range lookup. [#13057](https://github.com/mapbox/navigation/pull/13057)
- Added a minimum speed threshold for applying avoid-maneuver-radius for continuou alternative route, so that Nav SDK can suggest alternatives with maneuver being close in case of low speed. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Improved off-road mode detection in parking aisles that are marked as tunnels. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Improved ADAS cache performance. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Fixed a crash that could occur after navigation shutdown. [#13358](https://github.com/mapbox/navigation/pull/13358)
- Improved performance on routes with a large number of road alerts. [#13358](https://github.com/mapbox/navigation/pull/13358)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.21.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.21.0-rc.1))
- Mapbox Navigation Native `v324.21.0-rc.1`
- Mapbox Core Common `v24.21.0-rc.1`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))


## Navigation SDK Core Framework 3.20.1 - 27 March, 2026

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.20.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.20.2))
- Mapbox Navigation Native `v324.20.2`
- Mapbox Core Common `v24.20.2`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))

## Navigation SDK Core Framework 3.20.0 - 17 March, 2026
#### Features

- Introduced unknown status charger counts in EvStationMarker and EvseGroup models. [#12725](https://github.com/mapbox/navigation/pull/12725)
- Added metadata passthrough support for route refresh to enable app-side correlation [#12411](https://github.com/mapbox/navigation/pull/12411)
- Default navigation arrow width set to match the route line width. [#12578](https://github.com/mapbox/navigation/pull/12578)
- Made the navigation arrowhead shape more pointed. [#12578](https://github.com/mapbox/navigation/pull/12578)
- Changed the navigation arrow casing (border) color. [#12578](https://github.com/mapbox/navigation/pull/12578)
- The `EvseGroup` model is extended with `powerType` property indicating electrical power configuration. [#11553](https://github.com/mapbox/navigation/pull/11553)
- The `EvStationMarker` model is extended with a `chargersGroups` property, which is a list of all charging groups at the station. [#11553](https://github.com/mapbox/navigation/pull/11553)

#### Bug fixes and improvements

- Fixed the case when ASZ notification for speed zones missed when route is built inside a speed zone. [#12984](https://github.com/mapbox/navigation/pull/12984)
- Reduced main thread CPU usage in `MapboxTripSession` by dispatching route progress observer notifications asynchronously and moving the foreground notification update to an IO thread. [#12771](https://github.com/mapbox/navigation/pull/12771)
- Fixed a bug that caused the average speed zone to not update properly when a reroute occurred inside an active zone. [#12531](https://github.com/mapbox/navigation/pull/12531)
- Fixed a bug where, at the beginning of active guidance, users received a notification about a speed camera from the end of the route.  [#12531](https://github.com/mapbox/navigation/pull/12531)
- SDK now supports dynamic access token update for voice guidance and map gpt [#11300](https://github.com/mapbox/navigation/pull/11300)
- Fix route arrow scale when pixelRatio doesn't match density [#12348](https://github.com/mapbox/navigation/pull/12348)
- Ignore query param at geo deeplink parsing [#12742](https://github.com/mapbox/navigation/pull/12742)
- Introducing a new `RouterFailureType.ROUTE_EXPIRY_ERROR` router failure type to inform customers when an issue due to route expiry occurs. [#12379](https://github.com/mapbox/navigation/pull/12379)
- Improved internal flow of location updates for `LocationProviderSource.GPS`, which fixes delays in case main thread is blocked by the application.  [#12486](https://github.com/mapbox/navigation/pull/12486)
- Made default rounding increment in `DistanceFormatterOptions` dependent on distance numerical value. [#12123](https://github.com/mapbox/navigation/pull/12123)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.20.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.20.1))
- Mapbox Navigation Native `v324.20.1`
- Mapbox Core Common `v24.20.1`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))


## Navigation SDK Core Framework 3.20.0-rc.1 - 06 March, 2026
#### Features
- Introduced unknown status charger counts in EvStationMarker and EvseGroup models. [#12725](https://github.com/mapbox/navigation/pull/12725)
- Added metadata passthrough support for route refresh to enable app-side correlation [#12411](https://github.com/mapbox/navigation/pull/12411)
- Default navigation arrow width set to match the route line width. [#12578](https://github.com/mapbox/navigation/pull/12578)
- Made the navigation arrowhead shape more pointed. [#12578](https://github.com/mapbox/navigation/pull/12578)
- Changed the navigation arrow casing (border) color. [#12578](https://github.com/mapbox/navigation/pull/12578)
- The `EvseGroup` model is extended with `powerType` property indicating electrical power configuration. [#11553](https://github.com/mapbox/navigation/pull/11553)
- The `EvStationMarker` model is extended with a `chargersGroups` property, which is a list of all charging groups at the station. [#11553](https://github.com/mapbox/navigation/pull/11553)

#### Bug fixes and improvements
- Reduced main thread CPU usage in `MapboxTripSession` by dispatching route progress observer notifications asynchronously and moving the foreground notification update to an IO thread. [#12771](https://github.com/mapbox/navigation/pull/12771)
- Fixed a bug that caused the average speed zone to not update properly when a reroute occurred inside an active zone. [#12531](https://github.com/mapbox/navigation/pull/12531)
- Fixed a bug where, at the beginning of active guidance, users received a notification about a speed camera from the end of the route.  [#12531](https://github.com/mapbox/navigation/pull/12531)
- SDK now supports dynamic access token update for voice guidance and map gpt [#11300](https://github.com/mapbox/navigation/pull/11300)
- Fix route arrow scale when pixelRatio doesn't match density [#12348](https://github.com/mapbox/navigation/pull/12348)
- Ignore query param at geo deeplink parsing [#12742](https://github.com/mapbox/navigation/pull/12742)
- Introducing a new `RouterFailureType.ROUTE_EXPIRY_ERROR` router failure type to inform customers when an issue due to route expiry occurs. [#12379](https://github.com/mapbox/navigation/pull/12379)
- Improved internal flow of location updates for `LocationProviderSource.GPS`, which fixes delays in case main thread is blocked by the application.  [#12486](https://github.com/mapbox/navigation/pull/12486)
- Made default rounding increment in `DistanceFormatterOptions` dependent on distance numerical value. [#12123](https://github.com/mapbox/navigation/pull/12123)

#### Known issues :warning:

- Alternative Route Gap at Fork Points.
The `AlternativeRouteMetadata#forkIntersectionOfAlternativeRoute` property currently references a location slightly downstream (ahead) of the actual logical fork point. As a result, alternative routes may appear to start with a physical gap relative to the primary route at the junction.

- Loss of Original Route After Waypoint.
When a user switches from the primary to an alternative route after passing the first waypoint, the original primary route may be discarded by the navigator. This results in the UI displaying only the newly selected route, preventing the user from easily switching back to the previous path.

- Bearing Discrepancy After Map Re-attachment.
The location puck's visual bearing may diverge from the bearing reported by the navigator following a map detachment/re-attachment cycle. This synchronization lag occurs when the Nav Coordination MapComponent instance is destroyed and recreated, causing a temporary mismatch between the UI's orientation and the underlying navigation state.


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.20.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.20.0-rc.1))
- Mapbox Navigation Native `v324.20.0-rc.1`
- Mapbox Core Common `v24.20.0-rc.1`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))

## Navigation SDK Core Framework 3.19.5 - 03 April, 2026

#### Bug fixes and improvements
- Fixed an issue where the navigator would cycle between alternative routes after missing a turn at a waypoint, when the current position was already on the second leg of both the primary and alternative routes. [#13548](https://github.com/mapbox/navigation/pull/13548)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.19.5` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.19.5))
- Mapbox Navigation Native `v324.19.5`
- Mapbox Core Common `v24.19.5`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))

## Navigation SDK Core Framework 3.19.3 - 01 April, 2026
#### Features


#### Bug fixes and improvements
- Fixed an issue where `RouteLineColorResources.routeClosureColor` wasn't being applied to the route correctly when there is a road closure.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.19.3` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.19.3))
- Mapbox Navigation Native `v324.19.3`
- Mapbox Core Common `v24.19.3`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))


## Navigation SDK Core Framework 3.19.2 - 27 March, 2026

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.19.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.19.2))
- Mapbox Navigation Native `v324.19.2`
- Mapbox Core Common `v24.19.2`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))

## Navigation SDK Core Framework 3.19.0 - 27 February, 2026
#### Features

- Update `LaneIndicator` to include a new attribute `accessDesignated`, which contains a list of the designated supported access types for a lane. [#12022](https://github.com/mapbox/navigation/pull/12022)
- Added support of route refresh in case native route object is enabled. [#11924](https://github.com/mapbox/navigation/pull/11924)
- Added optional parameter `ImmediateRouteRefreshCallback` to `RouteRefreshController.requestImmediateRouteRefresh()` to receive request result notifications via `ImmediateRouteRefreshResult`. [#12101](https://github.com/mapbox/navigation/pull/12101)

#### Bug fixes and improvements
- Fixed a bug that caused the average speed zone to not update properly when a reroute occurred inside an active zone. [#12531](https://github.com/mapbox/navigation/pull/12531)
- Fixed a bug where, at the beginning of active guidance, users received a notification about a speed camera from the end of the route.  [#12531](https://github.com/mapbox/navigation/pull/12531)
- Introducing a new `RouterFailureType.ROUTE_EXPIRY_ERROR` router failure type to inform customers when an issue due to route expiry occurs. [#12379](https://github.com/mapbox/navigation/pull/12379)
- Fix route arrow scale when pixelRatio doesn't match density [#12348](https://github.com/mapbox/navigation/pull/12348)
- Improved internal flow of location updates for `LocationProviderSource.GPS`, which fixes delays in case main thread is blocked by the application.  [#12486](https://github.com/mapbox/navigation/pull/12486)
- Fixed an issue where arrival maneuver arrow used to point in an incorrect direction. Now the arrows for arrival maneuvers are not displayed. [#12172](https://github.com/mapbox/navigation/pull/12172)
- Improve `MapboxRouteArrowView` rendering logic to better handle render attempts when the previous attempt fails. [#12099](https://github.com/mapbox/navigation/pull/12099)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.19.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.19.0))
- Mapbox Navigation Native `v324.19.0`
- Mapbox Core Common `v24.19.0`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))


## Navigation SDK Core Framework 3.19.0-rc.1 - 13 February, 2026
#### Features


#### Bug fixes and improvements
- Introducing a new `RouterFailureType.ROUTE_EXPIRY_ERROR` router failure type to inform customers when an issue due to route expiry occurs. [#12379](https://github.com/mapbox/navigation/pull/12379)
- Fix route arrow scale when pixelRatio doesn't match density [#12348](https://github.com/mapbox/navigation/pull/12348)
- Improved internal flow of location updates for `LocationProviderSource.GPS`, which fixes delays in case main thread is blocked by the application.  [#12486](https://github.com/mapbox/navigation/pull/12486)

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.19.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.19.0-rc.1))
- Mapbox Navigation Native `v324.19.0-rc.1`
- Mapbox Core Common `v24.19.0-rc.1`
- Mapbox Java `v7.10.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.10.0))

## Navigation SDK Core Framework 3.19.0-beta.1 - 30 January, 2026
#### Features
- Update `LaneIndicator` to include a new attribute `accessDesignated`, which contains a list of the designated supported access types for a lane. [#12022](https://github.com/mapbox/navigation/pull/12022)
- Added support of route refresh in case native route object is enabled. [#11924](https://github.com/mapbox/navigation/pull/11924)
- Added optional parameter `ImmediateRouteRefreshCallback` to `RouteRefreshController.requestImmediateRouteRefresh()` to receive request result notifications via `ImmediateRouteRefreshResult`. [#12101](https://github.com/mapbox/navigation/pull/12101)

#### Bug fixes and improvements
- Fixed an issue where arrival maneuver arrow used to point in an incorrect direction. Now the arrows for arrival maneuvers are not displayed. [#12172](https://github.com/mapbox/navigation/pull/12172)
- Improve `MapboxRouteArrowView` rendering logic to better handle render attempts when the previous attempt fails. [#12099](https://github.com/mapbox/navigation/pull/12099)

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.19.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.19.0-beta.1))
- Mapbox Navigation Native `v324.19.0-beta.1`
- Mapbox Core Common `v24.19.0-beta.1`
- Mapbox Java `v7.9.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.9.0))


## Navigation SDK Core Framework 3.18.3 - 01 April, 2026

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.18.3` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.18.3))
- Mapbox Navigation Native `v324.18.3`
- Mapbox Core Common `v24.18.3`
- Mapbox Java `v7.9.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.9.0))

## Navigation SDK Core Framework 3.18.2 - 12 February, 2026

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.18.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.18.2))
- Mapbox Navigation Native `v324.18.2`
- Mapbox Core Common `v24.18.2`
- Mapbox Java `v7.9.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.9.0))

## Navigation SDK Core Framework 3.18.1 - 30 January, 2026
#### Features
- Added support of route refresh in case native route object is enabled. [#11924](https://github.com/mapbox/navigation/pull/11924)

#### Bug fixes and improvements
- Fixed an issue where arrival maneuver arrow used to point in an incorrect direction. Now the arrows for arrival maneuvers are not displayed. [#12172](https://github.com/mapbox/navigation/pull/12172)
- Improve `MapboxRouteArrowView` rendering logic to better handle render attempts when the previous attempt fails. [#12099](https://github.com/mapbox/navigation/pull/12099)


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.18.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.18.1))
- Mapbox Navigation Native `v324.18.1`
- Mapbox Core Common `v24.18.1`
- Mapbox Java `v7.9.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.9.0))


## Navigation SDK Core Framework 3.17.3 - 25 February, 2026
#### Features


#### Bug fixes and improvements
- Improved internal flow of location updates for `LocationProviderSource.GPS`, which fixes delays in case main thread is blocked by the application.  [#12486](https://github.com/mapbox/navigation/pull/12486)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.17.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.17.2))
- Mapbox Navigation Native `v324.17.2`
- Mapbox Core Common `v24.17.2`
- Mapbox Java `v7.9.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.9.0))

## Navigation SDK Core Framework 3.17.2 - 23 December, 2025
#### Features

#### Bug fixes and improvements
- Fix the race condition when canceling Active Guidance from a background thread that does not immediately cancel Route Progress updates.  [#11778](https://github.com/mapbox/navigation/pull/11778)
- Fix ConcurrentModificationException in RoadCamerasByTileProvider [#11837](https://github.com/mapbox/navigation/pull/11837)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.17.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.17.1))
- Mapbox Navigation Native `v324.17.1`
- Mapbox Core Common `v24.17.1`
- Mapbox Java `v7.9.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.9.0))

## Navigation SDK Core Framework 3.18.0 - 16 January, 2026
#### Features
- Added Experimental API: [#11743](https://github.com/mapbox/navigation/pull/11743)
  - `MapboxNavigation#registerVoiceInstructionsAvailableObserver` and `MapboxNavigation#unregisterVoiceInstructionsAvailableObserver` to be notified when voice instructions become available or unavailable;
  - `MapboxNavigation#registerRelevantVoiceInstructionsCallback` to asynchronously fetch the relevant voice instructions. The callback is automatically unregistered after receiving the result, register again for subsequent voice instructions.
- Adjust `FollowingFrameOptions#maxZoom`: The value is applied directly when the zoom level cannot be calculated precisely, e.g., when there is only one point to frame. [#11634](https://github.com/mapbox/navigation/pull/11634)
- Added experimental `NavigationOptions#nativeRouteObject` which replaces `NavigationRoute#directionsRoute` with a thin wrapper that accesses route data stored in native memory instead of the Java heap. This is an early preview feature. When this option is enabled, some functionality is disabled or not fully supported. See `NavigationOptions#nativeRouteObject` for details. [#11596](https://github.com/mapbox/navigation/pull/11596)
- Added new class `RerouteStateV2`: it allows you to observe RerouteStates with additional substates which are not present in the original `RerouteState`.  [#11698](https://github.com/mapbox/navigation/pull/11698)
Current additional states are: `RerouteStateV2.Deviation.ApplyingRoute` and `RerouteStateV2.Deviation.RouteIgnored`. See the corresponding API reference for details.
To observe `RerouteStateV2`, register a new type of observer: `RerouteController#registerRerouteStateV2Observer`. 
- Capabilities list added to Connector object, so it is now compliant with OCPI v2.3.0 [#11151](https://github.com/mapbox/navigation/pull/11151)
- Added an `isOffline` flag to `EvStationMarker` to identify when charging station data is from an offline source. [#11369](https://github.com/mapbox/navigation/pull/11369)
- Added support of connctor types in `MapboxEvViewOptions` to be able to customize the connector types in the EV view. [#11425](https://github.com/mapbox/navigation/pull/11425)

#### Bug fixes and improvements
- Made `RerouteStateV2` a sealed class. [#12017](https://github.com/mapbox/navigation/pull/12017)
- Fixed an issue when adding a stop point on top of an already traversed route, which could show a carried-over vanishing portion from the previous route. That is accomplished by ensuring that `MapboxRouteLineApi.getVanishPointOffset()` returns `0.0` in case the point was in the `VanishingPointState.DISABLED` state. [#11471](https://github.com/mapbox/navigation/pull/11471)
- Obfuscated access token in `RouteShieldError#url#toString`. [#11716](https://github.com/mapbox/navigation/pull/11716)
- Improved EV SAR call to return requested number of charging stations. [#11697](https://github.com/mapbox/navigation/pull/11697)
- Fix an issue where transitionEndListener passed to `NavigationCamera#requestNavigationCameraTo...` might not have been invoked. [#11276](https://github.com/mapbox/navigation/pull/11276)
- Added RouterFailureType.ROUTER_RECREATION_ERROR when route request failed due to related reason and made this error retriable [#11274](https://github.com/mapbox/navigation/pull/11274)
- Improved EV SAR call to evenly distribute returned stations along the route. [#11796](https://github.com/mapbox/navigation/pull/11796)
- Fixed the routing tiles endpoint configuration to avoid redundant recreations of Navigator. [#11661](https://github.com/mapbox/navigation/pull/11661)
- ⚠️ Breaking change (preview API): removed `MapboxRoadCamerasDisplayConfig::showOnlyOnRoute`. Safe to remove. It's now a default behavior to show road cameras only on the route. [#11625](https://github.com/mapbox/navigation/pull/11625)
- Fixed a bug that prevented road cameras from being removed from the map after passing them. [#11625](https://github.com/mapbox/navigation/pull/11625)
- Fixed an issue when re-route might take a few minutes, because of missing internal reroute state. [#11200](https://github.com/mapbox/navigation/pull/11200)
- Fixed Copilot issues that caused recordings to be lost. [#11619](https://github.com/mapbox/navigation/pull/11619)
- Fix ConcurrentModificationException in RoadCamerasByTileProvider [#11837](https://github.com/mapbox/navigation/pull/11837)
- Fix the race condition when canceling Active Guidance from a background thread that does not immediately cancel Route Progress updates.  [#11778](https://github.com/mapbox/navigation/pull/11778)
- Avoid high CPU usage when user location is stationary.
- Improve DR accuracy after sharp turns.
- Update interface for retrieving last voice instructions.
- Incident positions after a mid-leg route refresh now use correct distance calculations.
- Disallow route refreshes in other than Tracking state.
- Native Route Object via Flatbuffers.
- Implement reaction on notification that user-provided-charging-station is not needed.

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.18.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.18.0))
- Mapbox Navigation Native `v324.18.0`
- Mapbox Core Common `v24.18.0`
- Mapbox Java `v7.9.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.9.0))


## Navigation SDK Core Framework 3.18.0-beta.1 - 18 December, 2025
#### Features
- Adjust `FollowingFrameOptions#maxZoom`: The value is applied directly when the zoom level cannot be calculated precisely, e.g., when there is only one point to frame. [#11634](https://github.com/mapbox/navigation/pull/11634)
- Added experimental `NavigationOptions#nativeRouteObject` which replaces `NavigationRoute#directionsRoute` with a thin wrapper that accesses route data stored in native memory instead of the Java heap. This is an early preview feature. When this option is enabled, some functionality is disabled or not fully supported. See `NavigationOptions#nativeRouteObject` for details. [#11596](https://github.com/mapbox/navigation/pull/11596)
- Added new class `RerouteStateV2`: it allows you to observe RerouteStates with additional substates which are not present in the original `RerouteState`.  [#11698](https://github.com/mapbox/navigation/pull/11698)
Current additional states are: `RerouteStateV2.Deviation.ApplyingRoute` and `RerouteStateV2.Deviation.RouteIgnored`. See the corresponding API reference for details.
To observe `RerouteStateV2`, register a new type of observer: `RerouteController#registerRerouteStateV2Observer`. 
- Capabilities list added to Connector object, so it is now compliant with OCPI v2.3.0 [#11151](https://github.com/mapbox/navigation/pull/11151)
- Added an `isOffline` flag to `EvStationMarker` to identify when charging station data is from an offline source. [#11369](https://github.com/mapbox/navigation/pull/11369)
- Added support of connctor types in `MapboxEvViewOptions` to be able to customize the connector types in the EV view. [#11425](https://github.com/mapbox/navigation/pull/11425)

#### Bug fixes and improvements
- Fixed an issue when adding a stop point on top of an already traversed route, which could show a carried-over vanishing portion from the previous route. That is accomplished by ensuring that `MapboxRouteLineApi.getVanishPointOffset()` returns `0.0` in case the point was in the `VanishingPointState.DISABLED` state. [#11471](https://github.com/mapbox/navigation/pull/11471)
- Obfuscated access token in `RouteShieldError#url#toString`. [#11716](https://github.com/mapbox/navigation/pull/11716)
- Improved EV SAR call to return requested number of charging stations. [#11697](https://github.com/mapbox/navigation/pull/11697)
- Fix an issue where transitionEndListener passed to `NavigationCamera#requestNavigationCameraTo...` might not have been invoked. [#11276](https://github.com/mapbox/navigation/pull/11276)
- Added RouterFailureType.ROUTER_RECREATION_ERROR when route request failed due to related reason and made this error retriable [#11274](https://github.com/mapbox/navigation/pull/11274)
- Improved EV SAR call to evenly distribute returned stations along the route. [#11796](https://github.com/mapbox/navigation/pull/11796)
- Fixed the routing tiles endpoint configuration to avoid redundant recreations of Navigator. [#11661](https://github.com/mapbox/navigation/pull/11661)
- ⚠️ Breaking change (preview API): removed `MapboxRoadCamerasDisplayConfig::showOnlyOnRoute`. Safe to remove. It's now a default behavior to show road cameras only on the route. [#11625](https://github.com/mapbox/navigation/pull/11625)
- Fixed a bug that prevented road cameras from being removed from the map after passing them. [#11625](https://github.com/mapbox/navigation/pull/11625)
- Fixed an issue when re-route might take a few minutes, because of missing internal reroute state. [#11200](https://github.com/mapbox/navigation/pull/11200)
- Fixed Copilot issues that caused recordings to be lost. [#11619](https://github.com/mapbox/navigation/pull/11619)
- Fix ConcurrentModificationException in RoadCamerasByTileProvider [#11837](https://github.com/mapbox/navigation/pull/11837)
- Fix the race condition when canceling Active Guidance from a background thread that does not immediately cancel Route Progress updates.  [#11778](https://github.com/mapbox/navigation/pull/11778)
- Avoid high CPU usage when user location is stationary.
- Improve DR accuracy after sharp turns.
- Update interface for retrieving last voice instructions.
- Incident positions after a mid-leg route refresh now use correct distance calculations.
- Disallow route refreshes in other than Tracking state.
- Native Route Object via Flatbuffers.
- Implement reaction on notification that user-provided-charging-station is not needed.

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.18.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.18.0-beta.1))
- Mapbox Navigation Native `v324.18.0-beta.1`
- Mapbox Core Common `v24.18.0-beta.1`
- Mapbox Java `v7.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.8.0))


## Navigation SDK Core Framework 3.17.0 - 04 December, 2025
#### Features
- Added support of connctor types in `MapboxEvViewOptions` to be able to customize the connector types in the EV view. [#11425](https://github.com/mapbox/navigation/pull/11425)
- Introduce RouterFailureType::MISSING_TILES_ERROR to indicate inability to build a route due to missing routing tiles [#11532](https://github.com/mapbox/navigation/pull/11532)
- Added ZStd support. [#11513](https://github.com/mapbox/navigation/pull/11513)
- Reduce map-matcher latency in urban areas with lots of small tunnels [#11338](https://github.com/mapbox/navigation/pull/11338)
- Add a feature to override location bearing with yaw from orientation data (inactive by default) [#11338](https://github.com/mapbox/navigation/pull/11338)
- Improve behavior on elevated highways (new ramp detection algorithm) [#11338](https://github.com/mapbox/navigation/pull/11338)
- Improve tunnel mode behavior after tunnel exists [#11338](https://github.com/mapbox/navigation/pull/11338)
- Added `freeFlowSpeed` and `constrainedFlowSpeed` properties to `EHorizonEdgeMetadata` to provide free flow and constrained flow speed information for edges [#11309](https://github.com/mapbox/navigation/pull/11309)
- Reworked tile-loading delay in the predictive cache: both tile loading and tile calculation are now deferred for improved performance. [#11078](https://github.com/mapbox/navigation/pull/11078)
- Improved routing logic to prevent fallback to the onboard router when the online router encounters a `RouteCreationError`. [#11078](https://github.com/mapbox/navigation/pull/11078)
- Added `MapMatchingOptions.voiceUnits` which allows applications to specify the unit system used for voice instructions in Map Matching. [#10510](https://github.com/mapbox/navigation/pull/10510)
- Improved performance of `MapboxEvViewClient`, its API was slightly changed for this purpose. [#10693](https://github.com/mapbox/navigation/pull/10693)
- Renamed `EvStationMarker.maxOutputPower` to `EvStationMarker.maxOutputPowerkW` for clarity. [#10622](https://github.com/mapbox/navigation/pull/10622)
- Added new field `EvStationMarker.capabilities` to describe supported charging capabilities. [#10622](https://github.com/mapbox/navigation/pull/10622)
- Expose roadEdgeId to LocationMatcherResult [#10532](https://github.com/mapbox/navigation/pull/10532)
- Added `styleSlot` parameter to `MapboxEvViewOptions` to give more control over EV layer placement. [#10857](https://github.com/mapbox/navigation/pull/10857)

#### Bug fixes and improvements

- Fix ANR when calling `MapboxVoiceInstructionsPlayer::stop` [#10905](https://github.com/mapbox/navigation/pull/10905)
- Fixed waypoint handling when multiple matches are returned in `MapMatchingSuccessfulResult.matches`; waypoints are now assigned to the correct match. [#10517](https://github.com/mapbox/navigation/pull/10517)

- ⚠️ Breaking change (preview API): removed `MapMatchingSuccessfulResult#navigationRoutes`.

Why: the `navigationRoutes` property encouraged incorrect usage — calling
`mapboxNavigation.setNavigationRoutes(result.navigationRoutes)` treats each
match as an alternative route. Matches are results of map-matching and are
not true route alternatives; passing them together will make the
navigator accept only first route rejecting the others.
Migration guide: select navigation route from a single match `mapboxNavigation.setNavigationRoutes(listOf(result.matches.first().navigationRoute))`. [#10517](https://github.com/mapbox/navigation/pull/10517)
- Fixed an issue where `FollowingFrameOptions#defaultPitch` updates were not applied in Free Drive.  [#11000](https://github.com/mapbox/navigation/pull/11000)
- Fix NullPointerException when using `MapboxVoiceInstructionsPlayer`. [#10832](https://github.com/mapbox/navigation/pull/10832)
- Fix the bug that causes road cameras on alternative routes to not be removed from the road when its road is not active or passed during active guidance. [#10630](https://github.com/mapbox/navigation/pull/10630)
- Optimize the performance of road cameras in Free Drive mode.  [#11073](https://github.com/mapbox/navigation/pull/11073)
- Add the `RoadCamerasConfig::belowLayerId` option to set the `belowLayerId` of the road camera icons layer. By default, the road camera icons are below the 2D CPP icon. [#10922](https://github.com/mapbox/navigation/pull/10922)
- ⚠️ Breaking changes in Experimental API: `RoadCamerasConfig` constructor is now private. Use the `RoadCamerasConfig.Builder` to create an instance of `RoadCamerasConfig`. [#10922](https://github.com/mapbox/navigation/pull/10922)
- Fix an issue where transitionEndListener passed to `NavigationCamera#requestNavigationCameraTo...` might not have been invoked. [#11276](https://github.com/mapbox/navigation/pull/11276)
- Added RouterFailureType.ROUTER_RECREATION_ERROR when route request failed due to related reason and made this error retriable [#11325](https://github.com/mapbox/navigation/pull/11325)
- Fixed an issue when adding a stop point on top of an already traversed route, which could show a carried-over vanishing portion from the previous route. That is accomplished by ensuring that `MapboxRouteLineApi.getVanishPointOffset()` returns `0.0` in case the point was in the `VanishingPointState.DISABLED` state. [#11471](https://github.com/mapbox/navigation/pull/11471)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.17.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.17.0))
- Mapbox Navigation Native `v324.17.0`
- Mapbox Core Common `v24.17.0`
- Mapbox Java `v7.9.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.9.0))

## Navigation SDK Core Framework 3.17.0-rc.3 - 01 December, 2025
#### Features
- Added ZStd support. [#11513](https://github.com/mapbox/navigation/pull/11513)

#### Bug fixes and improvements
- Fixed an issue when adding a stop point on top of an already traversed route, which could show a carried-over vanishing portion from the previous route. That is accomplished by ensuring that `MapboxRouteLineApi.getVanishPointOffset()` returns `0.0` in case the point was in the `VanishingPointState.DISABLED` state. [#11471](https://github.com/mapbox/navigation/pull/11471)

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.17.0-rc.3` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.17.0-rc.3))
- Mapbox Navigation Native `v324.17.0-rc.3`
- Mapbox Core Common `v24.17.0-rc.3`
- Mapbox Java `v7.9.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.9.0))


## Navigation SDK Core Framework 3.17.0-rc.2 - 24 November, 2025
#### Features


#### Bug fixes and improvements


#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.17.0-rc.2` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.17.0-rc.2))
- Mapbox Navigation Native `v324.17.0-rc.2`
- Mapbox Core Common `v24.17.0-rc.2`
- Mapbox Java `v7.9.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.9.0))


## Navigation SDK Core Framework 3.17.0-rc.1 - 20 November, 2025
#### Features
- Reduce map-matcher latency in urban areas with lots of small tunnels [#11338](https://github.com/mapbox/navigation/pull/11338)
- Add a feature to override location bearing with yaw from orientation data (inactive by default) [#11338](https://github.com/mapbox/navigation/pull/11338)
- Improve behavior on elevated highways (new ramp detection algorithm) [#11338](https://github.com/mapbox/navigation/pull/11338)
- Improve tunnel mode behavior after tunnel exists [#11338](https://github.com/mapbox/navigation/pull/11338)
- Added `freeFlowSpeed` and `constrainedFlowSpeed` properties to `EHorizonEdgeMetadata` to provide free flow and constrained flow speed information for edges [#11309](https://github.com/mapbox/navigation/pull/11309)

#### Bug fixes and improvements
- Fix an issue where transitionEndListener passed to `NavigationCamera#requestNavigationCameraTo...` might not have been invoked. [#11276](https://github.com/mapbox/navigation/pull/11276)
- Added RouterFailureType.ROUTER_RECREATION_ERROR when route request failed due to related reason and made this error retriable [#11325](https://github.com/mapbox/navigation/pull/11325)

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.17.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.17.0-rc.1))
- Mapbox Navigation Native `v324.17.0-rc.1`
- Mapbox Core Common `v24.17.0-rc.1`
- Mapbox Java `v7.9.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.9.0))

## Navigation SDK Core Framework 3.17.0-beta.1 - 07 November, 2025
#### Features
- Reworked tile-loading delay in the predictive cache: both tile loading and tile calculation are now deferred for improved performance. [#11078](https://github.com/mapbox/navigation/pull/11078)
- Improved routing logic to prevent fallback to the onboard router when the online router encounters a `RouteCreationError`. [#11078](https://github.com/mapbox/navigation/pull/11078)
- Added `MapMatchingOptions.voiceUnits` which allows applications to specify the unit system used for voice instructions in Map Matching. [#10510](https://github.com/mapbox/navigation/pull/10510)
- Improved performance of `MapboxEvViewClient`, its API was slightly changed for this purpose. [#10693](https://github.com/mapbox/navigation/pull/10693)
- Renamed `EvStationMarker.maxOutputPower` to `EvStationMarker.maxOutputPowerkW` for clarity. [#10622](https://github.com/mapbox/navigation/pull/10622)
- Added new field `EvStationMarker.capabilities` to describe supported charging capabilities. [#10622](https://github.com/mapbox/navigation/pull/10622)
- Expose roadEdgeId to LocationMatcherResult [#10532](https://github.com/mapbox/navigation/pull/10532)
- Added `styleSlot` parameter to `MapboxEvViewOptions` to give more control over EV layer placement. [#10857](https://github.com/mapbox/navigation/pull/10857)

#### Bug fixes and improvements
- Fix ANR when calling `MapboxVoiceInstructionsPlayer::stop` [#10905](https://github.com/mapbox/navigation/pull/10905)
- Fixed waypoint handling when multiple matches are returned in `MapMatchingSuccessfulResult.matches`; waypoints are now assigned to the correct match. [#10517](https://github.com/mapbox/navigation/pull/10517)

- ⚠️ Breaking change (preview API): removed `MapMatchingSuccessfulResult#navigationRoutes`.

Why: the `navigationRoutes` property encouraged incorrect usage — calling
  `mapboxNavigation.setNavigationRoutes(result.navigationRoutes)` treats each
  match as an alternative route. Matches are results of map-matching and are
  not true route alternatives; passing them together will make the
  navigator accept only first route rejecting the others.
  Migration guide: select navigation route from a single match `mapboxNavigation.setNavigationRoutes(listOf(result.matches.first().navigationRoute))`. [#10517](https://github.com/mapbox/navigation/pull/10517)
- Fixed an issue where `FollowingFrameOptions#defaultPitch` updates were not applied in Free Drive.  [#11000](https://github.com/mapbox/navigation/pull/11000)
- Fix NullPointerException when using `MapboxVoiceInstructionsPlayer`. [#10832](https://github.com/mapbox/navigation/pull/10832)
- Fix the bug that causes road cameras on alternative routes to not be removed from the road when its road is not active or passed during active guidance. [#10630](https://github.com/mapbox/navigation/pull/10630)
- Optimize the performance of road cameras in Free Drive mode.  [#11073](https://github.com/mapbox/navigation/pull/11073)
- Add the `RoadCamerasConfig::belowLayerId` option to set the `belowLayerId` of the road camera icons layer. By default, the road camera icons are below the 2D CPP icon. [#10922](https://github.com/mapbox/navigation/pull/10922)
- ⚠️ Breaking changes in Experimental API: `RoadCamerasConfig` constructor is now private. Use the `RoadCamerasConfig.Builder` to create an instance of `RoadCamerasConfig`. [#10922](https://github.com/mapbox/navigation/pull/10922)

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.17.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.17.0-beta.1))
- Mapbox Navigation Native `v324.17.0-beta.1`
- Mapbox Core Common `v24.17.0-beta.1`
- Mapbox Java `v7.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.8.0))


## Navigation SDK Core Framework 3.16.0-beta.1 - 26 September, 2025
#### Notes
3.16.x is the next version after 3.12.x. For technical reasons, versions 3.13.x, 3.14.x and 3.15.x are skipped. Starting from 3.16.x, the Nav SDK minor version will be aligned with other Mapbox dependencies.

#### Features
- Added `AdasEdgeAttributes#isBuiltUpArea` and `AdasEdgeAttributes#roadItems` properties. [#10361](https://github.com/mapbox/navigation/pull/10361)
- Added a new `RoadObjectMatcherOptions` class that configures the road object matching behavior. Available through the `NavigationOptions` class. [#10326](https://github.com/mapbox/navigation/pull/10326)
- Added `RouteCalloutUiStateProvider` class that allows to listen to Route Callout UI data. [#9580](https://github.com/mapbox/navigation/pull/9580)
Normally, route callouts are drawn under the hood in NavSDK when this feature is enabled in `MapboxRouteLineApiOptions`.
However, there might be cases when app wants to only get the callout data from NavSDK and attach the DVA itself.
An example of such a case is using Mapbox Maps SDK Compose extensions: attaching a DVA for
Compose MapboxMap is done via [compose-specific API](https://docs.mapbox.com/android/maps/examples/compose/dynamic-view-annotations/),
which is not currently supported by NavSDK.
In this case you may listen to `RouteCalloutUiStateData` updates via `RouteCalloutUiStateProvider` and use its information by attach a DVA.
- Added experimental overloads for `MapboxManeuverApi#getRoadShields` and `MapboxRouteShieldApi#getRouteShields` that accept a `ShieldFontConfig` parameter, enabling custom font selection for route shields. [#9565](https://github.com/mapbox/navigation/pull/9565)
- Added experimental `MapboxNavigationSVGExternalFileResolver` that can resolve fonts for SVG rendering from assets or use system fonts. [#9565](https://github.com/mapbox/navigation/pull/9565)
- Updated `MapboxNavigation.replanRoute()` to now accept a new optional parameter of type `ReplanRoutesCallback`. [#10376](https://github.com/mapbox/navigation/pull/10376)

#### Bug fixes and improvements
- Fix the bug that causes road cameras on alternative routes to be marked as passed but not removed from the map. [#10456](https://github.com/mapbox/navigation/pull/10456)
- Fixed the incorrect order of callbacks when notifying about road cameras on the route. [#10120](https://github.com/mapbox/navigation/pull/10120)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.16.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.16.0-beta.1))
- Mapbox Navigation Native `v324.16.0-beta.1`
- Mapbox Core Common `v24.16.0-beta.1`
- Mapbox Java `v7.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.8.0))


## Navigation SDK Core Framework 3.12.0-beta.1 - 15 August, 2025
#### Features
- Added ability to filter by data source in EV charging station search operations. [#9787](https://github.com/mapbox/navigation/pull/9787)
- Added `MapboxSpeedZoneInfo` class to represent speed zone information. Available through `UpcomingCamerasObserver::onSpeedZoneInfo` and `RoadCamerasMapCallback::onSpeedZoneInfo` callbacks. [#9730](https://github.com/mapbox/navigation/pull/9730)
- Added experimental support for ADAS tiles in the predictive cache. See `PredictiveCacheNavigationOptions` for more information. [#9882](https://github.com/mapbox/navigation/pull/9882)

#### Bug fixes and improvements
- Optimize the `MapboxRouteArrowView` to skip re-rendering arrows that have not changed. [#9970](https://github.com/mapbox/navigation/pull/9970)
- Decrased excessively high GeoJSON buffer size from 128 to 32 to improve the memory footprint. [#10039](https://github.com/mapbox/navigation/pull/10039)
- Avoid unnecessary navigation arrow GeoJSON updates [#9738](https://github.com/mapbox/navigation/pull/9738)
- Optimized camera animations that involve significant zoom change. [#9729](https://github.com/mapbox/navigation/pull/9729)
- Fixed an issue where the closer part of route line might have been overlapped by a farther part in case they covered the same space within a single leg (e. g. U-turns on narrow roads).  [#9717](https://github.com/mapbox/navigation/pull/9717)
- Don't reset the re-route request when on-route/off-route events are flaky. [#9824](https://github.com/mapbox/navigation/pull/9824)
- Use the `enhancedLocation` in the RoadCamerasManager class to get a more accurate current speed for the vehicle. [#9944](https://github.com/mapbox/navigation/pull/9944)


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Navigation-native `324.15.0-beta.2`
- Common SDK `24.15.0-beta.2`
- Maps SDK `11.15.0-beta.2`
- Android Search SDK `2.15.0-beta.2`

## Navigation SDK Core Framework 3.11.0-beta.1 - 04 July, 2025
#### Features
- Added support for Android 16 KB page-size devices. To consume SDK compatible with NDK 27 you need to add `-ndk27` suffix to the artifact name, for example, `com.mapbox.navigationcore:navigation` -> `com.mapbox.navigationcore:navigation-ndk27`. [#9556](https://github.com/mapbox/navigation/pull/9556)
- Added method overload `TilesetDescriptorFactory#getLatest(Boolean)` that allows to specify whether to include ADAS tiles. [#9299](https://github.com/mapbox/navigation/pull/9299)
- Extracted TTS functionality into a new module `audio`. [#9305](https://github.com/mapbox/navigation/pull/9305)
- DR improvements - more robust models for GNSS trust, road calibration, and wheel speed trust; [#9629](https://github.com/mapbox/navigation/pull/9629)
- Improve main thread utilization by removing unintended locks (visible on systems with overloaded CPU) [#9629](https://github.com/mapbox/navigation/pull/9629)
- Disable the defaults for collection of tunnel/bridge subgraphs in free drive. The clients will need to specify explicitly which objects to collect via AlertServiceOptions in the public SDK interface. [#9629](https://github.com/mapbox/navigation/pull/9629)
- Added support of immediate update of location puck bearing in [NavigationLocationProvider] in case of overlapping key points. [#9181](https://github.com/mapbox/navigation/pull/9181)
- `RoadCamerasManager` in active guidance now relies on new `road_camera` Directions API annotation, which improves the performance of the camera data retrieval and quality of the data. [#9098](https://github.com/mapbox/navigation/pull/9098)
- Added `MapboxRoadCamera::activeGuidanceInfo`, containing information about the route id, leg index, geometry index and step intersection of the camera in active guidance. [#9098](https://github.com/mapbox/navigation/pull/9098)

#### Bug fixes and improvements
- Fixed an issue where after a reroute the vanishing point on the route line might have been ahead of the actual vehicle's position. [#9406](https://github.com/mapbox/navigation/pull/9406)
- Added `HistoryRecorderOptions#shouldRecordRouteLineEvents` property to enable/disable route line events collection for manual recording (see `CopilotOptions#shouldRecordRouteLineEvents` for the same functionality with Copilot); it is disabled by default. [#9434](https://github.com/mapbox/navigation/pull/9434)
- Fixed an issue where the Speed Camera notification would appear prematurely when the car's speed was 0. [#9590](https://github.com/mapbox/navigation/pull/9590)
- Fix Route replayer: normalize bearing values to be in the range of [0..360) degrees. [#9266](https://github.com/mapbox/navigation/pull/9266)
- Fixed a bug where alternative routes from `RoutesUpdatedResult#ignoredRoutes` were set to `RoutesUpdatedResult#navigationRoutes` after the first route progress update. [#9259](https://github.com/mapbox/navigation/pull/9259)
- Fix when already passed part of route appears behind CCP [#9510](https://github.com/mapbox/navigation/pull/9510)

#### Known issues :warning:


#### Other changes


### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Java `v7.4.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.4.0))


## Navigation SDK Core Framework 3.10.0-beta.1 - 22 May, 2025
#### Features
- ⚠️ Breaking changes in Experimental API. `MapboxEvSearchClientFactory.#getInstance()` no longer accepts access token as a parameter. The default `MapboxOptions.accessToken` will be used. [#9170](https://github.com/mapbox/navigation/pull/9170)
- `CarPlaceSearchOptions.accessToken` and corresponding builder function has been deprecated because `accessToken` is no longer in use as the search component now uses the default `MapboxOptions.accessToken`. [#9170](https://github.com/mapbox/navigation/pull/9170)
- Used legacy/custom date primitives in EV modules to support older Android API levels. [#9111](https://github.com/mapbox/navigation/pull/9111)
- Added `DriverNotification`, `DriverNotificationProvider` interfaces with `EvBetterRouteNotificationProvider` and `SlowTrafficNotificationProvider` implementations. Add new `DriverNotificationManager` API to attach or detach providers and `DriverNotificationManager.observeDriverNotification()` to handle the flow of driver notifications. [#8649](https://github.com/mapbox/navigation/pull/8649)
- Added default location providers. [#8886](https://github.com/mapbox/navigation/pull/8886)
- Added `EvBusyChargingStationNotificationProvider` to notify when the EV is charging station is busy and propose alternative route. [#9146](https://github.com/mapbox/navigation/pull/9146)
- Added experimental `NavigationPerformance#performanceTracingEnabled` which enables/disables internal performance trace sections. [#9110](https://github.com/mapbox/navigation/pull/9110)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.13.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.13.0-beta.1))
- Mapbox Navigation Native `v324.13.0-beta.1`
- Mapbox Core Common `v24.13.0-beta.1`
- Mapbox Java `v7.4.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v7.4.0))


## Navigation SDK Core Framework 3.7.0-beta.1 - 22 January, 2025
#### Features
- Add `MapboxRoadCamera::inOnRoute` flag which indicates if the roiad camera is on the current route. [#7666](https://github.com/mapbox/navigation/pull/7666)
- Add `MapboxRoadCamerasDisplayConfig::showOnlyOnRoute` config parameter to display only road cameras on the route. [#7666](https://github.com/mapbox/navigation/pull/7666)
- Added support for EV charge point tariffs accessible via `EvStation.tariffs`. [#7452](https://github.com/mapbox/navigation/pull/7452)
- New experimental property `LocationMatcherResult.correctedLocationData` is available. [#7852](https://github.com/mapbox/navigation/pull/7852)
- New experimental function `GraphAccessor.getAdasisEdgeAttributes()` is available. It returns ADAS attributes for the requested edge. [#7823](https://github.com/mapbox/navigation/pull/7823)
- Expose road type in the `MapboxRoadCamera` [#7625](https://github.com/mapbox/navigation/pull/7625)
- Added support for section control speed cameras. [#7472](https://github.com/mapbox/navigation/pull/7472)
- Extended `MapboxTripProgressApi` to provide information about time zone at leg/route destination. [#7674](https://github.com/mapbox/navigation/pull/7674)
- Added `TripProgressUpdateFormatter.getEstimatedTimeToArrival` overload that formats ETA using a given time zone. [#7674](https://github.com/mapbox/navigation/pull/7674)
- Added curvatures support on intersections in ADAS tiles [#7896](https://github.com/mapbox/navigation/pull/7896)
- Reduced amount of error logs  [#7896](https://github.com/mapbox/navigation/pull/7896)
- Added periodic logs of Navigator/Cache configs [#7896](https://github.com/mapbox/navigation/pull/7896)
- Added support for wheel speed usage during no signal simulation to determine passed distance for mobile profile [#7896](https://github.com/mapbox/navigation/pull/7896)
- Improved off road transitions [#7896](https://github.com/mapbox/navigation/pull/7896)
- :warning: Breaking changes in Experimental API `MapboxRouteCalloutView#renderCallouts(RouteCalloutData,MapboxRouteLineView)`. It's required to associate Route line with Callout View. [#7789](https://github.com/mapbox/navigation/pull/7789)
- Added experimental `SearchAlongRouteUtils` class to optimize search along routes scenario by providing optimally selected points. [#7857](https://github.com/mapbox/navigation/pull/7857)

#### Bug fixes and improvements
- Fixed a crash that happened on foreground service start on Android APIs 28 and below. [#7603](https://github.com/mapbox/navigation/pull/7603)
- Deprecated EstimatedTimeToArrivalFormatter and introduced EstimatedTimeOfArrivalFormatter, which allows to format ETA with respect to destination time zone. [#7696](https://github.com/mapbox/navigation/pull/7696)
- Deprecated TripProgressUpdateFormatter.estimatedTimeToArrivalFormatter and introduced TripProgressUpdateFormatter.estimatedTimeOfArrivalFormatter, which allows to format ETA with respect to destination time zone. [#7696](https://github.com/mapbox/navigation/pull/7696)
- Fixed `MapboxNavigationSDKInitializerImpl` logic so that `uxfKey` is properly retrieved and sent over. [#7663](https://github.com/mapbox/navigation/pull/7663)
- Fixed the condition for verifying the last good signal state in the offroad detection logic [#7896](https://github.com/mapbox/navigation/pull/7896)
- Fixed incorrect calculation of a "missing part" of the route causing all lanes to be mark as divergent [#7896](https://github.com/mapbox/navigation/pull/7896)
- Fixed EHorizon rural road objects sometimes marked as urban [#7896](https://github.com/mapbox/navigation/pull/7896)
- Fixed a bug that happened during reroute in case if initial route was requested with `approaches` option specified. [#7652](https://github.com/mapbox/navigation/pull/7652)

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
- Added experimental `RoutingTilesOptions#hdTilesOptions` to configure HD tiles endpoint. [#7171](https://github.com/mapbox/navigation/pull/7171)
- `DataInputsManager` now can be used from any thread. [#7384](https://github.com/mapbox/navigation/pull/7384)
- Added experimental Road Cameras modules to provide notifications about road cameras along the route and show them on the map. [#7149](https://github.com/mapbox/navigation/pull/7149)
- Added option to display the route line with a blur effect. [#6914](https://github.com/mapbox/navigation/pull/6914)
- Added experimental functions `MapboxNavigation#startTripSessionWithPermissionCheck()` and `MapboxNavigation#startReplayTripSessionWithPermissionCheck` that immediately throw `IllegalStateException` if they are called with `withForegroundService` parameter set to true, but Android foreground service permissions requirements are not met. [#7378](https://github.com/mapbox/navigation/pull/7378)

#### Bug fixes and improvements
- Improved reroute and alternative routes behavior [#7114](https://github.com/mapbox/navigation/pull/7114)
- Fixed map matching bug after leaving a tunnel [#7114](https://github.com/mapbox/navigation/pull/7114)
- Increased route stickiness in dead reckoning mode [#7114](https://github.com/mapbox/navigation/pull/7114)
- Added ability to send raw unfused GNSS location in addition to fused one [#7114](https://github.com/mapbox/navigation/pull/7114)
- Improved odometry and road graph fusing in urban canyons [#7114](https://github.com/mapbox/navigation/pull/7114)
- Signature of experimental `RawGnssSatelliteData` has been changed, now it requires `residual` as a constructor parameter [#7114](https://github.com/mapbox/navigation/pull/7114)
- Experimental `RawGnssLocation` type has been removed, now `RawGnssData` requires `DilutionOfPrecision` as a parameter [#7114](https://github.com/mapbox/navigation/pull/7114)
- Now service type is specified explicitly when foreground location service starts. [#7378](https://github.com/mapbox/navigation/pull/7378)

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
- Introduced support of Mapbox Map Matching API, see `MapboxNavigation#requestMapMatching`. [#2874](https://github.com/mapbox/navigation/pull/2874)
- Changed LocationOptions API. Now custom location providers that emit mocked locations are allowed. See documentation to get more information. [#3392](https://github.com/mapbox/navigation/pull/3392)
- Added Advanced Driver Assistance Systems (ADAS) functionality as an experimental API. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Added `NavigationViewApi#recenterCamera()` that allows to programatically recenter the camera position as if the recenter button was clicked. [#3489](https://github.com/mapbox/navigation/pull/3489)
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
- Fixed a bug with multiple instances of cache which resulted in excessive memory consumption. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Fixed an issue where reroute for multi-leg routes used to fail in case waypoint_names or waypoint_targets parameters were specified without an explicit waypoint_indices parameter. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Improved handling of no storage available during navigation tiles downloading. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Improved handling of invalid config in `DeviceProfile#customConfig`. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Fixed a native crash in E-Horizon implementation caused by internal race condition. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Made `MapboxReplayer` constructor public. [#3392](https://github.com/mapbox/navigation/pull/3392)
- Removed `OnlineRouteAlternativesSwitch`. Use `NavigationRouteAlternativesObserver` to receive an online alternative for the current offline route. Unlike `OnlineRouteAlternativesSwitch`, `NavigationRouteAlternativesObserver` doesn't switch to an online alternative automatically. [#3441](https://github.com/mapbox/navigation/pull/3441)
- Changed structure of `NavigationRoute`. Now it can represent routes received from Mapbox Map Matching API as well as Mapbox Directions API.
  `NavigationRoute#directionsResponse` has been removed. Use `NavigationRoute#waypoints` and `NavigationRoute#responseUUID` to access data which used to be available via `NavigationRoute#directionsResponse`.
  `NavigationRoute#routeOptions` has been removed. Try to utilise data available in `NavigationRoute`, for example instead of using coordinates from route options, use `NavigationRoute#waypoints`. Temporary property `NavigationRoute#evMaxCharge` has been added to access maximum possible charge for the vehicle the route was requested for instead of `navigationRoute.routeOptions.getUnrecognizedProperty("ev_max_charge")`.
  [#3441](https://github.com/mapbox/navigation/pull/3441)
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
- Improved `CustomRouterRule` to work independently from device network setup. [#3430](https://github.com/mapbox/navigation/pull/3430)
- Removed `CustomRouterRule#baseUrl`. Now `CustomRouterRule` intercepts all route and route refresh requests to Mapbox Directions API. [#3430](https://github.com/mapbox/navigation/pull/3430)
- Fixed a bug with multiple instances of cache which resulted in excessive memory consumption. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Fixed an issue where reroute for multi-leg routes used to fail in case waypoint_names or waypoint_targets parameters were specified without an explicit waypoint_indices parameter. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Improved Map Matching to avoid false deviation of the location puck to a parallel street. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Improved handling of no storage available during navigation tiles downloading. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Improved handling of invalid config in `DeviceProfile#customConfig`. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Fixed a native crash in E-Horizon implementation caused by internal race condition. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Made `MapboxReplayer` constructor public. [#3392](https://github.com/mapbox/navigation/pull/3392)
- Removed `OnlineRouteAlternativesSwitch`. Use `NavigationRouteAlternativesObserver` to receive an online alternative for the current offline route. Unlike `OnlineRouteAlternativesSwitch`, `NavigationRouteAlternativesObserver` doesn't switch to an online alternative automatically. [#3441](https://github.com/mapbox/navigation/pull/3441)
- Changed structure of `NavigationRoute`. Now it can represent routes received from Mapbox Map Matching API as well as Mapbox Directions API.
  `NavigationRoute#directionsResponse` has been removed. Use `NavigationRoute#waypoints`, `NavigationRoute#responseUUID`, and `NavigationRoute#responseMetadata` to access data which used to be available via `NavigationRoute#directionsResponse`.
  `NavigationRoute#routeOptions` has been removed. Try to utilise data available in `NavigationRoute`, for example instead of using coordinates from route options, use `NavigationRoute#waypoints`. Temporary property `NavigationRoute#evMaxCharge` has been added to access maximum possible charge for the vehicle the route was requested for instead of `navigationRoute.routeOptions.getUnrecognizedProperty("ev_max_charge")`.
  [#3441](https://github.com/mapbox/navigation/pull/3441)
- Added `MapMatchingSuccessfulResult#navigationRoutes` which represent routes that could be set to navigator using `MapboxNavigation#setNavigationRoutes`. [#3441](https://github.com/mapbox/navigation/pull/3441)
- Changed type of `MapMatchingSuccessfulResult#matches` from `List<NavigationRoute>` to `List<MapMatchingMatch>`. Now every item from `MapMatchingSuccessfulResult#matches` represents a Match Object from Mapbox Map Matching API. [#3441](https://github.com/mapbox/navigation/pull/3441)

## Features
- Changed LocationOptions API. Now custom location providers that emit mocked locations are allowed. In order to set a custom location provider, you now need to invoke `LocationOptions.Builder#locationProviderFactory` and pass a factory that will create a DeviceLocationProvider implementation based on a request parameter together will location provider type, which can be one of: REAL, MOCKED, MIXED. Note that if your provider can emit non-real locations, it must set `isMock` extra flag value to true for such locations. To set this flag, use:
```
Location.Builder#extra(Value.valueOf(hashMapOf(LocationExtraKeys.IS_MOCK to Value.valueOf(true/false))))
```
 [#3392](https://github.com/mapbox/navigation/pull/3392)
- Added Advanced Driver Assistance Systems (ADAS) functionality as an experimental API. [#3489](https://github.com/mapbox/navigation/pull/3489)
- Added `NavigationViewApi#recenterCamera()` that allows to programatically recenter the camera position as if the recenter button was clicked. [#3489](https://github.com/mapbox/navigation/pull/3489)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.1.0))
- Mapbox Navigation Native `v300.0.1`
- Mapbox Core Common `24.1.0`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))


## Navigation SDK Core Framework 1.0.0-beta.3 - 19 January, 2024
### Changelog

#### Bug fixes and improvements
- Support more options for `MapMatchingOptions`. [#3369](https://github.com/mapbox/navigation/pull/3369)
- Made `MapboxNavigation` constructor and `onDestroy` internal. To create an instance of `MapboxNavigation` use `MapboxNavigationProvider#create`; to destroy it use `MapboxNavigationProvider#destroy`. [3274](https://github.com/mapbox/navigation/pull/3274)

## Features
- Introduced support of Mapbox Map Matching API, see `MapboxNavigation#requestMapMatching`. [#2874](https://github.com/mapbox/navigation/pull/2874)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v11.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v11.1.0))
- Mapbox Navigation Native `v300.0.1`
- Mapbox Core Common `24.1.0`
- Mapbox Java `v6.15.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.15.0))
