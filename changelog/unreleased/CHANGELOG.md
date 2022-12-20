#### Features
- Test changes to check renaming [#6740](https://github.com/mapbox/mapbox-navigation-android/pull/6740)
- Introduced `MapboxSpeedInfoApi` and `MapboxSpeedInfoView`. The combination of API and View can be used to render posted and current speed limit at user's current location. [#6687](https://github.com/mapbox/mapbox-navigation-android/pull/6687)
- :warning: Deprecated `MapboxSpeedLimitApi` and `MapboxSpeedLimitView`. [#6687](https://github.com/mapbox/mapbox-navigation-android/pull/6687)

#### Bug fixes and improvements
- Fixed approaches list update in `RouteOptionsUpdater`(uses for reroute). It was putting to the origin approach corresponding approach from legacy approach list. [#6540](https://github.com/mapbox/mapbox-navigation-android/pull/6540)
- Updated the `MapboxRestAreaApi` logic to load a SAPA map only if the upcoming rest stop is at the current step of the route leg. [#6695](https://github.com/mapbox/mapbox-navigation-android/pull/6695)

#### Known issues :warning:
- It is an example of known issues [#example-known-issues](https://github.com/mapbox/mapbox-navigation-android/pull/example-known-issues)