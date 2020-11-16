package com.mapbox.navigation.core.trip.model.eh

import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.junit.Test

class EHorizonTest {

    @Test
    fun `current returns edge with the same edgeId`() {
        val electronicHorizon = EHorizonTestUtil.loadLongBranch()
        val eHorizon = EHorizonMapper.mapToEHorizon(electronicHorizon)

        val edge = eHorizon.current(
            mockk {
                every { edgeId } returns 2452997120465L
                every { percentAlong } returns 56.5
            }
        )

        assertEquals(2452997120465L, edge.id)
    }

    @Test(expected = Exception::class)
    fun `current throws when the edgeId does not exist`() {
        val electronicHorizon = EHorizonTestUtil.loadLongBranch()
        val eHorizon = EHorizonMapper.mapToEHorizon(electronicHorizon)

        eHorizon.current(
            mockk {
                every { edgeId } returns 0L
                every { percentAlong } returns 56.5
            }
        )
    }
}
