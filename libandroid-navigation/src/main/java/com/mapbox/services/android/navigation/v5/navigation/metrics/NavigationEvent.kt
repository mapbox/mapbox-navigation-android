package com.mapbox.services.android.navigation.v5.navigation.metrics

import android.os.Build
import android.os.Parcel

import com.mapbox.android.telemetry.Event
import com.mapbox.services.android.navigation.BuildConfig

/**
 * Base Event class for navigation events, contains common properties.
 */
abstract class NavigationEvent(phoneState: PhoneState) : Event() {
    private val OPERATING_SYSTEM = "Android - " + Build.VERSION.RELEASE

    val operatingSystem = OPERATING_SYSTEM
    val device = Build.MODEL
    val sdkVersion = BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME
    val event: String
    val created: String
    val applicationState: String
    val connectivity: String
    val isBatteryPluggedIn: Boolean
    val volumeLevel: Int
    val screenBrightness: Int
    val batteryLevel: Int
    var startTimestamp: String? = null
    var sdkIdentifier: String? = null
    var sessionIdentifier: String? = null
    var geometry: String? = null
    var profile: String? = null
    var originalRequestIdentifier: String? = null
    var requestIdentifier: String? = null
    var originalGeometry: String? = null
    var audioType: String? = null
    var locationEngine: String? = null
    var tripIdentifier: String? = null
    var lat: Double = 0.toDouble()
    var lng: Double = 0.toDouble()
    var isSimulation: Boolean = false
    var absoluteDistanceToDestination: Int = 0
    var percentTimeInPortrait: Int = 0
    var percentTimeInForeground: Int = 0
    var distanceCompleted: Int = 0
    var distanceRemaining: Int = 0
    var durationRemaining: Int = 0
    var eventVersion: Int = 0
    var estimatedDistance: Int = 0
    var estimatedDuration: Int = 0
    var rerouteCount: Int = 0
    var originalEstimatedDistance: Int = 0
    var originalEstimatedDuration: Int = 0
    var stepCount: Int = 0
    var originalStepCount: Int = 0
    var legIndex: Int = 0
    var legCount: Int = 0
    var stepIndex: Int = 0
    var voiceIndex: Int = 0
    var bannerIndex: Int = 0
    var totalStepCount: Int = 0

    abstract val eventName: String

    init {
        this.created = phoneState.created
        this.volumeLevel = phoneState.volumeLevel
        this.batteryLevel = phoneState.batteryLevel
        this.screenBrightness = phoneState.screenBrightness
        this.isBatteryPluggedIn = phoneState.isBatteryPluggedIn
        this.connectivity = phoneState.connectivity
        this.audioType = phoneState.audioType
        this.applicationState = phoneState.applicationState
        this.event = eventName
    }

    override fun describeContents(): Int {
        return 0
    }
    override fun writeToParcel(dest: Parcel, flags: Int) {}
}