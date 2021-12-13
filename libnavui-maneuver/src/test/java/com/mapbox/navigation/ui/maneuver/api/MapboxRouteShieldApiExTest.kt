package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.HttpResponseCallback
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.RoadShieldError
import com.mapbox.navigation.ui.maneuver.model.RoadShieldResult
import com.mapbox.navigation.ui.maneuver.model.TextComponentNode
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.ui.shield.model.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxRouteShieldApiExTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val mapboxRouteShieldApi = spyk(MapboxRouteShieldApi())

    private val testManeuvers = listOf<Maneuver>(
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_0"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/primary_0"
                        }
                    }
                )
            }
            every { secondary } returns null
            every { sub } returns null
        },
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_1"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<TextComponentNode>()
                    }
                )
            }
            every { secondary } returns null
            every { sub } returns null
        },
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_2"
                every { componentList } returns listOf()
            }
            every { secondary } returns mockk {
                every { id } returns "secondary_2"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/secondary_2"
                        }
                    }
                )
            }
            every { sub } returns null
        },
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_3"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<TextComponentNode>()
                    },
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/primary_3"
                        }
                    }
                )
            }
            every { secondary } returns mockk {
                every { id } returns "secondary_3"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/secondary_3"
                        }
                    }
                )
            }
            every { sub } returns mockk {
                every { id } returns "sub_3"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/sub_3"
                        }
                    }
                )
            }
        },
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_4"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns null
                        }
                    }
                )
            }
            every { secondary } returns null
            every { sub } returns mockk {
                every { id } returns "sub_4"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns null
                        }
                    }
                )
            }
        },
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_5"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            // same as primary_0 intentionally
                            every { shieldUrl } returns "https://shield.mapbox.com/primary_0"
                        }
                    }
                )
            }
            every { secondary } returns null
            every { sub } returns null
        }
    )

    @Test
    fun `when maneuver contains no shield urls`() = coroutineRule.runBlockingTest {
        val callback: RoadShieldCallback = mockk(relaxed = true)
        val maneuverSlot = slot<List<Maneuver>>()
        val shieldSlot = slot<Map<String, RoadShield>>()
        val errorSlot = slot<Map<String, RoadShieldError>>()
        val routeShieldCallback = slot<RouteShieldCallback>()
        every {
            mapboxRouteShieldApi.getLegacyRouteShields(emptyList(), capture(routeShieldCallback))
        } answers {
            routeShieldCallback.captured.onRoadShields(emptyList())
        }
        mapboxRouteShieldApi.getRouteShieldsFrom(emptyList(), callback)

        verify(exactly = 1) {
            callback.onRoadShields(capture(maneuverSlot), capture(shieldSlot), capture(errorSlot))
        }
        assertEquals(0, errorSlot.captured.size)
        assertEquals(0, shieldSlot.captured.size)
    }

    @Test
    fun `when maneuver contains only legacy urls and has all shields`() {
        val maneuvers = listOf(testManeuvers[3])
        val callback: RoadShieldCallback = mockk(relaxed = true)
        val maneuverSlot = slot<List<Maneuver>>()
        val shieldSlot = slot<Map<String, RoadShield>>()
        val errorSlot = slot<Map<String, RoadShieldError>>()
        val routeShieldCallback = slot<RouteShieldCallback>()
        val routeShields = listOf<Expected<RouteShieldError, RouteShieldResult>>(
            ExpectedFactory.createValue(
                RouteShieldResult(
                    shield = RouteShield.MapboxLegacyShield(
                        "https://shield.mapbox.com/primary_3",
                        byteArrayOf(12, 13)
                    ),
                    origin = RouteShieldOrigin(
                        false,
                        "https://shield.mapbox.com/primary_3",
                        errorMessage = ""
                    )
                )
            ),
            ExpectedFactory.createValue(
                RouteShieldResult(
                    shield = RouteShield.MapboxLegacyShield(
                        "https://shield.mapbox.com/secondary_3",
                        byteArrayOf(13, 14)
                    ),
                    origin = RouteShieldOrigin(
                        false,
                        "https://shield.mapbox.com/secondary_3",
                        errorMessage = ""
                    )
                )
            ),
            ExpectedFactory.createValue(
                RouteShieldResult(
                    shield = RouteShield.MapboxLegacyShield(
                        "https://shield.mapbox.com/sub_3",
                        byteArrayOf(14, 15)
                    ),
                    origin = RouteShieldOrigin(
                        false,
                        "https://shield.mapbox.com/sub_3",
                        errorMessage = ""
                    )
                )
            )
        )
        every {
            mapboxRouteShieldApi.getLegacyRouteShields(any(), capture(routeShieldCallback))
        } answers {
            routeShieldCallback.captured.onRoadShields(routeShields)
        }

        mapboxRouteShieldApi.getRouteShieldsFrom(maneuvers = maneuvers, callback = callback)

        verify(exactly = 1) {
            callback.onRoadShields(capture(maneuverSlot), capture(shieldSlot), capture(errorSlot))
        }
        assertEquals(0, errorSlot.captured.size)
        assertEquals(3, shieldSlot.captured.size)
    }

    @Test
    fun `when maneuver contains only legacy urls and has error and shields`() {
        val maneuvers = listOf(testManeuvers[3])
        val callback: RoadShieldCallback = mockk(relaxed = true)
        val maneuverSlot = slot<List<Maneuver>>()
        val shieldSlot = slot<Map<String, RoadShield>>()
        val errorSlot = slot<Map<String, RoadShieldError>>()
        val routeShieldCallback = slot<RouteShieldCallback>()
        val routeShields = listOf<Expected<RouteShieldError, RouteShieldResult>>(
            ExpectedFactory.createError(
                RouteShieldError(
                    url = "https://shield.mapbox.com/primary_3",
                    errorMessage = """
                        For legacy shield url: https://shield.mapbox.com/primary_3
                        an error was received with message: Resource is missing.
                    """.trimIndent()
                )
            ),
            ExpectedFactory.createValue(
                RouteShieldResult(
                    shield = RouteShield.MapboxLegacyShield(
                        "https://shield.mapbox.com/secondary_3",
                        byteArrayOf(13, 14)
                    ),
                    origin = RouteShieldOrigin(
                        false,
                        "https://shield.mapbox.com/secondary_3",
                        errorMessage = ""
                    )
                )
            ),
            ExpectedFactory.createValue(
                RouteShieldResult(
                    shield = RouteShield.MapboxLegacyShield(
                        "https://shield.mapbox.com/sub_3",
                        byteArrayOf(14, 15)
                    ),
                    origin = RouteShieldOrigin(
                        false,
                        "https://shield.mapbox.com/sub_3",
                        errorMessage = ""
                    )
                )
            )
        )
        every {
            mapboxRouteShieldApi.getLegacyRouteShields(any(), capture(routeShieldCallback))
        } answers {
            routeShieldCallback.captured.onRoadShields(routeShields)
        }

        mapboxRouteShieldApi.getRouteShieldsFrom(maneuvers = maneuvers, callback = callback)

        verify(exactly = 1) {
            callback.onRoadShields(capture(maneuverSlot), capture(shieldSlot), capture(errorSlot))
        }
        assertEquals(1, errorSlot.captured.size)
        assertEquals(2, shieldSlot.captured.size)
    }

}
