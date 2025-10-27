# Changelog for the Mapbox Navigation Android Auto SDK

Mapbox welcomes participation and contributions from everyone.

## Unreleased
#### Features
#### Bug fixes and improvements

## androidauto-v0.23.0 - 27 October, 2025
### Changelog
[Changes between 0.22.0 and 0.23.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.22.0...androidauto-v0.23.0)

#### Features
- Added support for Android Auto module that supports Android 16 KB page-size devices. To consume SDK compatible with NDK 27 you need to add `-ndk27` suffix to the artifact name, for example, `com.mapbox.navigation:ui-androidauto` -> `com.mapbox.navigation:ui-androidauto-ndk27`

#### Bug fixes and improvements


### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.6.1` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.6.1))


## androidauto-v0.22.0 - 16 March, 2023
### Changelog
[Changes between 0.21.0 and 0.22.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.21.0...androidauto-v0.22.0)

#### Features


#### Bug fixes and improvements
- Fixed PlacesListOnMapScreen issues when screen changed cause places to disappear [#7021](https://github.com/mapbox/mapbox-navigation-android/pull/7021)
- Fixed an issue that caused route preview screens to display incorrect route distance. [#7031](https://github.com/mapbox/mapbox-navigation-android/pull/7031)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.5.0))
- Mapbox Navigation `v2.10.3` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.10.3))


## androidauto-v0.21.0 - 06 March, 2023
### Changelog
[Changes between 0.20.0 and 0.21.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.20.0...androidauto-v0.21.0)

#### Bug fixes and improvements
- Fixed an issue with the experimental route preview screen that caused template restrictions violations. [#7014](https://github.com/mapbox/mapbox-navigation-android/pull/7014)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.5.0))
- Mapbox Navigation `v2.10.3` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.10.3))


## androidauto-v0.20.0 - 06 March, 2023
### Changelog
[Changes between 0.19.0 and 0.20.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.19.0...androidauto-v0.20.0)

#### Features


#### Bug fixes and improvements
- Bumped Navigation and Search versions to 2.10.3 and 1.0.0-rc.1 respectively. [#7000](https://github.com/mapbox/mapbox-navigation-android/pull/7000)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.5.0))
- Mapbox Navigation `v2.10.3` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.10.3))


## androidauto-v0.19.0 - 31 January, 2023
### Changelog
[Changes between 0.18.1 and 0.19.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.18.1...androidauto-v0.19.0)

