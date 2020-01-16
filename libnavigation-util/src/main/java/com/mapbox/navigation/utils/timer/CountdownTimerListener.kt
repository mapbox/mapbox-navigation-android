package com.mapbox.navigation.utils.timer

interface CountdownTimerListener {

    fun millisUntilExpiry(millis: Long)

    fun onTimerExpired()
}
