package com.mapbox.navigation.core.internal.accounts

import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.SKUIdentifier
import com.mapbox.navigation.core.accounts.BillingServiceWrapper
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class MapboxNavigationAccountsTest {

    @Before
    fun setup() {
        mockkObject(BillingServiceWrapper)
    }

    @Test
    fun `obtainSkuToken when resourceUrl notNullOrEmpty querySize zero sessionActiveGuidance`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        every {
            BillingServiceWrapper.getSessionSKUTokenIfValid(SKUIdentifier.NAV2_SES_TRIP)
        } returns "12345"

        val result = MapboxNavigationAccounts.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/")
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?sku=12345"),
            result
        )
    }

    @Test
    fun `obtainSkuToken when resourceUrl not empty and querySize not zero sessionActiveGuidance`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        every {
            BillingServiceWrapper.getSessionSKUTokenIfValid(SKUIdentifier.NAV2_SES_TRIP)
        } returns "12345"

        val result = MapboxNavigationAccounts.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/?query=test")
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?query=test&sku=12345"),
            result
        )
    }

    @Test
    fun `obtainSkuToken when resourceUrl notNullOrEmpty but token blank then sku is not added`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.NO_SESSION
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.NO_SESSION

        val result = MapboxNavigationAccounts.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/?query=test")
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?query=test"),
            result
        )
    }

    @Test
    fun `obtainSkuToken when free drive session`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.NO_SESSION
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        every {
            BillingServiceWrapper.getSessionSKUTokenIfValid(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns "12345"

        val result = MapboxNavigationAccounts.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/")
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?sku=12345"),
            result
        )
    }

    @Test
    fun `do not append sku token when active guidance session paused`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_PAUSED
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.NO_SESSION

        val result = MapboxNavigationAccounts.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/")
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/"),
            result
        )
    }

    @Test
    fun `do not append sku token when free drive session paused`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.NO_SESSION
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.SESSION_PAUSED

        val result = MapboxNavigationAccounts.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/")
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/"),
            result
        )
    }

    @Test
    fun obtainSkuId_is_09() {
        assertEquals("09", MapboxNavigationAccounts.obtainSkuId())
    }

    @After
    fun tearDown() {
        unmockkObject(BillingServiceWrapper)
    }
}
