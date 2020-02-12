## Changelog for the Mapbox Navigation SDK for Android

Mapbox welcomes participation and contributions from everyone.

### v1.0.0-beta.1 - January 24, 2020

### v0.43.0 -

* Fix Javadoc Kotlin issues [#2103](https://github.com/mapbox/mapbox-navigation-android/pull/2103)
* Fix intermittent memory issues in CI [#2096](https://github.com/mapbox/mapbox-navigation-android/pull/2096)
* Fix methods for recalculating waypoints coordinates, names, indices, approaches [#2066](https://github.com/mapbox/mapbox-navigation-android/pull/2066)

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
