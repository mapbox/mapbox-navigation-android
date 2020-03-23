package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import androidx.annotation.Keep
import com.mapbox.navigation.base.metrics.NavigationMetrics

@Keep
@SuppressLint("ParcelCreator")
internal class NavigationCancelEvent(
    phoneState: PhoneState
) : NavigationEvent(phoneState) {
    /*
     * Don't remove any fields, cause they are should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    var arrivalTimestamp: String? = null // Schema null if the user cancelled without arriving (can't happen)
    var rating: Int = 0
    var comment: String = ""

    override fun getEventName(): String = NavigationMetrics.CANCEL_SESSION
}
