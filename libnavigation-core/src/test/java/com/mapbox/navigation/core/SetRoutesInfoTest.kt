package com.mapbox.navigation.core

import com.mapbox.navigation.base.internal.CurrentIndicesSnapshot
import com.mapbox.navigation.core.directions.session.RoutesExtra
import org.junit.Assert.assertEquals
import org.junit.Test

class SetRoutesInfoTest {

    @Test
    fun setAlternativeRoutesInfoUsesCorrectReason() {
        val info = SetAlternativeRoutesInfo(0)
        assertEquals(RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE, info.reason)
    }

    @Test
    fun setRefreshedRoutesInfoUsesCorrectReason() {
        val info = SetRefreshedRoutesInfo(CurrentIndicesSnapshot())
        assertEquals(RoutesExtra.ROUTES_UPDATE_REASON_REFRESH, info.reason)
    }
}
