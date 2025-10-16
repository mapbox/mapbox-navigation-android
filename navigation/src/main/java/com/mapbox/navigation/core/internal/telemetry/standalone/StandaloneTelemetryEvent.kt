package com.mapbox.navigation.core.internal.telemetry.standalone

import android.os.Build
import androidx.annotation.RestrictTo
import com.google.gson.Gson
import com.mapbox.bindgen.Value
import com.mapbox.common.TelemetrySystemUtils
import com.mapbox.common.toValue
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics
import com.mapbox.navigation.core.internal.SdkInfoProvider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface StandaloneTelemetryEvent : MetricEvent {
    fun toValue(): Value
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class StandaloneCustomEvent(
    val type: String,
    val payload: String,
    val customEventVersion: String = "1.0",
) : StandaloneTelemetryEvent {

    private val created: String = TelemetrySystemUtils.obtainCurrentDate()

    override val metricName: String = EVENT_NAME

    override fun toValue(): Value {
        return hashMapOf(
            "type" to type.toValue(),
            "payload" to payload.toValue(),
            "customEventVersion" to customEventVersion.toValue(),
            "created" to created.toValue(),
        ).run {
            putAll(CONSTANT_FIELDS)
            Value.valueOf(this)
        }
    }

    override fun toJson(gson: Gson): String {
        return gson.toJson(this)
    }

    private companion object {

        const val EVENT_NAME = NavigationMetrics.CUSTOM_EVENT

        // Required fields that can't be absent.
        // Most of them don't make sense in a standalone mode without navigation state.
        val CONSTANT_FIELDS = mapOf(
            "event" to EVENT_NAME.toValue(),
            "version" to "2.4".toValue(),
            "sdkIdentifier" to SdkInfoProvider.sdkInformation().name.toValue(),
            "operatingSystem" to "Android - ${Build.VERSION.RELEASE}".toValue(),
            "device" to (Build.MODEL ?: "Unknown").toValue(),
            "createdMonotime" to 0.toValue(),
            "driverMode" to "freeDrive".toValue(),
            "driverModeId" to "00000000-0000-0000-0000-000000000000".toValue(),
            "driverModeStartTimestamp" to "non_valid".toValue(),
            "driverModeStartTimestampMonotime" to 0.toValue(),
            "eventVersion" to 0.toValue(),
            "simulation" to false.toValue(),
            "locationEngine" to "".toValue(),
            "lat" to 0.0.toValue(),
            "lng" to 0.0.toValue(),
        )
    }
}
