### About the Mapbox Navigation SDK for Android

When your users want to get from one location to another, don’t push them out of your application into a generic map application. Instead, keep them engaged with your application 100% of the time with in-app turn-by-turn navigation.

The Mapbox Navigation SDK for Android is built on top of [the Mapbox Directions API](https://github.com/mapbox/mapbox-java/blob/master/mapbox/libjava-services/src/main/java/com/mapbox/services/api/directions/v5/DirectionsService.java) and contains logic needed to get timed navigation instructions. We are designing new maps specifically for navigation that highlight traffic conditions and helpful landmarks. The calculations use the user's current location and compare it to the current route that the user's traversing to provide critical information at any given moment. _You control the entire experience, from the time your user chooses a destination to when they arrive._


[Our full Mapbox Navigation documentation](https://www.mapbox.com/android-docs/mapbox-navigation/0.1/navigation/)


### Installing the Navigation SDK


The snippet to add to your `build.gradle` to consume this SDK is the following:

```
// Mapbox Navigation SDK for Android
compile('com.mapbox.mapboxsdk:mapbox-android-navigation:0.1.0@aar') {
    transitive = true
}
```


### Using the Navigation SDK

<!---
Keep adding text...
-->

