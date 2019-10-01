package com.mapbox.services.android.navigation.v5.accounts

import android.content.SharedPreferences
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.annotation.NonNull
import com.mapbox.android.accounts.v1.MapboxAccounts

private const val MAPBOX_NAV_PREFERENCE_TRIPS_SKU = "com.mapbox.navigationsdk.accounts.trips.sku"
private const val MAPBOX_NAV_PREFERENCE_ROUTE_REQ_COUNT = "com.mapbox.navigationsdk.accounts.trips.count"
private const val MAPBOX_NAV_PREFERENCE_TRIPS_TIMESTAMP = "com.mapbox.navigationsdk.accounts.trips.time"
private const val ROUTE_REQUEST_COUNT_THRESHOLD = 5
private const val TRIPS_TIMER_EXPIRE_THRESHOLD = 2
private const val DEFAULT_REQUEST_COUNT = 0
private const val DEFAULT_TOKEN_TIMER = 0L
private const val TRIPS_TIMER_EXPIRE_AFTER = DateUtils.HOUR_IN_MILLIS / 1000 * TRIPS_TIMER_EXPIRE_THRESHOLD

internal class Trips(@NonNull private val preferences: SharedPreferences): TokenGenerator {

    enum class RotateTripsType {
        INVALID,
        ROTATE_ON_TIMER_EXPIRE,
        ROTATE_ON_REQUEST_COUNT_EXPIRE
    }

    private var rotateTripsType: RotateTripsType = RotateTripsType.INVALID

    private fun refreshSkuToken() {
        if (!shouldRefreshSku()) {
            return
        }
        var requestCount: Int
        when (rotateTripsType) {
            RotateTripsType.ROTATE_ON_TIMER_EXPIRE -> {
                requestCount = getRouteRequestCountThreshold()
                requestCount++
                setTimerExpiry(getNow())
                persistTripsSkuToken()
            }
            RotateTripsType.ROTATE_ON_REQUEST_COUNT_EXPIRE -> {
                requestCount = 0
                setTimerExpiry(getNow())
                persistTripsSkuToken()
            }
            else -> {
                requestCount = getRouteRequestCountThreshold()
                requestCount++
            }
        }
        setRouteRequestCountThreshold(requestCount)
    }

    private fun shouldRefreshSku(): Boolean {
        val routeReqCountExpired = validateRouteRequestCountExpiry()
        val timerExpired = validateTimerExpiry()
        rotateTripsType = when {
            routeReqCountExpired -> RotateTripsType.ROTATE_ON_REQUEST_COUNT_EXPIRE
            timerExpired -> RotateTripsType.ROTATE_ON_TIMER_EXPIRE
            else -> RotateTripsType.INVALID
        }
        return routeReqCountExpired || timerExpired
    }

    private fun validateTimerExpiry(): Boolean {
        val skuTokenTimeStamp = getTimerExpiry()
        return isTwoHoursExpired(skuTokenTimeStamp)
    }

    private fun validateRouteRequestCountExpiry(): Boolean {
        val routeRequestCount = getRouteRequestCountThreshold()
        return routeRequestCount > ROUTE_REQUEST_COUNT_THRESHOLD
    }

    private fun setRouteRequestCountThreshold(count: Int) {
        preferences.edit().putInt(MAPBOX_NAV_PREFERENCE_ROUTE_REQ_COUNT, count).apply()
    }

    private fun getRouteRequestCountThreshold(): Int {
        return preferences.getInt(MAPBOX_NAV_PREFERENCE_ROUTE_REQ_COUNT, DEFAULT_REQUEST_COUNT)
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
        return preferences.getLong(MAPBOX_NAV_PREFERENCE_TRIPS_TIMESTAMP, DEFAULT_TOKEN_TIMER)
    }

    private fun isTwoHoursExpired(then: Long): Boolean {
        return isExpired(getNow(), then)
    }

    private fun getNow(): Long {
        return SystemClock.elapsedRealtime()
    }

    private fun isExpired(now: Long, then: Long): Boolean {
        return now - then > TRIPS_TIMER_EXPIRE_AFTER
    }

    override fun obtainSkuToken(): String {
        refreshSkuToken()
        return retrieveTripsSkuToken()
    }

    override fun onNavigationEnd() {
        setRouteRequestCountThreshold(DEFAULT_REQUEST_COUNT)
        setTimerExpiry(DEFAULT_TOKEN_TIMER)
    }
}