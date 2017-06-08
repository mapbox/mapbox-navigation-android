## Changelog for the Mapbox Navigation SDK for Android

Mapbox welcomes participation and contributions from everyone.

### v0.3.1 - June 8, 2017
* Use AutoValue inside RouteProgress objects.
* Directly use direction distance measurements instead of calculating them.

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
