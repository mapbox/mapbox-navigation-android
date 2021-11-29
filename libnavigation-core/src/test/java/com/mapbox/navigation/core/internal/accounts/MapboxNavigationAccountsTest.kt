package com.mapbox.navigation.core.internal.accounts

import com.mapbox.common.BillingServiceInterface
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.SessionSKUIdentifier
import com.mapbox.navigation.core.accounts.BillingServiceProvider
import io.mockk.every
import io.mockk.mockk
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

    private companion object {
        /**
         * Since [MapboxNavigationAccounts] is a singleton, it will effectively obtain
         * an instance of [BillingServiceInterface] from the mocked [BillingServiceProvider]
         * only once when initializing the first test.
         *
         * That instance of returned [BillingServiceInterface] has to have only one mock
         * as well, otherwise, tests following the first one will try to interact with a different
         * mock than [MapboxNavigationAccounts] singleton uses.
         *
         * To ensure that there's only one mock, we're storing it in a companion object.
         */
        private val billingService = mockk<BillingServiceInterface>()
    }

    @Before
    fun setup() {
        mockkObject(BillingServiceProvider)
        every { BillingServiceProvider.getInstance() } returns billingService
    }

    @Test
    fun `obtainSkuToken when resourceUrl notNullOrEmpty querySize zero sessionActiveGuidance`() {
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        every {
            billingService.getSessionSKUTokenIfValid(SessionSKUIdentifier.NAV2_SES_TRIP)
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
            billingService.getSessionStatus(SessionSKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        every {
            billingService.getSessionSKUTokenIfValid(SessionSKUIdentifier.NAV2_SES_TRIP)
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
            billingService.getSessionStatus(SessionSKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.NO_SESSION
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV2_SES_FDTRIP)
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
            billingService.getSessionStatus(SessionSKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.NO_SESSION
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        every {
            billingService.getSessionSKUTokenIfValid(SessionSKUIdentifier.NAV2_SES_FDTRIP)
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
            billingService.getSessionStatus(SessionSKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_PAUSED
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV2_SES_FDTRIP)
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
            billingService.getSessionStatus(SessionSKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.NO_SESSION
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV2_SES_FDTRIP)
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
        unmockkObject(BillingServiceProvider)
    }
}
