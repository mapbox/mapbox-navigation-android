package com.mapbox.services.android.navigation.v5.internal.accounts

import android.content.SharedPreferences
import android.os.SystemClock
import com.mapbox.android.accounts.v1.MapboxAccounts

internal class TripsSku(
    private val preferences: SharedPreferences,
    private val timerExpireAfter: Long,
    private val routeRequestThreshold: Int
) : SkuGenerator {

    enum class RotateTripsType {
        INVALID,
        ROTATE_ON_TIMER_EXPIRE,
        ROTATE_ON_REQUEST_COUNT_EXPIRE
    }

    companion object {
        private const val MAPBOX_NAV_PREFERENCE_TRIPS_SKU = "com.mapbox.navigationsdk.accounts.trips.skuToken"
        private const val MAPBOX_NAV_PREFERENCE_ROUTE_REQ_COUNT = "com.mapbox.navigationsdk.accounts.trips.request.count"
        private const val MAPBOX_NAV_PREFERENCE_TRIPS_TIMESTAMP = "com.mapbox.navigationsdk.accounts.trips.timeStamp"
        private const val DEFAULT_TRIP_REQUEST_COUNT = 0
        private const val DEFAULT_TRIP_TOKEN_TIMER = 0L
    }

    private var isNavigating = false

    override fun generateToken(): String {
        refreshSkuToken()
        return retrieveTripsSkuToken()
    }

    override fun onNavigationStart() {
        this.isNavigating = true
        setRouteRequestCount(DEFAULT_TRIP_REQUEST_COUNT)
    }

    override fun onNavigationEnd() {
        this.isNavigating = false
        setTimerExpiry(DEFAULT_TRIP_TOKEN_TIMER)
    }

    private fun refreshSkuToken() {
        when(isNavigating) {
            true -> {
                if(validateTimerExpiry()) {
                    setTimerExpiry(getNow())
                    persistTripsSkuToken()
                }
            }
            false -> {
                var requestCount: Int
                val rotateTripsType = validateRotation()
                if (rotateTripsType == RotateTripsType.ROTATE_ON_TIMER_EXPIRE
                        || rotateTripsType == RotateTripsType.ROTATE_ON_REQUEST_COUNT_EXPIRE) {
                    requestCount = 1
                    setTimerExpiry(getNow())
                    persistTripsSkuToken()
                } else {
                    requestCount = getRouteRequestCountThreshold()
                    requestCount++
                }
                setRouteRequestCount(requestCount)
            }
        }
    }

    private fun validateRotation(): RotateTripsType {
        val routeReqCountExpired = validateRouteRequestCountExpiry()
        val timerExpired = validateTimerExpiry()
        return when {
            routeReqCountExpired -> RotateTripsType.ROTATE_ON_REQUEST_COUNT_EXPIRE
            timerExpired -> RotateTripsType.ROTATE_ON_TIMER_EXPIRE
            else -> RotateTripsType.INVALID
        }
    }

    private fun validateTimerExpiry(): Boolean {
        val skuTokenTimeStamp = getTimerExpiry()
        return isTwoHoursExpired(skuTokenTimeStamp)
    }

    private fun validateRouteRequestCountExpiry(): Boolean {
        val routeRequestCount = getRouteRequestCountThreshold()
        return routeRequestCount > routeRequestThreshold
    }

    private fun setRouteRequestCount(count: Int) {
        preferences.edit().putInt(MAPBOX_NAV_PREFERENCE_ROUTE_REQ_COUNT, count).apply()
    }

    private fun getRouteRequestCountThreshold(): Int {
        return preferences.getInt(MAPBOX_NAV_PREFERENCE_ROUTE_REQ_COUNT, DEFAULT_TRIP_REQUEST_COUNT)
    }

    private fun persistTripsSkuToken() {
        val token = generateTripsSkuToken()
        preferences.edit().putString(MAPBOX_NAV_PREFERENCE_TRIPS_SKU, token).apply()
    }

    private fun retrieveTripsSkuToken(): String {
        return preferences.getString(MAPBOX_NAV_PREFERENCE_TRIPS_SKU, "")!!
    }

    private fun generateTripsSkuToken(): String {
        return MapboxAccounts.obtainNavigationSkuSessionToken()
    }

    private fun setTimerExpiry(then: Long) {
        preferences.edit().putLong(MAPBOX_NAV_PREFERENCE_TRIPS_TIMESTAMP, then).apply()
    }

    private fun getTimerExpiry(): Long {
        return preferences.getLong(MAPBOX_NAV_PREFERENCE_TRIPS_TIMESTAMP, DEFAULT_TRIP_TOKEN_TIMER)
    }

    private fun isTwoHoursExpired(then: Long): Boolean {
        return isExpired(getNow(), then)
    }

    private fun getNow(): Long {
        return SystemClock.elapsedRealtime()
    }

    private fun isExpired(now: Long, then: Long): Boolean {
        return (now - then) / 1000 > timerExpireAfter
    }
}
