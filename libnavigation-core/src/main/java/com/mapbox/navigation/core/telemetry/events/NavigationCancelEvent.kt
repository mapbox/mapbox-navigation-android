package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import com.mapbox.navigation.base.metrics.NavigationMetrics

@SuppressLint("ParcelCreator")
internal class NavigationCancelEvent(
    phoneState: PhoneState
) : NavigationEvent(phoneState) {
    /*
     * Don't remove any fields, cause they should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    var arrivalTimestamp: String? = null
    var rating: Int = 0
    var comment: String = ""

    override fun getEventName(): String = NavigationMetrics.CANCEL_SESSION
}
