<div align="center">
  <a href="https://www.mapbox.com/android-docs/navigation/overview/"><img src="https://github.com/mapbox/mapbox-navigation-android/blob/master/.github/splash-img.png?raw=true" alt="Mapbox Service"></a>
</div>
<br>
<p align="center">
  <a href="https://maven-badges.herokuapp.com/maven-central/com.mapbox.mapboxsdk/mapbox-android-navigation">
    <img src="https://maven-badges.herokuapp.com/maven-central/com.mapbox.mapboxsdk/mapbox-android-navigation/badge.svg"
         alt="Maven Central">
  </a>
  <a href="https://circleci.com/gh/mapbox/mapbox-navigation-android">
    <img src="https://circleci.com/gh/mapbox/mapbox-navigation-android.svg?style=shield&circle-token=:circle-token">
  </a>
</p>

When your users want to get from one location to another, don’t push them out of your application into a generic map application. Instead, keep them engaged with your application 100% of the time with in-app turn-by-turn navigation.

The Mapbox Navigation SDK for Android is built on top of [the Mapbox Directions API](https://www.mapbox.com/directions) and contains logic needed to get timed navigation instructions.

The Mapbox Navigation SDK is a precise and flexible platform which enables your users to explore the world's streets. We are designing new maps specifically for navigation that highlight traffic conditions and helpful landmarks. The calculations use the user's current location and compare it to the current route that the user's traversing to provide critical information at any given moment. _You control the entire experience, from the time your user chooses a destination to when they arrive._


## Getting Started

If you are looking to include this inside your project, please take a look at [the detailed instructions](https://www.mapbox.com/android-docs/navigation/overview/) found in our docs. If you are interested in building from source, read the contributing guide inside this project.

The snippet to add to your `build.gradle` file to use this SDK is the following:

```
// Mapbox Navigation SDK for Android

compile 'com.mapbox.mapboxsdk:mapbox-android-navigation:0.10.0'

```

## Documentation

You'll find all of the documentation for this SDK on [our Mapbox Navigation page](https://www.mapbox.com/android-docs/navigation/overview/). This includes information on installation, using the API, and links to the API reference.

## Getting Help

- **Need help with your code?**: Look for previous questions on the [#mapbox tag](https://stackoverflow.com/questions/tagged/mapbox+android) — or [ask a new question](https://stackoverflow.com/questions/tagged/mapbox+android).
- **Have a bug to report?** [Open an issue](https://github.com/mapbox/mapbox-navigation-android/issues). If possible, include the version of Mapbox Services, a full log, and a project that shows the issue.
- **Have a feature request?** [Open an issue](https://github.com/mapbox/mapbox-navigation-android/issues/new). Tell us what the feature should do and why you want the feature.

## Using Snapshots

If you want to test recent bug fixes or features that have not been packaged in an official release yet, you can use a `-SNAPSHOT` release of the current development version of the Mapbox Navigation SDK via Gradle, available on [Sonatype](https://oss.sonatype.org/content/repositories/snapshots/com/mapbox/mapboxsdk/).

```gradle
repositories {
    mavenCentral()
    maven { url "http://oss.sonatype.org/content/repositories/snapshots/" }
}

dependencies {
    compile 'com.mapbox.mapboxsdk:mapbox-android-navigation:0.11.0-SNAPSHOT'
}
```

## Sample code

[We've added several navigation examples to this repo's test app](https://github.com/mapbox/mapbox-navigation-android/tree/master/app/src/main/java/com/mapbox/services/android/navigation/testapp/activity) to help you get started with the SDK and to inspire you.

## Translations

This project uses Transifex for translating the SDKs `string.xml` files. To help contribute or add support for a new language, visit the [Transifex project page](https://www.transifex.com/mapbox/mapbox-navigation-sdk-for-android/).