#### Features
- Added experimental route preview screen that uses route preview feature from Nav SDK. It can be enabled using `prepareExperimentalRoutePreviewScreen` extension. [#6875](https://github.com/mapbox/mapbox-navigation-android/pull/6875)
- Added support for Junction Views. [#6849](https://github.com/mapbox/mapbox-navigation-android/pull/6849)

#### Bug fixes and improvements
- Optimized `SpeedLimitWidget` memory usage. [#6859](https://github.com/mapbox/mapbox-navigation-android/pull/6859)
- Fixed an issue with `CarRouteLineRenderer` that caused route arrows to be rendered above the location puck. [#6921](https://github.com/mapbox/mapbox-navigation-android/pull/6921)
- Updated `CarActiveGuidanceMarkers` to support different route providers. This makes it possible to use the class in route preview screens. [#6873](https://github.com/mapbox/mapbox-navigation-android/pull/6873)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.5.0))
- Mapbox Navigation `v2.10.1` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.10.1))


## androidauto-v0.18.1 - 12 December, 2022
### Changelog
[Changes between 0.18.0 and 0.18.1](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.18.0...androidauto-v0.18.1)

#### Features
#### Bug fixes and improvements
- Fixed incompatibility with nav sdk. [2.10.0-beta.3](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.10.0-beta.3). Android Auto head unit will crash immediately. [#6714](https://github.com/mapbox/mapbox-navigation-android/pull/6714)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.5.0))
- Mapbox Navigation `v2.9.3` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.9.3))


## androidauto-v0.18.0 - 09 December, 2022
### Changelog
[Changes between 0.17.1 and 0.18.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.17.1...androidauto-v0.18.0)

#### Features
- Added customizable `ActionStrip`s for `MapboxScreen`s with `MapboxScreenActionStripProvider` found in the `MapboxCarOptions`. [#6681](https://github.com/mapbox/mapbox-navigation-android/pull/6681)
#### Bug fixes and improvements
- Renamed `CarAudioGuidanceUi` to `CarAudioGuidanceAction`. [#6681](https://github.com/mapbox/mapbox-navigation-android/pull/6681)
- Deleted `MapboxActionProvider` interface in favor of `MapboxScreenActionStripProvider`. [#6681](https://github.com/mapbox/mapbox-navigation-android/pull/6681)
- Fixed issue where `CarAudioGuidanceUi` would invalidate the screen so often that performance would be effected. [#6681](https://github.com/mapbox/mapbox-navigation-android/pull/6681)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.5.0))
- Mapbox Navigation `v2.9.3` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.9.3))


## androidauto-v0.17.1 - 02 December, 2022
### Changelog
[Changes between 0.17.0 and 0.17.1](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.17.0...androidauto-v0.17.1)

#### Features
#### Bug fixes and improvements
- Fixed issue when `NavigationManager.updateTrip` crashes because navigation is not started https://issuetracker.google.com/u/0/issues/260968395. [#6673](https://github.com/mapbox/mapbox-navigation-android/pull/6673)
- Fixed AAOS Google Play release issue. Previously the AndroidManifest was specific to AA. Now you must specify the `meta-data` type depending on whether you are using AA or AAOS. [#6679](https://github.com/mapbox/mapbox-navigation-android/pull/6679)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.5.0))
- Mapbox Navigation `v2.9.3` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.9.3))


## androidauto-v0.17.0 - 30 November, 2022
### Changelog
[Changes between 0.16.0 and 0.17.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.16.0...androidauto-v0.17.0)

#### Features
#### Bug fixes and improvements
- Added `MapboxCarMapLoader.getStyleExtension` to get access to the values set. [#6571](https://github.com/mapbox/mapbox-navigation-android/pull/6571)
- Removed `@MapboxExperimental` from `MapboxCarMapObserver` implementations. [#6588](https://github.com/mapbox/mapbox-navigation-android/pull/6588)
- Renamed `CarCompassSurfaceRenderer` to `CarCompassRenderer`. [#6588](https://github.com/mapbox/mapbox-navigation-android/pull/6588)
- Renamed `CarLogoSurfaceRenderer` to `CarLogoRenderer`. [#6588](https://github.com/mapbox/mapbox-navigation-android/pull/6588)
- Renamed `RoadLabelSurfaceLayer` to `CarRoadLabelRenderer`. [#6588](https://github.com/mapbox/mapbox-navigation-android/pull/6588)
- Renamed `CarRouteLine` to `CarRouteLineRenderer`. [#6588](https://github.com/mapbox/mapbox-navigation-android/pull/6588)
- Renamed `RoadLabelOptions` to `CarRoadLabelOptions`. [#6588](https://github.com/mapbox/mapbox-navigation-android/pull/6588)
- Renamed `RoadLabelRenderer` to `CarRoadLabelRenderer`. [#6588](https://github.com/mapbox/mapbox-navigation-android/pull/6588)
- Made `ActiveGuidanceScreen` internal in favor of `MapboxScreen.ACTIVE_GUIDANCE`. [#6588](https://github.com/mapbox/mapbox-navigation-android/pull/6588)
- Made `NeedsLocationPermissionsScreen` internal in favor of `MapboxScreen.NEEDS_LOCATION_PERMISSION`. [#6588](https://github.com/mapbox/mapbox-navigation-android/pull/6588)
- Fixed race condition where `NavigationManager.updateTrip` can be called after `navigationEnded` is called. This is a likely cause to a crash seen. [#6661](https://github.com/mapbox/mapbox-navigation-android/pull/6661)
 
### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.5.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.5.0))
- Mapbox Navigation `v2.9.2` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.9.2))


## androidauto-v0.16.0 - 04 November, 2022
### Changelog
[Changes between 0.15.0 and 0.16.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.15.0...androidauto-v0.16.0)

#### Features
- Added `MapboxCarMapLoader` to handle dark and light map style configuration changes. [#6530](https://github.com/mapbox/mapbox-navigation-android/pull/6530)

#### Bug fixes and improvements
- Moved `surfacelayer` package into an internal package because it will be replaced by `BitmapWidget`. [#6527](https://github.com/mapbox/mapbox-navigation-android/pull/6527)
- Flattened the `car` package because it is adding an unnecessary and confusing layer. [#6527](https://github.com/mapbox/mapbox-navigation-android/pull/6527)
- Removed `Fragment.attachAudioGuidance` extension function because it will be replaced by `ComponentInstaller`. [#6527](https://github.com/mapbox/mapbox-navigation-android/pull/6527)
- Fixed an issue with compass, logo and speed limit widgets that caused them to disappear if road label widget was not added to the same screen. [#6541](https://github.com/mapbox/mapbox-navigation-android/pull/6541)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.3.0))
- Mapbox Search `v1.0.0-beta.39` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.39))
- Mapbox Navigation `v2.9.0-rc.2` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.9.0-rc.2))


## androidauto-v0.15.0 - October 21, 2022
### Changelog
[Changes between 0.14.0 and 0.15.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.14.0...androidauto-v0.15.0)

#### Features
- Added a new `MapboxCarOptions` that contains mutable options for `MapboxCarContext`. [#6478](https://github.com/mapbox/mapbox-navigation-android/pull/6478)
- Added a new `MapboxCarOptionsCustomization` that allows you to change the `MapboxCarOptions`. [#6478](https://github.com/mapbox/mapbox-navigation-android/pull/6478)

#### Bug fixes and improvements
- Removed options from the `MapboxCarContext` constructor so that it can be compatible with future changes. [#6478](https://github.com/mapbox/mapbox-navigation-android/pull/6478)
- Deleted `RoutePreviewCarContext` in favor of `MapboxCarContext`. [#6478](https://github.com/mapbox/mapbox-navigation-android/pull/6478)
- Renamed `CarSettingsStorage` to `MapboxCarStorage`. [#6478](https://github.com/mapbox/mapbox-navigation-android/pull/6478)
- Deleted `ActionProvider` and `ScreenActionProvider` in favor of `MapboxActionProvider`. [#6494](https://github.com/mapbox/mapbox-navigation-android/pull/6494)
- Renamed `CarAppLocation` to `CarLocationProvider` with a `getRegisteredInstance` accessor. [#6492](https://github.com/mapbox/mapbox-navigation-android/pull/6492)
- Removed `MapboxCarApp` as it is no longer needed. Use `CarLocationProvider.getRegisteredInstance()` if needed. [#6492](https://github.com/mapbox/mapbox-navigation-android/pull/6492)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.3.0))
- Mapbox Navigation `v2.9.0-beta.3` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.9.0-beta.3))
- Mapbox Search `v1.0.0-beta.38.1` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.38.1))

## androidauto-v0.14.0 - October 13, 2022
### Changelog
[Changes between 0.13.0 and 0.14.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.13.0...androidauto-v0.14.0)

#### Features
- Added a new `MapboxScreenManager` framework. This makes it possible to build custom user experiences that are drastically different than the default provided by the SDK. [#6429](https://github.com/mapbox/mapbox-navigation-android/pull/6429)
- Added a default `MapboxScreenGraph` that prepares an end to end navigation experience. [#6429](https://github.com/mapbox/mapbox-navigation-android/pull/6429)

#### Bug fixes and improvements
- Deleted `CarAppState` in favor of a `MapboxScreenEvent` which can be accessed through `MapboxScreenManager.screenEvent`. [#6429](https://github.com/mapbox/mapbox-navigation-android/pull/6429)
- Replaced all public implementations of `Screen` with backwards compatible implementations of `MapboxScreenFactory` and `MapboxScreen`. [#6429](https://github.com/mapbox/mapbox-navigation-android/pull/6429)
- Renamed `MainCarContext` to `MapboxCarContext`. [#6429](https://github.com/mapbox/mapbox-navigation-android/pull/6429)
- Renamed `MainCarScreen` to `FreeDriveCarScreen`. [#6429](https://github.com/mapbox/mapbox-navigation-android/pull/6429)
- Removed all public apis with `com.mapbox.search` while the SDK is in beta. [#6429](https://github.com/mapbox/mapbox-navigation-android/pull/6429)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.3.0))
- Mapbox Navigation `v2.9.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.9.0-beta.1))
- Mapbox Search `v1.0.0-beta.37` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.37))

## androidauto-v0.13.0 - October 6, 2022
### Changelog
[Changes between 0.12.0 and 0.13.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.12.0...androidauto-v0.13.0)

#### Bug fixes and improvements
- Removed `MapboxNavigation` from `PlaceListOnMapScreen`. [#6371](https://github.com/mapbox/mapbox-navigation-android/pull/6371)
- Migrate `CarDistanceFormatter` to an object that works with `MapboxNavigationApp`. [#6401](https://github.com/mapbox/mapbox-navigation-android/pull/6401)
- Removed `MainCarContext` from `CarRouteLine` and `CarLocationRenderer` constructor. [#6406](https://github.com/mapbox/mapbox-navigation-android/pull/6406)
- Fixed issue where style changes cause `CarRouteLine` to trigger excessive calls to the map style. [#6406](https://github.com/mapbox/mapbox-navigation-android/pull/6406)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.3.0))
- Mapbox Navigation `v2.9.0-beta.1` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.9.0-beta.1))
- Mapbox Search `v1.0.0-beta.37` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.37))

## androidauto-v0.12.0 - Sep 26, 2022
### Changelog
[Changes between 0.11.0 and 0.12.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.11.0...androidauto-v0.12.0)

#### Features
#### Bug fixes and improvements
- Use `MapboxAudioGuidance` from public api. [#6336](https://github.com/mapbox/mapbox-navigation-android/pull/6336)
- Fixed an issue that caused camera animations to interfere with user gestures. [#6357](https://github.com/mapbox/mapbox-navigation-android/pull/6357)
- Marked `CarFeedbackSender`, `CarGridFeedbackScreen`, `CarLocationsOverviewCamera`, `CarNavigationCamera`, `PlacesListItemClickListener`, `PlacesListOnMapLayerUtil`, `PlacesListOnMapScreen`, `CarRoutePreviewScreen`, `CarRouteRequest`, `PlaceSearchScreen`, `MainCarScreen`, `AppAudioGuidanceUtil`, `MapboxCarApp` methods with `@UiThread` annotation. [#6267](https://github.com/mapbox/mapbox-navigation-android/pull/6267)
- Removed `MapboxNavigation` from `CarRouteRequest` constructor. [#6372](https://github.com/mapbox/mapbox-navigation-android/pull/6372)
- Removed Search SDK as an inherited library. You must include search directly in order to access the api. [#6388](https://github.com/mapbox/mapbox-navigation-android/pull/6388)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.3.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.3.0))
- Mapbox Navigation `v2.9.0-alpha.3` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.9.0-alpha.3))
- Mapbox Search `v1.0.0-beta.36` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.36))

## androidauto-v0.11.0 - Sep 16, 2022
### Changelog
[Changes between 0.10.0 and 0.11.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.10.0...androidauto-v0.11.0)

#### Features
- Added `CarArrivalTrigger` and `CarActiveGuidanceMarkers` for logical components that help build the `ActiveGuidanceScreen`. [#6326](https://github.com/mapbox/mapbox-navigation-android/pull/6326)
#### Bug fixes and improvements
- Remove experimental from `MapboxCarNavigationManager` and showcase java. [#6292](https://github.com/mapbox/mapbox-navigation-android/pull/6292)
- Removed `MapboxNavigation` from `CarNavigationInfoObserver` constructor, and rename to `CarNavigationInfoProvider`. Removed dependencies from `CarActiveGuidanceCarContext` that require `MapboxNavigation`. [#6224](https://github.com/mapbox/mapbox-navigation-android/pull/6224)
- Removed `MapboxNavigation` from `CarSpeedLimitRenderer` constructor. [#6325](https://github.com/mapbox/mapbox-navigation-android/pull/6325)
- Deleted `CarActiveGuidanceCarContext` because it is no longer needed after `MapboxNavigation` is removed. [#6326](https://github.com/mapbox/mapbox-navigation-android/pull/6326)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.2.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.2.0))
- Mapbox Navigation `v2.8.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.8.0-beta.3))
- Mapbox Search `v1.0.0-beta.36` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.35))

## androidauto-v0.10.0 - Sep 9, 2022
### Changelog
[Changes between 0.9.0 and 0.10.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.9.0...androidauto-v0.10.0)

#### Features
- Added support for MUTCD and Vienna designs in the Speed limit widget. [#6275](https://github.com/mapbox/mapbox-navigation-android/pull/6275)
- Added options to customize behavior of the Speed limit widget. [#6275](https://github.com/mapbox/mapbox-navigation-android/pull/6275)

#### Bug fixes and improvements
- Bump minimum version nav-sdk requirement to 2.8.0-beta.3. [#6305](https://github.com/mapbox/mapbox-navigation-android/pull/6305)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.2.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.2.0))
- Mapbox Navigation `v2.8.0-beta.3` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.8.0-beta.3))
- Mapbox Search `v1.0.0-beta.35` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.35))

## androidauto-v0.9.0 - Sep 1, 2022
### Changelog
[Changes between 0.8.0 and 0.9.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.8.0...androidauto-v0.9.0)

#### Features
- Added support for remote icons in `CarGridFeedbackScreen`. [#6227](https://github.com/mapbox/mapbox-navigation-android/pull/6227)

#### Bug fixes and improvements
- Removed `MapboxNavigation` from `RoadNameObserver` and `RoadLabelSurfaceLayer` constructor. [#6224](https://github.com/mapbox/mapbox-navigation-android/pull/6224)
- Removed `MapboxNavigation` form `CarLocationsOverviewCamera` constructor. [#6225](https://github.com/mapbox/mapbox-navigation-android/pull/6225)
- Removed `MapboxNavigation` from `CarNavigationCamera` and `CarRouteLine` constructors. [#6219](https://github.com/mapbox/mapbox-navigation-android/pull/6219)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.2.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.2.0))
- Mapbox Navigation `v2.8.0-beta.2` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.8.0-beta.2))
- Mapbox Search `v1.0.0-beta.35` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.35))

## androidauto-v0.8.0 - Aug 18, 2022
### Changelog
[Changes between 0.7.0 and 0.8.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.7.0...androidauto-v0.8.0)

#### Bug fixes and improvements
- Added `CarPlaceSearchOptions` to `MainCarContext` so that place search can have a stable api. [#6165](https://github.com/mapbox/mapbox-navigation-android/pull/6165)
- Deleted `MapboxCarSearchApp` because it is no longer needed. [#6165](https://github.com/mapbox/mapbox-navigation-android/pull/6165)
- Replaced `CarSearchEngine` with `CarPlaceSearch` and move the implementation to public internal package. [#6165](https://github.com/mapbox/mapbox-navigation-android/pull/6165)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.2.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.2.0))
- Mapbox Navigation `v2.7.0-rc.2` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.7.0-rc.2))
- Mapbox Search `v1.0.0-beta.34` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.34))

## androidauto-v0.7.0 - Aug 12, 2022
### Changelog
[Changes between 0.6.0 and 0.7.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.6.0...androidauto-v0.7.0)

#### Bug fixes and improvements
- Added metalava api tracking. Removed `AndroidAutoLog` and `RendererUtils` from public api. [#6130](https://github.com/mapbox/mapbox-navigation-android/pull/6130)
- Bumped maps extension version to fix AAOS issue when changing screens. [#6139](https://github.com/mapbox/mapbox-navigation-android/pull/6139)
- Added an option to override items displayed in the feedback screen. [#6144](https://github.com/mapbox/mapbox-navigation-android/pull/6144)
- Fixed an issue with alternative route line not vanishing. [#6153](https://github.com/mapbox/mapbox-navigation-android/pull/6153)
- Added an option to override route options used for route requests. [#6153](https://github.com/mapbox/mapbox-navigation-android/pull/6153)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.2.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.2.0))
- Mapbox Navigation `v2.7.0-rc.2` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.7.0-rc.2))
- Mapbox Search `v1.0.0-beta.34` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.34))

## androidauto-v0.6.0 - Jul 29, 2022
### Changelog
[Changes between 0.5.0 and 0.6.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.5.0...androidauto-v0.6.0)

#### Bug fixes and improvements
- Added an option to hide or show alternative routes during route preview and active guidance. [#6100](https://github.com/mapbox/mapbox-navigation-android/pull/6100)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.1.0))
- Mapbox Navigation `v2.7.0-beta.3` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.7.0-beta.3))
- Mapbox Search `v1.0.0-beta.34` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.34))

## androidauto-v0.5.0 - Jul 22, 2022
### Changelog
[Changes between 0.4.0 and 0.5.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.4.0...androidauto-v0.5.0)

#### Bug fixes and improvements
- Fixed an issue with speed limit widget that caused it to turn into a back rectangle. [#6064](https://github.com/mapbox/mapbox-navigation-android/pull/6064)
- Fixed an issue with compass and logo widgets that caused them to draw behind location puck. [#6076](https://github.com/mapbox/mapbox-navigation-android/pull/6076)
- Added telemetry to determine when the car head unit has been started or stopped. [#6084](https://github.com/mapbox/mapbox-navigation-android/pull/6084)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.1.0))
- Mapbox Navigation `v2.7.0-beta.2` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.7.0-beta.2))
- Mapbox Search `v1.0.0-beta.33` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.33))

## androidauto-v0.4.0 - Jul 12, 2022
### Changelog
[Changes between 0.3.0 and 0.4.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.3.0...androidauto-v0.4.0)

#### Bug fixes and improvements
- Added an option to inject your own SearchEngine. [#6042](https://github.com/mapbox/mapbox-navigation-android/pull/6042)
- Reverted `MainScreenManager` refactor due to incompatibility with Drop-In UI. [#6043](https://github.com/mapbox/mapbox-navigation-android/pull/6043)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.1.0))
- Mapbox Navigation `v2.7.0-alpha.3` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.7.0-alpha.3))
- Mapbox Search `v1.0.0-beta.33` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.33))

## androidauto-v0.3.0 - Jun 24, 2022
### Changelog
[Changes between 0.2.0 and 0.3.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.2.0...androidauto-v0.3.0)

#### Bug fixes and improvements
- Updated to use shared `MapboxAudioGuidance` instance. [#5846](https://github.com/mapbox/mapbox-navigation-android/pull/5846)
- Added map style observers to handle style changes. [#5853](https://github.com/mapbox/mapbox-navigation-android/pull/5853)
- Updated to format the distance displayed in the instrument cluster. [#5928](https://github.com/mapbox/mapbox-navigation-android/pull/5928)
- Refactored `MainScreenManager` to be `MapboxScreenManager` with customizable `MapboxScreenProvider`. [#5866](https://github.com/mapbox/mapbox-navigation-android/pull/5866)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.1.0))
- Mapbox Navigation `v2.6.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.6.0-rc.1))
- Mapbox Search `v1.0.0-beta.29` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.29))

## androidauto-v0.2.0 - May 19, 2022
### Changelog
[Changes between 0.1.0 and 0.2.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.1.0...androidauto-v0.2.0)

#### Features
- Added a notification interceptor for Android Auto. [#5778](https://github.com/mapbox/mapbox-navigation-android/pull/5778)

#### Bug fixes and improvements
- Remove extra arrival feedback options. [#5805](https://github.com/mapbox/mapbox-navigation-android/pull/5805)
- Fixed an issue when the first voice instruction was not played. [#5825](https://github.com/mapbox/mapbox-navigation-android/pull/5825)

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.1.0))
- Mapbox Navigation `v2.5.0-rc.1` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.5.0-rc.1))
- Mapbox Search `v1.0.0-beta.29` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.29))

## androidauto-v0.1.0 - May 05, 2022

This is the initial launch of the Mapbox Navigation Android Auto SDK Developer Preview.

#### Features
 - Free drive screen: `MainCarScreen`
 - Search for places: `SearchScreen`
 - List places on map: `PlaceListOnMapScreen`
 - Route preview: `CarRoutePreviewScreen`
 - Active guidance screen: `ActiveGuidanceScreen`
   - Lane guidance images: `CarLanesImageRenderer`
   - Mapping Mapbox Directions into Android Auto Maneuvers: `CarManeuverMapper`
   - Current road label on the map: `RoadLabelSurfaceLayer`
   - Speed limit view on the map: `SpeedLimitWidget`
   - Audio guidance: `MapboxAudioGuidance`
   - Camera that follows the location puck: `CarNavigationCamera`
 - Navigate from voice commands: `GeoDeeplinkNavigateAction`
 - Location puck: `CarLocationPuck`
 - Gestures on the map: `MainMapActionStrip`
 - Feedback items for every screen: `CarGridFeedbackScreen`
 - Update Android Auto `NavigationManager`: `MapboxCarNavigationManager`

### Mapbox dependencies
This release defines minimum versions for the Mapbox dependencies.
- Mapbox Maps Android Auto Extension `v0.1.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/extension-androidauto-v0.1.0))
- Mapbox Navigation `v2.4.1` ([release notes](https://github.com/mapbox/mapbox-navigation-android/releases/tag/v2.4.1))
- Mapbox Search `v1.0.0-beta.26` ([release notes](https://github.com/mapbox/mapbox-search-android/releases/tag/v1.0.0-beta.26))
