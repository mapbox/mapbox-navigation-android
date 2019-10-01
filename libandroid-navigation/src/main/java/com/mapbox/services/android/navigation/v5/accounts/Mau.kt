package com.mapbox.services.android.navigation.v5.accounts

import android.content.SharedPreferences
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.annotation.NonNull
import com.mapbox.android.accounts.v1.MapboxAccounts
import com.mapbox.core.utils.TextUtils

private const val MAPBOX_NAV_PREFERENCE_MAU_SKU = "com.mapbox.navigationsdk.accounts.mau.sku"
private const val MAPBOX_NAV_PREFERENCES_USER_ID = "com.mapbox.navigationsdk.accounts.mau.userid"
private const val MAPBOX_NAV_PREFERENCE_MAU_TIMESTAMP = "com.mapbox.navigationsdk.accounts.trips.time"
private const val MAU_TIMER_EXPIRE_THRESHOLD = 1
private const val DEFAULT_TOKEN_TIMER = 0L
private const val MAU_TIMER_EXPIRE_AFTER = DateUtils.HOUR_IN_MILLIS / 1000 * MAU_TIMER_EXPIRE_THRESHOLD

internal class Mau(@NonNull private val preferences: SharedPreferences): TokenGenerator {

    private fun refreshSkuToken() {
        if (!shouldRefreshSku()) {
            return
        }
        setTimerExpiry(getNow())
        persistMauSkuToken()
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

    private fun persistMauUserId(userId: String) {
        preferences.edit().putString(MAPBOX_NAV_PREFERENCES_USER_ID, userId).apply()
    }

    private fun retrieveUserId(): String {
        return preferences.getString(MAPBOX_NAV_PREFERENCES_USER_ID, "")!!
    }

    private fun generateUserId(): String {
        return MapboxAccounts.obtainEndUserId()
    }

    private fun generateMauSkuToken(): String {
        var userId = retrieveUserId()
        if (TextUtils.isEmpty(userId)) {
            userId = generateUserId()
            persistMauUserId(userId)
        }
        return MapboxAccounts.obtainNavigationSkuUserToken(userId)
    }

    private fun isOneHoursExpired(then: Long): Boolean {
        return isExpired(getNow(), then)
    }

    private fun getNow(): Long {
        return SystemClock.elapsedRealtime()
    }

    private fun isExpired(now: Long, then: Long): Boolean {
        return now - then > MAU_TIMER_EXPIRE_AFTER
    }

    override fun obtainSkuToken(): String {
        refreshSkuToken()
        return retrieveMauSkuToken()
    }

    override fun onNavigationEnd() {
        setTimerExpiry(DEFAULT_TOKEN_TIMER)
    }
}