<div align="center">
  <a href="https://www.mapbox.com/android-docs/navigation/overview/"><img src="https://github.com/mapbox/mapbox-navigation-android/blob/main/.github/splash-img.png?raw=true" alt="Mapbox Service"></a>
</div>
<br>
<p align="center">
  <a href="https://circleci.com/gh/mapbox/mapbox-navigation-android">
    <img src="https://circleci.com/gh/mapbox/mapbox-navigation-android.svg?style=shield&circle-token=:circle-token">
  </a>
</p>

When your users want to get from one location to another, don’t push them out of your application into a generic map application. Instead, keep them engaged with your application 100% of the time with in-app turn-by-turn navigation.

The Mapbox Navigation SDK for Android is built on top of [the Mapbox Directions API](https://www.mapbox.com/directions) and [the Mapbox Maps SDK](https://www.mapbox.com/maps) to provide tools needed to build a complete navigation experience.

The Mapbox Navigation SDK is a precise and flexible platform which enables your users to explore the world's streets. We are designing new maps specifically for navigation that highlight traffic conditions and helpful landmarks. The calculations are based on the user's current location and compare it to the current route that the user's traversing to provide critical information at any given moment. _You control the entire experience, from the time your user chooses a destination to when they arrive._

## Getting Started
Refer to the [full documentation pages](https://docs.mapbox.com/android/navigation/) for [installation](https://docs.mapbox.com/android/navigation/guides/get-started/install/) and usage instructions.

For the latest version and changelog visit [CHANGELOG](./CHANGELOG.md) or [releases](https://github.com/mapbox/mapbox-navigation-android/releases) pages.

Along with the full documentation, [this migration guide](https://docs.mapbox.com/android/navigation/guides/migrate-to-v2/) can help you transition your project from version `v1` of the Navigation SDK to `v2` or higher.

## Getting Help

- **Need help with your code?**: Look for previous questions on the [#mapbox tag](https://stackoverflow.com/questions/tagged/mapbox+android) — or [ask a new question](https://stackoverflow.com/questions/tagged/mapbox+android).
- **Have a bug to report?** [Open an issue](https://github.com/mapbox/mapbox-navigation-android/issues). If possible, include the version of Mapbox Services, a full log, and a project that shows the issue.
- **Have a feature request?** [Open an issue](https://github.com/mapbox/mapbox-navigation-android/issues/new). Tell us what the feature should do and why you want the feature.

## Using Snapshots

You can use a `-SNAPSHOT` release if you want to test recent bug fixes or features that have not been packaged in an official release yet.

##### `1.0.0`+ versions of the Navigation SDK:

To access `SNAPSHOT` builds follow the [installation instructions](https://docs.mapbox.com/android/navigation/guides/get-started/install/) and then:
1. Provide the below additional snapshot repository reference, next to the existing release repository reference:
```groovy
maven {
    url 'https://api.mapbox.com/downloads/v2/snapshots/maven'
    authentication {
        basic(BasicAuthentication)
    }
    credentials {
        username = "mapbox"
        password = "{secret Mapbox token with DOWNLOADS:READ scope, the same as the token used for the release repository}"
    }
}
```

2. Append `-SNAPSHOT` to the target version:
```groovy
dependencies {
  implementation "com.mapbox.navigation:android:X.Y.Z-SNAPSHOT"
}
```

You can find the latest snapshot version reference in [gradle.properties](./gradle.properties).

## <a name="sample-code">Sample code

Examples for Mapbox Navigation Android SDK are now available on their own separate repo, [available here](https://github.com/mapbox/mapbox-navigation-android-examples).

The QA application [is available here](https://github.com/mapbox/mapbox-navigation-android/tree/main/qa-test-app/README.md).
  
General documentation for Mapbox Navigation Android SDK is [available here](https://docs.mapbox.com/android/navigation). 

## Contributing

We welcome feedback, translations, and code contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.
