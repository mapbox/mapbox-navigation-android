package com.mapbox.services.android.navigation.v5.internal.accounts

import android.content.SharedPreferences
import android.os.SystemClock
import com.mapbox.android.accounts.v1.MapboxAccounts
import com.mapbox.core.utils.TextUtils

internal class MauSku(
    private val preferences: SharedPreferences,
    private val timerExpireAfter: Long
) : SkuGenerator {

    companion object {
        private const val MAPBOX_NAV_PREFERENCE_MAU_SKU = "com.mapbox.navigationsdk.accounts.mau.sku"
        private const val MAPBOX_NAV_PREFERENCES_USER_ID = "com.mapbox.navigationsdk.accounts.mau.userid"
        private const val MAPBOX_NAV_PREFERENCE_MAU_TIMESTAMP = "com.mapbox.navigationsdk.accounts.mau.time"
        private const val MAPBOX_MAP_PREFERENCE_SKU = "com.mapbox.mapboxsdk.accounts.skutoken"
        private const val DEFAULT_TOKEN_TIMER = 0L
    }

    override fun generateSkuToken(): String {
        refreshSkuToken()
        return retrieveMauSkuToken()
    }

    override fun onNavigationEnd() {
        setTimerExpiry(DEFAULT_TOKEN_TIMER)
    }

    private fun refreshSkuToken() {
        if (!shouldRefreshSku()) {
            return
        }
        setTimerExpiry(getNow())
        persistMauSkuToken()
        persistMapsSkuToken()
    }

    private fun shouldRefreshSku(): Boolean {
        return validateTimerExpiry()
    }

    private fun validateTimerExpiry(): Boolean {
        val skuTokenTimeStamp = getTimerExpiry()
        return isOneHoursExpired(skuTokenTimeStamp)
    }

    private fun setTimerExpiry(then: Long) {
        preferences.edit().putLong(MAPBOX_NAV_PREFERENCE_MAU_TIMESTAMP, then).apply()
    }

    private fun getTimerExpiry(): Long {
        return preferences.getLong(MAPBOX_NAV_PREFERENCE_MAU_TIMESTAMP, DEFAULT_TOKEN_TIMER)
    }

    private fun persistMauSkuToken() {
        val token = generateMauSkuToken()
        preferences.edit().putString(MAPBOX_NAV_PREFERENCE_MAU_SKU, token).apply()
    }

    private fun retrieveMauSkuToken(): String {
        return preferences.getString(MAPBOX_NAV_PREFERENCE_MAU_SKU, "")!!
    }

    private fun persistMapsSkuToken() {
        val mapsSkuToken = generateMapsSkuToken()
        preferences.edit().putString(MAPBOX_MAP_PREFERENCE_SKU, mapsSkuToken).apply()
    }

    private fun persistMauUserId(userId: String) {
        preferences.edit().putString(MAPBOX_NAV_PREFERENCES_USER_ID, userId).apply()
    }

    private fun retrieveUserId(): String {
        return preferences.getString(MAPBOX_NAV_PREFERENCES_USER_ID, "")!!
    }

    private fun getUserId(): String {
        var userId = retrieveUserId()
        if (TextUtils.isEmpty(userId)) {
            userId = generateUserId()
            persistMauUserId(userId)
        }
        return userId
    }

    private fun generateUserId(): String {
        return MapboxAccounts.obtainEndUserId()
    }

    private fun generateMauSkuToken(): String {
        return MapboxAccounts.obtainNavigationSkuUserToken(getUserId())
    }

    private fun generateMapsSkuToken(): String {
        return MapboxAccounts.obtainMapsSkuUserToken(getUserId())
    }

    private fun isOneHoursExpired(then: Long): Boolean {
        return isExpired(getNow(), then)
    }

    private fun getNow(): Long {
        return SystemClock.elapsedRealtime()
    }

    private fun isExpired(now: Long, then: Long): Boolean {
        return now - then > timerExpireAfter
    }
}
