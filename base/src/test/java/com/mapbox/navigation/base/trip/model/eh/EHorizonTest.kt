package com.mapbox.navigation.base.trip.model.eh

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 *                           start
 *                   /         |          \
 *                 one        two       three
 *               / | | \   (not mpp)      |  |
 *         four five six seven        eight nine
 *                |     (not mpp)      |  |
 *               ten              eleven twelve
 *              /   \
 *       thirteen  fourteen
 */

class EHorizonTest {
    private val start: EHorizonEdge = mockk(relaxed = true)
    private val one: EHorizonEdge = mockk(relaxed = true)
    private val two: EHorizonEdge = mockk(relaxed = true)
    private val three: EHorizonEdge = mockk(relaxed = true)
    private val four: EHorizonEdge = mockk(relaxed = true)
    private val five: EHorizonEdge = mockk(relaxed = true)
    private val six: EHorizonEdge = mockk(relaxed = true)
    private val seven: EHorizonEdge = mockk(relaxed = true)
    private val eight: EHorizonEdge = mockk(relaxed = true)
    private val nine: EHorizonEdge = mockk(relaxed = true)
    private val ten: EHorizonEdge = mockk(relaxed = true)
    private val eleven: EHorizonEdge = mockk(relaxed = true)
    private val twelve: EHorizonEdge = mockk(relaxed = true)
    private val thirteen: EHorizonEdge = mockk(relaxed = true)
    private val fourteen: EHorizonEdge = mockk(relaxed = true)

    private val eHorizonGraphPosition: EHorizonGraphPosition = mockk(relaxed = true)
    private val eHorizonPosition: EHorizonPosition = mockk(relaxed = true)
    private val eHorizon = EHorizon(start)

    @Before
    fun setUp() {
        every { start.isMpp() } returns true
        every { one.isMpp() } returns true
        every { two.isMpp() } returns false // not mpp
        every { three.isMpp() } returns true
        every { four.isMpp() } returns true
        every { five.isMpp() } returns true
        every { six.isMpp() } returns true
        every { seven.isMpp() } returns false // not mpp
        every { eight.isMpp() } returns true
        every { nine.isMpp() } returns true
        every { ten.isMpp() } returns true
        every { eleven.isMpp() } returns true
        every { twelve.isMpp() } returns true
        every { thirteen.isMpp() } returns true
        every { fourteen.isMpp() } returns true

        every { start.out } returns listOf(one, two, three)
        every { one.out } returns listOf(four, five, six, seven)
        every { two.out } returns emptyList()
        every { three.out } returns listOf(eight, nine)
        every { four.out } returns emptyList()
        every { five.out } returns listOf(ten)
        every { six.out } returns emptyList()
        every { seven.out } returns emptyList()
        every { eight.out } returns listOf(eleven, twelve)
        every { nine.out } returns emptyList()
        every { ten.out } returns listOf(thirteen, fourteen)
        every { eleven.out } returns emptyList()
        every { twelve.out } returns emptyList()
        every { thirteen.out } returns emptyList()
        every { fourteen.out } returns emptyList()
    }

    @Test
    fun mpp() {
        val mpp = eHorizon.mpp()
        val expectedMpp = listOf(
            listOf(start, one, four),
            listOf(start, one, five, ten, thirteen),
            listOf(start, one, five, ten, fourteen),
            listOf(start, one, six),
            listOf(start, three, eight, eleven),
            listOf(start, three, eight, twelve),
            listOf(start, three, nine),
        )

        assertEquals(expectedMpp, mpp)
    }

    @Test
    fun `mpp from position`() {
        every { eHorizonPosition.eHorizonGraphPosition } returns eHorizonGraphPosition
        every { eHorizonGraphPosition.edgeId } returns EDGE_ID
        every { one.id } returns EDGE_ID

        val mpp = eHorizon.mpp(eHorizonPosition)
        val expectedMpp = listOf(
            listOf(one, four),
            listOf(one, five, ten, thirteen),
            listOf(one, five, ten, fourteen),
            listOf(one, six),
        )

        assertEquals(expectedMpp, mpp)
    }

    private companion object {
        private const val EDGE_ID = 1L
    }
}
