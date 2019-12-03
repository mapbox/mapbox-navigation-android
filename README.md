<div align="center">
  <img src="https://github.com/flitsmeister/flitsmeister-navigation-android/blob/master/.github/splash-image.png?raw=true" alt="Flitsmeister Service">
</div>
<br>
<p align="center">
  <a href="https://jitpack.io/#flitsmeister/flitsmeister-navigation-android">
    <img src="https://jitpack.io/v/flitsmeister/flitsmeister-navigation-android.svg"
         alt="Jitpack">
  </a>
</p>

When your users want to get from one location to another, don’t push them out of your application into a generic map application. Instead, keep them engaged with your application 100% of the time with in-app turn-by-turn navigation.

The Flitsmeister Navigation SDK for Android is built on a fork of [the mapbox Navigation SDK v0.19](https://github.com/flitsmeister/flitsmeister-navigation-android/tree/v0.19.0) which is build on top of [the Mapbox Directions API](https://www.mapbox.com/directions) and contains logic needed to get timed navigation instructions.

The Flitsmeister Navigation SDK is a precise and flexible platform which enables your users to explore the world's streets. We are designing new maps specifically for navigation that highlight traffic conditions and helpful landmarks. The calculations use the user's current location and compare it to the current route that the user's traversing to provide critical information at any given moment. _You control the entire experience, from the time your user chooses a destination to when they arrive._

# Why have we forked

# What have we changed

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
  implementation 'com.github.flitsmeister:flitsmeister-navigation-android:v1.0.0'
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
  libraryDependencies += "com.github.flitsmeister" % "flitsmeister-navigation-android" % "v1.0.0"	
```

## leiningen
Step 1. Add it in your `project.clj` at the end of repositories:
```
  :repositories [["jitpack" "https://jitpack.io"]]
```
Step 2. Add the dependency
```
  :dependencies [[com.github.flitsmeister/flitsmeister-navigation-android "v1.0.0"]]
```

To run the [sample code](#sample-code) on a device or emulator, include your [developer access token](https://www.mapbox.com/help/define-access-token/) in `developer-config.xml` found in the project. 

## Getting Help

- **Have a bug to report?** [Open an issue](https://github.com/flitsmeister/flitsmeister-navigation-android/issues). If possible, include the version of Flitsmeister Services, a full log, and a project that shows the issue.
- **Have a feature request?** [Open an issue](https://github.com/flitsmeister/flitsmeister-navigation-android/issues/new). Tell us what the feature should do and why you want the feature.

## <a name="sample-code">Sample code

[We've added one navigation examples to this repo's test app](https://github.com/flitsmeister/flitsmeister-navigation-android/tree/master/app/src/main/java/com/mapbox/services/android/navigation/testapp/) to help you get started with the SDK and to inspire you.

## Contributing

We welcome feedback, translations, and code contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.
