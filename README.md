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
  <a href="https://codecov.io/gh/mapbox/mapbox-navigation-android">
    <img src="https://codecov.io/gh/mapbox/mapbox-navigation-android/branch/master/graph/badge.svg">
  </a>
</p>

When your users want to get from one location to another, don’t push them out of your application into a generic map application. Instead, keep them engaged with your application 100% of the time with in-app turn-by-turn navigation.

The Mapbox Navigation SDK for Android is built on top of [the Mapbox Directions API](https://www.mapbox.com/directions) and contains logic needed to get timed navigation instructions.

The Mapbox Navigation SDK is a precise and flexible platform which enables your users to explore the world's streets. We are designing new maps specifically for navigation that highlight traffic conditions and helpful landmarks. The calculations use the user's current location and compare it to the current route that the user's traversing to provide critical information at any given moment. _You control the entire experience, from the time your user chooses a destination to when they arrive._


## Getting Started

If you are looking to include this inside your project, please take a look at [the detailed instructions](https://www.mapbox.com/android-docs/navigation/overview/) found in our docs. If you are interested in building from source, read the contributing guide inside this project.

Add this snippet to your `build.gradle` file to use this SDK (`libandroid-navigation`):

```
implementation 'com.mapbox.mapboxsdk:mapbox-android-navigation:0.42.5'
```

And for `libandroid-navigation-ui`:

```
implementation 'com.mapbox.mapboxsdk:mapbox-android-navigation-ui:0.42.5'
```
**Note**:  When using the UI library, you _do not_ need to add both dependencies.  The UI library will automatically pull in `libandroid-navigation`.

**Important Note**: You _must_ include the following snippet in your top project-level `build.gradle` file:
```
repositories {
    maven { url 'https://mapbox.bintray.com/mapbox' }
}
```

This will ensure the `mapbox` dependency is properly downloaded.

To run the [sample code](#sample-code) on a device or emulator, include your [developer access token](https://www.mapbox.com/help/define-access-token/) in `developer-config.xml` found in the project. 
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
    implementation 'com.mapbox.mapboxsdk:mapbox-android-navigation:0.43.0-SNAPSHOT'
}
```

## <a name="sample-code">Sample code

[We've added several navigation examples to this repo's test app](https://github.com/mapbox/mapbox-navigation-android/tree/master/app/src/main/java/com/mapbox/services/android/navigation/testapp/activity) to help you get started with the SDK and to inspire you.

## Contributing

We welcome feedback, translations, and code contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License of Dependencies

This SDK uses [Mapbox Navigator](https://github.com/mapbox/mapbox-navigation-android/blob/45b2aeb5f21fe8d008f533d036774dbe891252d4/libandroid-navigation/build.gradle#L47), a private binary, as a dependency. The Mapbox Navigator binary may be used with a Mapbox account and under the [Mapbox TOS](https://www.mapbox.com/tos/). If you do not wish to use this binary, make sure you swap out this dependency in [libandroid-navigation/build.gradle](https://github.com/mapbox/mapbox-navigation-android/blob/master/libandroid-navigation/build.gradle). Code in this repo falls under the [MIT license](./LICENSE).
