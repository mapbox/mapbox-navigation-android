package com.mapbox.navigation.core.internal.utils

import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.internal.RouterWrapper
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RouterExTest {

    @Test
    fun testIsInternalImplementation() {
        listOf<Triple<String, Router, Boolean>>(
            Triple(
                "Router interface",
                mockk(),
                false
            ),
            Triple(
                "RouterWrapper, internal implementation",
                mockk<RouterWrapper>(),
                true
            ),
        ).let { testCases ->
            testCases.forEach { (message, router, isInternal) ->
                assertEquals(message, isInternal, router.isInternalImplementation())
            }
        }
    }
}
