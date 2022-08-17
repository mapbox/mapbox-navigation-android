package com.mapbox.navigation.base.route

import com.mapbox.navigation.base.internal.CurrentIndicesSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentIndicesSnapshotTest {

    @Test
    fun defaultConstructor() {
        with(CurrentIndicesSnapshot()) {
            assertEquals(0, legIndex)
            assertEquals(0, routeGeometryIndex)
            assertNull(legGeometryIndex)
        }
    }
}
