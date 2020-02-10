<div align="center">
  <img src="https://github.com/flitsmeister/flitsmeister-navigation-android/blob/master/.github/splash-image.png" alt="Flitsmeister Navigation Android Splash">
</div>
<br>
<p align="center">
  <a href="https://jitpack.io/#flitsmeister/flitsmeister-navigation-android">
    <img src="https://jitpack.io/v/flitsmeister/flitsmeister-navigation-android.svg"
         alt="Jitpack">
  </a>
</p>

The Flitsmeister Navigation SDK for Android is built on a fork of the [Mapbox Navigation SDK v0.19](https://github.com/flitsmeister/flitsmeister-navigation-android/tree/v0.19.0) which is build on top of the [Mapbox Directions API](https://www.mapbox.com/directions) and contains logic needed to get timed navigation instructions.

With this SDK you can implement turn by turn navigation in your own Android app while hosting your own Map tiles and Directions API.

# Why have we forked

1. Mapbox decided to put a closed source component to their navigation SDK and introduced a non open source license. Flitsmeister wants an open source solution.
2. Mapbox decided to put telemetry in their SDK. We couldn't turn this off without adjusting the source.
3. We want to use the SDK without paying Mapbox for each MAU and without Mapbox API keys.

All issues are covered with this SDK. 

# What have we changed

- We completely removed the UI part from the SDK so it will only contain the logics for navigation and not the visuals.
- We upgraded the [Mapbox Map SDK](https://github.com/mapbox/mapbox-gl-native/tree/master/platform/android) to version 8.5.0.
- We upgraded the [Mapbox Core](https://github.com/mapbox/mapbox-events-android) to version 1.3.0.
- We upgraded the [NavigationRoute](https://github.com/flitsmeister/flitsmeister-navigation-android/blob/master/libandroid-navigation/src/main/java/com/mapbox/services/android/navigation/v5/navigation/NavigationRoute.java#L425) 
 with the possibility to add an intercepter to the request.
- We changed the [locationLayerPlugin](https://github.com/mapbox/mapbox-plugins-android) to the [location component](https://docs.mapbox.com/android/api/map-sdk/8.5.0/com/mapbox/mapboxsdk/location/LocationComponent.html)
- We updated the logic around the implementation of the locationEngine so it can be used with the new locationEngine from the [Mapbox SDK](https://github.com/mapbox/mapbox-gl-native/tree/master/platform/android).
- We removed the telemetry class from the project. Nothing is being send to Mapbox or Flitsmeister.

# Getting Started

If you are looking to include this inside your project, you have to follow the the following steps:

## gradle
Step 1. Add it in your root `build.gradle` at the end of repositories:
```
  allprojects {
    repositories {
      ...
      maven { url 'https://jitpack.io' }
    }
  }
```
Step 2. Add the dependency
```
  implementation 'com.github.flitsmeister:flitsmeister-navigation-android:v1.0.1'
```

## maven
Step 1. Add it in your root `build.gradle` at the end of repositories:
```
  <repositories>
    <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
    </repository>
  </repositories>
```
Step 2. Add the dependency
```
  <dependency>
     <groupId>com.github.flitsmeister</groupId>
     <artifactId>flitsmeister-navigation-android</artifactId>
     <version>v1.0.0</version>
  </dependency>
```

## sbt
Step 1. Add it in your `build.sbt` at the end of resolvers:
```
  resolvers += "jitpack" at "https://jitpack.io"
```
Step 2. Add the dependency
```
  libraryDependencies += "com.github.flitsmeister" % "flitsmeister-navigation-android" % "v1.0.1"	
```

## leiningen
Step 1. Add it in your `project.clj` at the end of repositories:
```
  :repositories [["jitpack" "https://jitpack.io"]]
```
Step 2. Add the dependency
```
  :dependencies [[com.github.flitsmeister/flitsmeister-navigation-android "v1.0.1"]]
```

To run the [sample code](#sample-code) on a device or emulator, include your [developer access token](https://www.mapbox.com/help/define-access-token/) in `developer-config.xml` found in the project. 

# Getting Help

- **Have a bug to report?** [Open an issue](https://github.com/flitsmeister/flitsmeister-navigation-android/issues). If possible, include the version of Flitsmeister Services, a full log, and a project that shows the issue.
- **Have a feature request?** [Open an issue](https://github.com/flitsmeister/flitsmeister-navigation-android/issues/new). Tell us what the feature should do and why you want the feature.

## <a name="sample-code">Sample code

We've added one [navigation example](https://github.com/flitsmeister/flitsmeister-navigation-android/tree/master/app/src/main/java/com/mapbox/services/android/navigation/testapp/) to this repo's test app. We are planning to add more to help you get started with the SDK and to inspire you.

In order to see the map or calculate a route you need your own Maptile and Direction services.

## Contributing

We welcome feedback, translations, and code contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

# License

[100% MIT License](LICENSE)


