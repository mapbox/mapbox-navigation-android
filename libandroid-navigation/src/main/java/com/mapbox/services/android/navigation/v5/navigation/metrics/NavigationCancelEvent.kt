package com.mapbox.services.android.navigation.v5.navigation.metrics

import android.annotation.SuppressLint

@SuppressLint("ParcelCreator")
internal class NavigationCancelEvent(phoneState: PhoneState) : NavigationEvent(phoneState) {
    var arrivalTimestamp: String? = null
    var rating: Int = 0
        private set
    var comment: String? = null

    override val eventName: String
        get() = NAVIGATION_CANCEL

    fun setRating(newRating: Int?) {
        newRating?.let {newRating->
            rating = newRating
        }
    }

    companion object {
        private val NAVIGATION_CANCEL = "navigation.cancel"
    }
}
