# Changelog for the Mapbox Navigation Android Auto SDK

Mapbox welcomes participation and contributions from everyone.

## Unreleased
#### Features
#### Bug fixes and improvements
- Updated to use shared `MapboxAudioGuidance` instance. [#5846](https://github.com/mapbox/mapbox-navigation-android/pull/5846)
- Added map style observers to handle style changes. [#5853](https://github.com/mapbox/mapbox-navigation-android/pull/5853)

## androidauto-v0.2.0 - May 19, 2022
### Changelog
[Changes between 0.1.0 and 0.2.0](https://github.com/mapbox/mapbox-navigation-android/compare/androidauto-v0.1.0...androidauto-v0.1.0)

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
