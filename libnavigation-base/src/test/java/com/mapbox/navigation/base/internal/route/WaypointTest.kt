package com.mapbox.navigation.base.internal.route

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class WaypointTest(
    private val internalType: Waypoint.InternalType,
    private val expected: Int,
) {

    companion object {

        @Parameterized.Parameters(name = "{0} to {1}")
        @JvmStatic
        fun data(): Collection<Array<Any>> = listOf<Array<Any>>(
            arrayOf(Waypoint.InternalType.EvChargingUser, Waypoint.EV_CHARGING_USER),
            arrayOf(Waypoint.InternalType.EvChargingServer, Waypoint.EV_CHARGING_SERVER),
            arrayOf(Waypoint.InternalType.Silent, Waypoint.SILENT),
            arrayOf(Waypoint.InternalType.Regular, Waypoint.REGULAR),
        ).also {
            assertEquals(Waypoint.InternalType.values().size, it.size)
        }
    }

    @Test
    fun waypointType() {
        val waypoint = Waypoint(mockk(), "name", null, internalType)
        assertEquals(expected, waypoint.type)
    }
}
