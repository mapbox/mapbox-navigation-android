## Changelog for the Mapbox Navigation SDK for Android

Mapbox welcomes participation and contributions from everyone.

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

* Fix NPE with reroute metric events [#565](https://github.com/mapbox/mapbox-navigation-android/pull/565)
* Adjust metric listener reset [#566](https://github.com/mapbox/mapbox-navigation-android/pull/566)
* Update distance completed in off-route scenario [#568](https://github.com/mapbox/mapbox-navigation-android/pull/568)
* Update Maps SDK to `5.2.1` [#570](https://github.com/mapbox/mapbox-navigation-android/pull/570)

### v0.7.1-beta.1 - December 1, 2017

* Expanded the width of route lines when zoomed out
* Added support for displaying alternative routes on map
* Adds exclude, voiceUnits, and banner instruction info to request/response [#500](https://github.com/mapbox/mapbox-navigation-android/pull/500)
* Add Imperial / Metric support for UI & Notification [#501](https://github.com/mapbox/mapbox-navigation-android/pull/501)
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
