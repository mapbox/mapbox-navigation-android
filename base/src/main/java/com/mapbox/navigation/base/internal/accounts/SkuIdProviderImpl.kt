package com.mapbox.navigation.base.internal.accounts

import com.mapbox.common.SessionSKUIdentifier
import com.mapbox.common.UserSKUIdentifier

class SkuIdProviderImpl : SkuIdProvider {

    override fun getActiveGuidanceSku(): SessionSKUIdentifier {
        return SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP
    }

    override fun getFreeDriveSku(): SessionSKUIdentifier {
        return SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP
    }

    override fun getUserSkuId(): UserSKUIdentifier {
        return UserSKUIdentifier.NAV3_CORE_MAU
    }
}
