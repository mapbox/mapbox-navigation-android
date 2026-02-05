package com.mapbox.navigation.core.trip.session

import android.annotation.SuppressLint
import android.location.LocationManager
import android.os.SystemClock
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.common.location.DeviceLocationProviderType
import com.mapbox.common.location.ExtendedLocationProviderParameters
import com.mapbox.common.location.GetLocationCallback
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationError
import com.mapbox.common.location.LocationErrorCode
import com.mapbox.common.location.LocationObserver
import com.mapbox.common.location.LocationProvider
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.navigation.base.options.LocationOptions
import com.mapbox.navigation.base.options.LocationOptions.LocationProviderSource.Companion.BEST
import com.mapbox.navigation.base.options.LocationOptions.LocationProviderSource.Companion.FUSED
import com.mapbox.navigation.base.options.LocationOptions.LocationProviderSource.Companion.GPS
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationProvider
import com.mapbox.navigation.core.utils.ThreadUtils
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logW
import java.util.concurrent.TimeUnit

/**
 * This class is only intended to be used by the [MapboxTripSession].
 *
 * Trip locations can come from the device, from an external location engine, or from a replay
 * simulator. This class is responsible for determining which LocationEngine is used.
 *
 * When a trip session is started with replay, use the [ReplayLocationProvider]. If the
 * trip session is not using replay, use the provider from [NavigationOptions.locationOptions].
 */
internal class TripSessionLocationEngine constructor(
    locationOptions: LocationOptions,
    private val replayLocationProviderProvider: (MapboxReplayer) -> ReplayLocationProvider = {
        ReplayLocationProvider(it)
    },
) {

    val mapboxReplayer: MapboxReplayer by lazy { MapboxReplayer() }
    var isReplayEnabled = false
        private set

    private val replayLocationProvider: ReplayLocationProvider by lazy {
        replayLocationProviderProvider.invoke(mapboxReplayer)
    }
    private val optionsBasedLocationProvider: LocationProvider?
    private var activeLocationProvider: LocationProvider? = null
    private var onRawLocationUpdate: (Location) -> Unit = { }

    private val handlerThread = ThreadUtils.prepareHandlerThread("locations inputs thread")

    private val lastLocationCallback = GetLocationCallback {
        logD(LOG_CATEGORY) {
            "last location callback $it"
        }
        if (it != null) {
            handleReceivedLocation(it)
        }
    }
    private val locationObserver = LocationObserver { locations ->
        logD(LOG_CATEGORY) {
            "location callback $locations"
        }
        locations.lastOrNull()?.let { handleReceivedLocation(it) }
    }
    private var lastLocationTask: Cancelable? = null

    init {
        val customFactory = locationOptions.locationProviderFactory
        if (customFactory != null) {
            if (locationOptions.locationProviderType == LocationOptions.LocationProviderType.REAL) {
                LocationServiceFactory.getOrCreate()
                    .setUserDefinedDeviceLocationProviderFactory(customFactory)
            }
        }
        val deviceLocationProviderExpected =
            if (locationOptions.locationProviderType == LocationOptions.LocationProviderType.REAL) {
                val commonType = locationOptions.locationProviderSource.toCommon()
                LocationServiceFactory.getOrCreate().getDeviceLocationProvider(
                    extendedParameters = ExtendedLocationProviderParameters.Builder()
                        .deviceLocationProviderType(commonType)
                        .apply {
                            if (commonType == DeviceLocationProviderType.ANDROID) {
                                locationProviderName(LocationManager.GPS_PROVIDER)
                            }
                        }
                        .build(),
                    request = locationOptions.request,
                )
            } else {
                locationOptions.locationProviderFactory?.build(locationOptions.request)
                    ?: ExpectedFactory.createError(
                        LocationError(
                            LocationErrorCode.INVALID_ARGUMENT,
                            "Custom location provider factory is null, " +
                                "while location provider type is " +
                                locationOptions.locationProviderType,
                        ),
                    )
            }
        if (deviceLocationProviderExpected.isError) {
            logW(
                LOG_CATEGORY,
                "Location updates are not possible: " +
                    "could not find suitable location provider. " +
                    "Error code: ${deviceLocationProviderExpected.error!!.code}, " +
                    "message: ${deviceLocationProviderExpected.error!!.message}.",
            )
        }
        optionsBasedLocationProvider = deviceLocationProviderExpected.value
    }

    private fun handleReceivedLocation(location: Location) {
        logIfLocationIsNotFreshEnough(location)
        onRawLocationUpdate(location)
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(isReplayEnabled: Boolean, onRawLocationUpdate: (Location) -> Unit) {
        logD(LOG_CATEGORY) {
            "starting location updates for ${if (isReplayEnabled) "replay " else ""}location engine"
        }
        stopLocationUpdates()
        this.onRawLocationUpdate = onRawLocationUpdate
        activeLocationProvider = if (isReplayEnabled) {
            replayLocationProvider
        } else {
            optionsBasedLocationProvider
        }
        this.isReplayEnabled = isReplayEnabled
        activeLocationProvider?.addLocationObserver(locationObserver, handlerThread.looper)
        lastLocationTask = activeLocationProvider?.getLastLocation(lastLocationCallback)
    }

    fun stopLocationUpdates() {
        if (isReplayEnabled) {
            replayLocationProvider.cleanUpLastLocation()
        }
        isReplayEnabled = false
        onRawLocationUpdate = { }
        activeLocationProvider?.run {
            removeLocationObserver(locationObserver)
            lastLocationTask?.cancel()
        }
        activeLocationProvider = null
    }

    fun destroy() {
        LocationServiceFactory.getOrCreate().setUserDefinedDeviceLocationProviderFactory(null)
        handlerThread.quit()
    }

    private fun logIfLocationIsNotFreshEnough(location: Location) {
        val currentTime = SystemClock.elapsedRealtimeNanos()
        val locationAgeMilliseconds = location.monotonicTimestamp?.let {
            TimeUnit.MILLISECONDS.convert(
                currentTime - it,
                TimeUnit.NANOSECONDS,
            )
        }
        if (
            locationAgeMilliseconds == null ||
            locationAgeMilliseconds > DELAYED_LOCATION_WARNING_THRESHOLD_MS
        ) {
            logW("Got an obsolete location: age = $locationAgeMilliseconds ms", LOG_CATEGORY)
        }
    }

    private fun LocationOptions.LocationProviderSource.toCommon() = when (this) {
        BEST -> DeviceLocationProviderType.BEST
        GPS -> DeviceLocationProviderType.ANDROID
        FUSED -> DeviceLocationProviderType.GOOGLE_PLAY_SERVICES
        else -> {
            throw IllegalArgumentException("Unknown location provider source: $this")
        }
    }

    private companion object {
        private const val DELAYED_LOCATION_WARNING_THRESHOLD_MS = 500 // 0.5s
        private const val LOG_CATEGORY = "TripSessionLocationEngine"
    }
}
