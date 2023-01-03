#### Features


#### Bug fixes and improvements
- :warning: Updated the `NavigationView` default navigation puck asset. [#6678](https://github.com/mapbox/mapbox-navigation-android/pull/6678)

  Previous puck can be restored by injecting `LocationPuck2D` with the `bearingImage` set to `com.mapbox.navigation.ui.maps.R.drawable.mapbox_navigation_puck_icon` drawable:
  ```kotlin
  navigationView.customizeViewStyles {
      locationPuckOptions = LocationPuckOptions.Builder(context)
          .defaultPuck(
              LocationPuck2D(
                  bearingImage = ContextCompat.getDrawable(
                      context,
                      com.mapbox.navigation.ui.maps.R.drawable.mapbox_navigation_puck_icon,
                  )
              )
          )
          .idlePuck(regularPuck(context))
          .build()
  }
  ```
- one more bugfix [#0000](https://github.com/mapbox/mapbox-navigation-android/pull/0000)
- Added guarantees that route progress with `RouteProgress#currentState == OFF_ROUTE` arrives earlier than `NavigationRerouteController#reroute` is called. [#6764](https://github.com/mapbox/mapbox-navigation-android/pull/6764)

#### Known issues :warning:


#### Other changes
