# Changelog for the Mapbox Navigation Android Auto SDK

Mapbox welcomes participation and contributions from everyone.

## Unreleased
#### Features
#### Bug fixes and improvements
- Removed `MapboxNavigation` from `PlaceListOnMapScreen`. [#6371](https://github.com/mapbox/mapbox-navigation-android/pull/6371)
- Migrate `CarDistanceFormatter` to an object that works with `MapboxNavigationApp`. [#6401](https://github.com/mapbox/mapbox-navigation-android/pull/6401)

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
