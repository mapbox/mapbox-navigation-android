# Changelog for the Mapbox Navigation Android Auto SDK

Mapbox welcomes participation and contributions from everyone.

## Unreleased
#### Features
#### Bug fixes and improvements

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

#### Features
 - Added notification interceptor for Android Auto. [#5778](https://github.com/mapbox/mapbox-navigation-android/pull/5778)

#### Bug fixes and improvements
 - Remove extra arrival feedback options. [#5805](https://github.com/mapbox/mapbox-navigation-android/pull/5805)
 - Fixed an issue when the first voice instruction was not played. [#5825](https://github.com/mapbox/mapbox-navigation-android/pull/5825)

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
