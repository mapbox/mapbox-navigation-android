
<p align="center">
  <img src="screenshots/free-drive.png" height="250">
  <img src="screenshots/active-guidance.png" height="250">
</p>

## Overview

The Mapbox Navigation Android Auto SDK is a public library to help you publish Mapbox Android Auto 
apps to [Google Play](https://play.google.com/store/apps). The library provides everything you need 
to have an Android Auto Navigation App accepted by Google Play. This library bundles 
[Mapbox Maps](https://www.mapbox.com/maps), [Mapbox Navigation](https://www.mapbox.com/navigation/), 
[Mapbox Search](https://www.mapbox.com/search), and the 
[Jetpack Car Library for Android Auto](https://developer.android.com/jetpack/androidx/releases/car-app).

This library builds on top of the 
[Mapbox Maps Android Auto Extension](https://github.com/mapbox/mapbox-maps-android/tree/main/extension-androidauto). 
Please refer to the maps extension for building solutions without Mapbox Navigation.

A simple example can be found in our 
[app-qa-androidauto](https://github.com/mapbox/navigation/tree/main/projects/mapbox-navigation-android/app-qa-androidauto). 
To see a larger integration, you can refer to the 
[navigation examples](https://github.com/mapbox/mapbox-navigation-android-examples).

Please refer to 
[Google's developer documentation](https://developer.android.com/training/cars/navigation)to learn 
how to develop apps for Android Auto.

## Getting Started

This README is intended for building an app that uses the Mapbox Navigation Android Auto SDK. To 
add the android auto extension to your project, you configure its dependency in your `build.gradle` 
files.

```groovy
// In the root build.gradle file
// The Mapbox access token needs to a scope set to DOWNLOADS:READ
allprojects {
    repositories {
        maven {
            url 'https://api.mapbox.com/downloads/v2/releases/maven'
            authentication {
                basic(BasicAuthentication)
            }
            credentials {
                username = "mapbox"
                password = "MAPBOX_SECRET_TOKEN"
            }
        }
    }
}

// You must have the same version android-auto-components as Nav SDK version if you use the latest
dependencies {
   implementation 'com.mapbox.navigationcore:android-auto-components:${NAV_SDK_VERSION}'
}
```

## AndroidManifest and CarAppService

You should become familiar with 
[Google's documentation for building a navigation app](https://developer.android.com/training/cars/apps/navigation). 
You will need to declare your own 
[CarAppService](https://developer.android.com/reference/androidx/car/app/CarAppService) to get 
started. You will also need to specify the `androidx.car.app.category.NAVIGATION` category in order 
to have the app accepted by Google Play.

``` xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.mapbox.navigation.testapp">

    <application
        <!-- Link to your implementation of CarAppService -->
        <service
            android:name=".car.MainCarAppService"
            android:exported="true"
            tools:ignore="ExportedService"
            android:foregroundServiceType="location">

            <intent-filter>
                <action android:name="androidx.car.app.CarAppService" />
                <category android:name="androidx.car.app.category.NAVIGATION" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

## Example MainCarAppService

```
class MainCarAppService : CarAppService() {
    override fun createHostValidator() = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession() = MainCarSession()
}
```

The `MainCarSession` example is where you will implement your Android Auto experience. All the building blocks are included in the SDK, but you will need to decide how to use them yourself. Mapbox is also simplifiying and solidifying this architecuture. Please refer to `MapboxCarMap` and `MapboxNavigationApp`. `MapboxCarMap` will make the Mapbox Map show on the Android Auto head unit, you can add/remove elements with `MapboxCarMapObserver`. Similarly, `MapboxNavigationApp` helps you to customize the entire turn-by-turn navigation experience.

## Testing

Please refer to [Google training documentation](https://developer.android.com/training/cars/testing) to use the ./desktop-head-unit.

tl;dr here. You need to run an emulator to test Android Auto.
Testing in real cars, requires publishing your app to Google Play.

1. Install the emulator: SDK Manager > SDK Tools > Android Auto Desktop Head Unit Emulator > **install**
1. Make sure you have the Android Auto app on your phone https://play.google.com/store/apps/details?id=com.google.android.projection.gearhead
1. Android Auto App, enable developer settings by tapping on device info and then version info
1. Android Auto App > click the hamburger on the top right > Start head unit server
1. Set the `ANDROID_HOME` environment variable to your android SDK location (e.g., /Users/{user}/Library/Android/sdk)
1. $ make car
   1. $ adb forward tcp:5277 tcp:5277
   1. $ cd $(ANDROID_HOME)/extras/google/auto/
   1. $ ./desktop-head-unit
