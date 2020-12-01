package com.mapbox.navigation.core.trip.model.eh

import com.mapbox.navigator.ElectronicHorizonResultType
import com.mapbox.navigator.FRC
import com.mapbox.navigator.GraphPosition
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EHorizonMapperTest {

    @Test
    fun `mapToEHorizonResultType should convert to types`() {
        assertEquals(
            EHorizonResultType.INITIAL,
            EHorizonMapper.mapToEHorizonResultType(ElectronicHorizonResultType.INITIAL)
        )
        assertEquals(
            EHorizonResultType.UPDATE,
            EHorizonMapper.mapToEHorizonResultType(ElectronicHorizonResultType.UPDATE)
        )
    }

    @Test
    fun `mapToFunctionalRoadClass should convert to types`() {
        assertEquals(
            FunctionalRoadClass.MOTORWAY,
            EHorizonMapper.mapToFunctionalRoadClass(FRC.MOTORWAY)
        )
        assertEquals(
            FunctionalRoadClass.TRUNK,
            EHorizonMapper.mapToFunctionalRoadClass(FRC.TRUNK)
        )
        assertEquals(
            FunctionalRoadClass.PRIMARY,
            EHorizonMapper.mapToFunctionalRoadClass(FRC.PRIMARY)
        )
        assertEquals(
            FunctionalRoadClass.SECONDARY,
            EHorizonMapper.mapToFunctionalRoadClass(FRC.SECONDARY)
        )
        assertEquals(
            FunctionalRoadClass.TERTIARY,
            EHorizonMapper.mapToFunctionalRoadClass(FRC.TERTIARY)
        )
        assertEquals(
            FunctionalRoadClass.UNCLASSIFIED,
            EHorizonMapper.mapToFunctionalRoadClass(FRC.UNCLASSIFIED)
        )
        assertEquals(
            FunctionalRoadClass.RESIDENTIAL,
            EHorizonMapper.mapToFunctionalRoadClass(FRC.RESIDENTIAL)
        )
        assertEquals(
            FunctionalRoadClass.SERVICE_OTHER,
            EHorizonMapper.mapToFunctionalRoadClass(FRC.SERVICE_OTHER)
        )
    }

    @Test
    fun `mapToEHorizon should have a null parent`() {
        val electronicHorizon = EHorizonTestUtil.loadSmallGraph()

        val eHorizon = EHorizonMapper.mapToEHorizon(electronicHorizon)

        assertNull(eHorizon.start.parent)
    }

    @Test
    fun `mapToEHorizon should map geometry`() {
        val electronicHorizon = EHorizonTestUtil.loadSmallGraph()

        val start = EHorizonMapper.mapToEHorizon(electronicHorizon).start
        val firstLocation = start.geometry?.coordinates()?.get(0)!!
        val secondLocation = start.geometry?.coordinates()?.get(1)!!

        assertEquals(2, start.geometry?.coordinates()?.size)
        assertEquals(-121.46764399999999, firstLocation.longitude(), 0.00001)
        assertEquals(38.562946, firstLocation.latitude(), 0.00001)
        assertEquals(-121.467637, secondLocation.longitude(), 0.00001)
        assertEquals(38.562968999999995, secondLocation.latitude(), 0.00001)
    }

    @Test
    fun `mapToEHorizon should map all top level edge values`() {
        val electronicHorizon = EHorizonTestUtil.loadSmallGraph()

        val start = EHorizonMapper.mapToEHorizon(electronicHorizon).start

        assertEquals(6900441309522L, start.id)
        assertEquals(0.toByte(), start.level)
        assertEquals(1.0, start.probability, 0.0001)
        assertEquals(15.0, start.heading, 0.0001)
        assertEquals(3.0, start.length, 0.0001)
        assertEquals(1, start.out.size)
        assertEquals(null, start.parent)
        assertEquals(FunctionalRoadClass.SERVICE_OTHER, start.functionRoadClass)
        assertEquals(5.5555, start.speed, 0.0001)
        assertEquals(false, start.ramp)
        assertEquals(false, start.motorway)
        assertEquals(false, start.bridge)
        assertEquals(false, start.tunnel)
        assertEquals(false, start.toll)
        assertTrue(start.names.isEmpty())
        assertEquals(0.toByte(), start.curvature)
        assertEquals(2, start.geometry?.coordinates()?.size)
        assertNull(start.speedLimit)
        assertEquals(1.toByte(), start.laneCount)
        assertEquals(8.0, start.meanElevation)
        assertEquals("USA", start.countryCode)
        assertEquals("CA", start.stateCode)
    }

    @Test
    fun `mapToEHorizon should map all outgoing edges`() {
        val electronicHorizon = EHorizonTestUtil.loadLongBranch()

        val eHorizon = EHorizonMapper.mapToEHorizon(electronicHorizon)
        var outCount = 0
        fun countOutgoingEdges(edge: Edge) {
            outCount += edge.out.size
            edge.out.forEach { countOutgoingEdges(it) }
        }
        countOutgoingEdges(eHorizon.start)

        assertEquals(14, outCount)
    }

    @Test
    fun `mapToEHorizonPosition should map position`() {
        val graphPosition: GraphPosition = mockk {
            every { edgeId } returns 6900441309522L
            every { percentAlong } returns 0.2880
        }

        val eHorizonPosition = EHorizonMapper.mapToEHorizonPosition(graphPosition)

        assertEquals(6900441309522L, eHorizonPosition.edgeId)
        assertEquals(0.2880, eHorizonPosition.percentAlong, 0.0001)
    }

    @Test
    fun `two identical graphs should be equal`() {
        val lhsFromNative = EHorizonTestUtil.loadSmallGraph()
        val rhsFromNative = EHorizonTestUtil.loadSmallGraph()

        val lhs = EHorizonMapper.mapToEHorizon(lhsFromNative)
        val rhs = EHorizonMapper.mapToEHorizon(rhsFromNative)

        assertEquals(lhs, rhs)
        assertEquals(lhs.hashCode(), rhs.hashCode())
    }

    @Test
    fun `toString should not overflow`() {
        val lhsFromNative = EHorizonTestUtil.loadLongBranch()

        val stringValue = EHorizonMapper.mapToEHorizon(lhsFromNative).toString()

        assertTrue(stringValue.length > 5000)
    }
}
