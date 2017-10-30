## Changelog for the Mapbox Navigation SDK for Android

Mapbox welcomes participation and contributions from everyone.

### v0.7.0 - TBD

* Updated to Mapbox Java 3.0 [#373](https://github.com/mapbox/mapbox-navigation-android/pull/373)

### v0.6.3 -October 18, 2017

* significant reroute metric fixes [#348](https://github.com/mapbox/mapbox-navigation-android/pull/348)
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
