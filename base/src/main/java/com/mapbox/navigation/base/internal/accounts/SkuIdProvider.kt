package com.mapbox.navigation.base.internal.accounts

import com.mapbox.common.SessionSKUIdentifier
import com.mapbox.common.UserSKUIdentifier

interface SkuIdProvider {
    fun getActiveGuidanceSku(): SessionSKUIdentifier

    fun getFreeDriveSku(): SessionSKUIdentifier

    fun getUserSkuId(): UserSKUIdentifier
}
