package com.mapbox.navigation.core.utils.routeRefresh

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RouteRefreshUtilsTest(private val param: Param) {

    @Test
    fun isResultStale() {
        val utils = RouteRefreshUtils()

        assertEquals(
            param.expected,
            utils.isResultStale(param.currentPrimaryId, param.refreshedPrimaryId),
        )
    }

    data class Param(
        val currentPrimaryId: String?,
        val refreshedPrimaryId: String?,
        val expected: Boolean,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters(): List<Param> = listOf(
            Param(currentPrimaryId = null, refreshedPrimaryId = null, expected = false),
            Param(currentPrimaryId = "route-1", refreshedPrimaryId = null, expected = false),
            Param(currentPrimaryId = null, refreshedPrimaryId = "route-1", expected = false),
            Param(currentPrimaryId = "route-1", refreshedPrimaryId = "route-1", expected = false),
            Param(currentPrimaryId = "", refreshedPrimaryId = "", expected = false),
            Param(currentPrimaryId = "route-1", refreshedPrimaryId = "route-2", expected = true),
            Param(currentPrimaryId = "route-1", refreshedPrimaryId = "ROUTE-1", expected = true),
            Param(currentPrimaryId = "route-1", refreshedPrimaryId = "", expected = true),
        )
    }
}
