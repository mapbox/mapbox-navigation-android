package com.mapbox.navigation.ui.maps.internal.camera

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FollowingFramingModeHolderTest {

    private lateinit var holder: FollowingFramingModeHolder

    @Before
    fun setup() {
        holder = FollowingFramingModeHolder()
    }

    @Test
    fun `initial state has LOCATION_INDICATOR mode and prevMode`() {
        assertEquals(FollowingFramingMode.LOCATION_INDICATOR, holder.mode)
        assertEquals(FollowingFramingMode.LOCATION_INDICATOR, holder.prevMode)
    }

    @Test
    fun `setting mode to different value updates prevMode`() {
        holder.mode = FollowingFramingMode.LOCATION_INDICATOR
        holder.mode = FollowingFramingMode.MULTIPLE_POINTS

        assertEquals(FollowingFramingMode.MULTIPLE_POINTS, holder.mode)
        assertEquals(FollowingFramingMode.LOCATION_INDICATOR, holder.prevMode)
    }

    @Test
    fun `setting mode to same value updates prevMode`() {
        holder.mode = FollowingFramingMode.LOCATION_INDICATOR
        holder.mode = FollowingFramingMode.LOCATION_INDICATOR

        assertEquals(FollowingFramingMode.LOCATION_INDICATOR, holder.mode)
        assertEquals(FollowingFramingMode.LOCATION_INDICATOR, holder.prevMode)
    }

    @Test
    fun `alternating between two modes works correctly`() {
        holder.mode = FollowingFramingMode.MULTIPLE_POINTS
        assertEquals(FollowingFramingMode.MULTIPLE_POINTS, holder.mode)
        assertEquals(FollowingFramingMode.LOCATION_INDICATOR, holder.prevMode)

        holder.mode = FollowingFramingMode.LOCATION_INDICATOR
        assertEquals(FollowingFramingMode.LOCATION_INDICATOR, holder.mode)
        assertEquals(FollowingFramingMode.MULTIPLE_POINTS, holder.prevMode)

        holder.mode = FollowingFramingMode.MULTIPLE_POINTS
        assertEquals(FollowingFramingMode.MULTIPLE_POINTS, holder.mode)
        assertEquals(FollowingFramingMode.LOCATION_INDICATOR, holder.prevMode)

        holder.mode = FollowingFramingMode.LOCATION_INDICATOR
        assertEquals(FollowingFramingMode.LOCATION_INDICATOR, holder.mode)
        assertEquals(FollowingFramingMode.MULTIPLE_POINTS, holder.prevMode)
    }

    @Test
    fun `addObserver with custom initial mode invokes observer with that mode`() {
        holder.mode = FollowingFramingMode.MULTIPLE_POINTS
        val observer: (FollowingFramingMode?) -> Unit = mockk(relaxed = true)

        holder.addObserver(observer)

        verify(exactly = 1) { observer(FollowingFramingMode.MULTIPLE_POINTS) }
    }

    @Test
    fun `observer is notified when mode changes to different value`() {
        val observer: (FollowingFramingMode?) -> Unit = mockk(relaxed = true)
        holder.addObserver(observer)

        holder.mode = FollowingFramingMode.MULTIPLE_POINTS

        verifyOrder {
            observer(FollowingFramingMode.LOCATION_INDICATOR) // initial call
            observer(FollowingFramingMode.MULTIPLE_POINTS) // mode change
        }
    }

    @Test
    fun `observer is not notified when mode is set to same value`() {
        val observer: (FollowingFramingMode?) -> Unit = mockk(relaxed = true)
        holder.addObserver(observer)

        clearMocks(observer, answers = false)

        holder.mode = FollowingFramingMode.LOCATION_INDICATOR

        verify(exactly = 0) { observer(any()) }
    }

    @Test
    fun `multiple observers are all notified on mode change`() {
        val observer1: (FollowingFramingMode?) -> Unit = mockk(relaxed = true)
        val observer2: (FollowingFramingMode?) -> Unit = mockk(relaxed = true)
        val observer3: (FollowingFramingMode?) -> Unit = mockk(relaxed = true)

        holder.addObserver(observer1)
        holder.addObserver(observer2)
        holder.addObserver(observer3)

        holder.mode = FollowingFramingMode.MULTIPLE_POINTS

        verify(exactly = 1) { observer1(FollowingFramingMode.MULTIPLE_POINTS) }
        verify(exactly = 1) { observer2(FollowingFramingMode.MULTIPLE_POINTS) }
        verify(exactly = 1) { observer3(FollowingFramingMode.MULTIPLE_POINTS) }
    }

    @Test
    fun `removeObserver stops notifications to that observer`() {
        val observer: (FollowingFramingMode?) -> Unit = mockk(relaxed = true)
        holder.addObserver(observer)

        holder.removeObserver(observer)
        holder.mode = FollowingFramingMode.MULTIPLE_POINTS

        // Only the initial call from addObserver, no call after removal
        verify(exactly = 1) { observer(FollowingFramingMode.LOCATION_INDICATOR) }
        verify(exactly = 0) { observer(FollowingFramingMode.MULTIPLE_POINTS) }
    }
}
