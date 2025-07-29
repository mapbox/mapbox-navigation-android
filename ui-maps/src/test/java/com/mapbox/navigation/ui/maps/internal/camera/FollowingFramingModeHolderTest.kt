package com.mapbox.navigation.ui.maps.internal.camera

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
}
