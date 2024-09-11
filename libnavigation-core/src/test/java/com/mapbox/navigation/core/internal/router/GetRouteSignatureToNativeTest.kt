package com.mapbox.navigation.core.internal.router

import com.mapbox.navigator.GetRouteOrigin
import com.mapbox.navigator.GetRouteReason
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class GetRouteSignatureToNativeTest internal constructor(
    private val input: GetRouteSignature,
    private val expectedOutput: com.mapbox.navigator.GetRouteSignature,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    GetRouteSignature(
                        GetRouteSignature.Reason.NEW_ROUTE,
                        GetRouteSignature.Origin.SDK,
                    ),
                    com.mapbox.navigator.GetRouteSignature(
                        GetRouteReason.NEW_ROUTE,
                        GetRouteOrigin.PLATFORM_SDK,
                        "",
                    ),
                ),
                arrayOf(
                    GetRouteSignature(
                        GetRouteSignature.Reason.NEW_ROUTE,
                        GetRouteSignature.Origin.APP,
                    ),
                    com.mapbox.navigator.GetRouteSignature(
                        GetRouteReason.NEW_ROUTE,
                        GetRouteOrigin.CUSTOMER,
                        "",
                    ),
                ),
                arrayOf(
                    GetRouteSignature(
                        GetRouteSignature.Reason.REROUTE_OTHER,
                        GetRouteSignature.Origin.SDK,
                    ),
                    com.mapbox.navigator.GetRouteSignature(
                        GetRouteReason.REROUTE_OTHER,
                        GetRouteOrigin.PLATFORM_SDK,
                        "",
                    ),
                ),
                arrayOf(
                    GetRouteSignature(
                        GetRouteSignature.Reason.REROUTE_BY_DEVIATION,
                        GetRouteSignature.Origin.SDK,
                    ),
                    com.mapbox.navigator.GetRouteSignature(
                        GetRouteReason.REROUTE_BY_DEVIATION,
                        GetRouteOrigin.PLATFORM_SDK,
                        "",
                    ),
                ),
            )
        }
    }

    @Test
    fun toNativeSignature() {
        assertEquals(expectedOutput, input.toNativeSignature())
    }
}
