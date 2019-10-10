package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.annotation.SuppressLint

@SuppressLint("ParcelCreator")
internal class NavigationCancelEvent(
    phoneState: PhoneState
) : NavigationEvent(phoneState) {

    companion object {
        private const val NAVIGATION_CANCEL = "navigation.cancel"
    }

    /*
     * Don't remove any fields, cause they are should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    var arrivalTimestamp: String = ""
    var rating: Int = 0
    var comment: String = ""

    override fun getEventName(): String = NAVIGATION_CANCEL
}
