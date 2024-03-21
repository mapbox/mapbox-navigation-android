package com.mapbox.navigation.instrumentation_tests.core

import android.content.Context
import android.location.Location
import androidx.annotation.IntegerRes
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TollCostsTest : BaseCoreNoCleanUpTest() {

    @Test
    fun testNoTollCostsData() {
        val route = getRoute(context, R.raw.route_with_no_toll_costs)
        assertNull(route.tollCosts())
    }

    @Test
    fun testEmptyTollCosts() {
        val route = getRoute(context, R.raw.route_with_empty_toll_costs)
        val tollCosts = route.tollCosts()
        assertNotNull(tollCosts)

        tollCosts!!
        assertEquals(1, tollCosts.size)
        assertNull(tollCosts[0].currency())
        assertNull(tollCosts[0].paymentMethods())
    }

    @Test
    fun testEmptyTollCostsDataPaymentMethods() {
        val route = getRoute(context, R.raw.route_with_empty_toll_costs_payment_data)
        val tollCosts = route.tollCosts()
        assertNotNull(tollCosts)

        tollCosts!!
        assertEquals(1, tollCosts.size)
        assertEquals("JPY", tollCosts[0].currency())

        val paymentMethods = tollCosts[0].paymentMethods()
        assertNotNull(paymentMethods)

        paymentMethods!!
        assertNull(paymentMethods.cash())
        assertNull(paymentMethods.etc())
    }

    private fun getRoute(
        context: Context,
        @IntegerRes routeFileResource: Int
    ): DirectionsRoute {
        val routeAsString = readRawFileText(context, routeFileResource)
        return DirectionsRoute.fromJson(routeAsString)
    }

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            // no op
        }
    }
}
