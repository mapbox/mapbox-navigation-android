package com.mapbox.navigation.core.internal.accounts

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class MapboxNavigationAccountsTest {

    @Test
    fun obtainSkuToken_when_resourceUrl_notNullOrEmpty_querySize_zero() {
        mockkObject(TokenGeneratorProvider)
        every { TokenGeneratorProvider.getNavigationTokenGenerator() } returns mockk {
            every { getSKUToken() } returns "12345"
        }

        val result = MapboxNavigationAccounts.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/")
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?sku=12345"),
            result
        )
        unmockkObject(TokenGeneratorProvider)
    }

    @Test
    fun obtainSkuToken_when_resourceUrl_notNullOrEmpty_querySize_not_zero() {
        mockkObject(TokenGeneratorProvider)
        every { TokenGeneratorProvider.getNavigationTokenGenerator() } returns mockk {
            every { getSKUToken() } returns "12345"
        }

        val result = MapboxNavigationAccounts.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/?query=test")
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?query=test&sku=12345"),
            result
        )
        unmockkObject(TokenGeneratorProvider)
    }

    @Test
    fun `obtainSkuToken when resourceUrl notNullOrEmpty but token blank then sku is not added`() {
        mockkObject(TokenGeneratorProvider)
        every { TokenGeneratorProvider.getNavigationTokenGenerator() } returns mockk {
            every { getSKUToken() } returns ""
        }

        val result = MapboxNavigationAccounts.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/?query=test")
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?query=test"),
            result
        )
        unmockkObject(TokenGeneratorProvider)
    }

    @Test
    fun obtainSkuId_is_09() {
        assertEquals("09", MapboxNavigationAccounts.obtainSkuId())
    }
}
