<div align="center">
  <a href="https://www.mapbox.com/android-docs/navigation/overview/"><img src="https://github.com/mapbox/mapbox-navigation-android/blob/main/.github/splash-img.png?raw=true" alt="Mapbox Service"></a>
</div>
<br>
<p align="center">
  <a href="https://circleci.com/gh/mapbox/mapbox-navigation-android">
    <img src="https://circleci.com/gh/mapbox/mapbox-navigation-android.svg?style=shield&circle-token=:circle-token">
  </a>
  <a href="https://codecov.io/gh/mapbox/mapbox-navigation-android">
    <img src="https://codecov.io/gh/mapbox/mapbox-navigation-android/branch/main/graph/badge.svg">
  </a>
</p>

When your users want to get from one location to another, don’t push them out of your application into a generic map application. Instead, keep them engaged with your application 100% of the time with in-app turn-by-turn navigation.

The Mapbox Navigation SDK for Android is built on top of [the Mapbox Directions API](https://www.mapbox.com/directions) and contains logic needed to get timed navigation instructions.

The Mapbox Navigation SDK is a precise and flexible platform which enables your users to explore the world's streets. We are designing new maps specifically for navigation that highlight traffic conditions and helpful landmarks. The calculations use the user's current location and compare it to the current route that the user's traversing to provide critical information at any given moment. _You control the entire experience, from the time your user chooses a destination to when they arrive._

## Getting Started

**NOTE:** On June 3rd, 2020, Mapbox released the `1.0` version of the Navigation SDK. The Mapbox team recommends that you build your navigation project with a `1.0` version or higher.

[Here are `1.0` installation instructions](https://docs.mapbox.com/android/beta/navigation/overview/#installation) and full documentation can be found along the sidebar sections of https://docs.mapbox.com/android/beta/navigation/overview.

Along with the full documentation, [this migration guide](https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide) can help you transition your project from a "legacy" version of the Navigation SDK (`0.42.6` or below) to a `1.0` version or higher.

Please see [this documentation link](https://docs.mapbox.com/android/navigation/overview/) if you're looking for information on the "legacy" pre-`1.0.0` Navigation SDK and you don't plan on migrating your project to `1.0.0` or higher.

## Getting Help

- **Need help with your code?**: Look for previous questions on the [#mapbox tag](https://stackoverflow.com/questions/tagged/mapbox+android) — or [ask a new question](https://stackoverflow.com/questions/tagged/mapbox+android).
- **Have a bug to report?** [Open an issue](https://github.com/mapbox/mapbox-navigation-android/issues). If possible, include the version of Mapbox Services, a full log, and a project that shows the issue.
- **Have a feature request?** [Open an issue](https://github.com/mapbox/mapbox-navigation-android/issues/new). Tell us what the feature should do and why you want the feature.

## Using Snapshots

You can use a `-SNAPSHOT` release if you want to test recent bug fixes or features that have not been packaged in an official release yet.

##### `1.0.0`+ versions of the Navigation SDK:

To access SNAPSHOT builds follow the [installation instructions](https://docs.mapbox.com/android/beta/navigation/overview/#installation) but replace the repository url and the version name:
```groovy
allprojects {
   repositories {
     maven {
       url 'https://api.mapbox.com/downloads/v2/snapshots/maven'
       authentication {
         basic(BasicAuthentication)
       }
       credentials {
         username = "mapbox"
         password = "{secret Mapbox token with DOWNLOADS:READ scope}"
       }
     }
   }
}

dependencies {
   implementation 'com.mapbox.navigation:ui:1.2.0-SNAPSHOT'
}
```

## <a name="sample-code">Sample code

[We've added several navigation examples to this repo's `examples` module](https://github.com/mapbox/mapbox-navigation-android/tree/main/examples) to help you get started with the Navigation SDK and to inspire you.

## Contributing

We welcome feedback, translations, and code contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.
