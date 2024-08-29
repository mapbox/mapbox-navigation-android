package com.mapbox.navigation.base.internal.accounts

import com.mapbox.common.SessionSKUIdentifier
import com.mapbox.common.UserSKUIdentifier
import org.junit.Assert.assertEquals
import org.junit.Test

class SkuIdProviderTest {

    private val skuIdProvider = SkuIdProviderImpl()

    @Test
    fun testActiveGuidanceSku() {
        assertEquals(
            SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
            skuIdProvider.getActiveGuidanceSku(),
        )
    }

    @Test
    fun testFreeDriveSku() {
        assertEquals(
            SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP,
            skuIdProvider.getFreeDriveSku(),
        )
    }

    @Test
    fun testUserSkuId() {
        assertEquals(
            UserSKUIdentifier.NAV3_CORE_MAU,
            skuIdProvider.getUserSkuId(),
        )
    }
}
