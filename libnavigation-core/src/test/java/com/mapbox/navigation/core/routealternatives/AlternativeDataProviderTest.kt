package com.mapbox.navigation.core.routealternatives

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.utils.DecodeUtils
import com.mapbox.navigation.base.utils.DecodeUtils.stepsGeometryToPoints
import com.mapbox.navigation.core.internal.AlternativeDataProvider
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlternativeDataProviderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.DefaultDispatcher } returns coroutineRule.testDispatcher
        mockkStatic(DecodeUtils::class)
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
        unmockkStatic(DecodeUtils::class)
    }

    @Test
    fun `getRouteProgressData - fork passed`() = coroutineRule.runBlockingTest {
        val primaryRouteGeometryIndex = 200
        val primaryRouteProgressData = RouteProgressData(4, primaryRouteGeometryIndex, 30)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 5,
            primaryForkRouteGeometryIndex = primaryRouteGeometryIndex - 1,
            primaryForkLegGeometryIndex = 80,
            alternativeForkLegIndex = 3,
            alternativeForkRouteGeometryIndex = 90,
            alternativeForkLegGeometryIndex = 40,
            emptyList()
        )
        val expected = RouteProgressData(3, 90, 40)

        val actual = AlternativeDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `getRouteProgressData - fork passed, alternative is longer than primary`() = coroutineRule.runBlockingTest {
        val primaryRouteGeometryIndex = 200
        val primaryRouteProgressData = RouteProgressData(3, primaryRouteGeometryIndex, 40)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 3,
            primaryForkRouteGeometryIndex = primaryRouteGeometryIndex - 1,
            primaryForkLegGeometryIndex = 40,
            alternativeForkLegIndex = 5,
            alternativeForkRouteGeometryIndex = 130,
            alternativeForkLegGeometryIndex = 80,
            emptyList()
        )
        val expected = RouteProgressData(5, 130, 80)

        val actual = AlternativeDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `getRouteProgressData - on fork`() = coroutineRule.runBlockingTest {
        val primaryRouteGeometryIndex = 200
        val primaryRouteProgressData = RouteProgressData(4, primaryRouteGeometryIndex, 30)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 5,
            primaryForkRouteGeometryIndex = primaryRouteGeometryIndex,
            primaryForkLegGeometryIndex = 80,
            alternativeForkLegIndex = 3,
            alternativeForkRouteGeometryIndex = 90,
            alternativeForkLegGeometryIndex = 40,
            emptyList()
        )
        val expected = RouteProgressData(3, 90, 40)

        val actual = AlternativeDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `getRouteProgressData - before fork, alternative starts after the primary route ahead of current position`() =
        coroutineRule.runBlockingTest {
            val primaryRouteProgressData = RouteProgressData(0, 3, 3)
            val alternativeMetadata = alternativeRouteMetadata(
                primaryForkLegIndex = 0,
                primaryForkRouteGeometryIndex = 100,
                primaryForkLegGeometryIndex = 100,
                alternativeForkLegIndex = 0,
                alternativeForkRouteGeometryIndex = 80,
                alternativeForkLegGeometryIndex = 80,
                listOf(
                    listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                )
            )
            val expected = RouteProgressData(0, 0, 0)

            val actual = AlternativeDataProvider.getRouteProgressData(
                primaryRouteProgressData,
                alternativeMetadata
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `getRouteProgressData - before fork, alternative starts together with the primary route`() = coroutineRule.runBlockingTest {
        val primaryRouteProgressData = RouteProgressData(4, 200, 30)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 5,
            primaryForkRouteGeometryIndex = 250,
            primaryForkLegGeometryIndex = 80,
            alternativeForkLegIndex = 5,
            alternativeForkRouteGeometryIndex = 250,
            alternativeForkLegGeometryIndex = 70,
            listOf(
                listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                listOf(List(23) { mockk() }),
                listOf(List(32) { mockk() }, List(19) { mockk() }),
                listOf(List(14) { mockk() }, List(23) { mockk() }),
                // 136 points for legs #0-#3
                // (135 if you don't count the current leg's starting point)
                listOf(List(8) { mockk() }),
            )
        )
        val expected = RouteProgressData(4, 200, 65)

        val actual = AlternativeDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `getRouteProgressData - before fork, alternative starts after the primary route on the first leg`() =
        coroutineRule.runBlockingTest {
            val primaryRouteProgressData = RouteProgressData(4, 200, 35)
            val alternativeMetadata = alternativeRouteMetadata(
                primaryForkLegIndex = 5,
                primaryForkRouteGeometryIndex = 250,
                primaryForkLegGeometryIndex = 85,
                alternativeForkLegIndex = 5,
                alternativeForkRouteGeometryIndex = 220,
                alternativeForkLegGeometryIndex = 85,
                listOf(
                    listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                    listOf(List(23) { mockk() }),
                    listOf(List(32) { mockk() }, List(19) { mockk() }),
                    listOf(List(14) { mockk() }, List(23) { mockk() }),
                    // 136 points for legs #0-#3
                    // (135 if you don't count the current leg's starting point)
                    listOf(List(8) { mockk() }),
                )
            )
            val expected = RouteProgressData(4, 170, 35)

            val actual = AlternativeDataProvider.getRouteProgressData(
                primaryRouteProgressData,
                alternativeMetadata
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `getRouteProgressData - before fork, alternative starts after the primary route on the second leg`() =
        coroutineRule.runBlockingTest {
            val primaryRouteProgressData = RouteProgressData(4, 200, 35)
            val alternativeMetadata = alternativeRouteMetadata(
                primaryForkLegIndex = 5,
                primaryForkRouteGeometryIndex = 250,
                primaryForkLegGeometryIndex = 85,
                alternativeForkLegIndex = 3,
                alternativeForkRouteGeometryIndex = 220,
                alternativeForkLegGeometryIndex = 81,
                listOf(
                    listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                    listOf(List(23) { mockk() }),
                    // 136 points for legs #0-#1
                    // (135 if you don't count the current leg's starting point)
                    listOf(List(32) { mockk() }, List(19) { mockk() }),
                )
            )
            val expected = RouteProgressData(2, 170, 119)

            val actual = AlternativeDataProvider.getRouteProgressData(
                primaryRouteProgressData,
                alternativeMetadata
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `getRouteProgressData - before fork, alternative starts after the primary route on the current leg`() =
        coroutineRule.runBlockingTest {
            val primaryRouteProgressData = RouteProgressData(4, 200, 60)
            val alternativeMetadata = alternativeRouteMetadata(
                primaryForkLegIndex = 4,
                primaryForkRouteGeometryIndex = 250,
                primaryForkLegGeometryIndex = 110,
                alternativeForkLegIndex = 0,
                alternativeForkRouteGeometryIndex = 90,
                alternativeForkLegGeometryIndex = 90,
                listOf(
                    listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                    listOf(List(23) { mockk() }),
                    listOf(List(32) { mockk() }, List(19) { mockk() }),
                    listOf(List(14) { mockk() }, List(23) { mockk() }),
                )
            )
            val expected = RouteProgressData(0, 40, 40)

            val actual = AlternativeDataProvider.getRouteProgressData(
                primaryRouteProgressData,
                alternativeMetadata
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `getRouteProgressData - before fork, primary starts after the alternative route on the current leg`() =
        coroutineRule.runBlockingTest {
            // primary leg and route indices diff is 140
            val primaryRouteProgressData = RouteProgressData(0, 40, 40)
            val alternativeMetadata = alternativeRouteMetadata(
                primaryForkLegIndex = 0,
                primaryForkRouteGeometryIndex = 90,
                primaryForkLegGeometryIndex = 90,
                alternativeForkLegIndex = 4,
                alternativeForkRouteGeometryIndex = 250,
                alternativeForkLegGeometryIndex = 115,
                listOf(
                    listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                    listOf(List(23) { mockk() }),
                    listOf(List(32) { mockk() }, List(19) { mockk() }),
                    listOf(
                        List(14) { mockk() },
                        List(23) { mockk() }
                    ),
                    // 136 points for legs #0-#3
                    // (135 if you don't count the current leg's starting point)
                    listOf(List(8) { mockk() }),
                )
            )
            val expected = RouteProgressData(4, 200, 65)

            val actual = AlternativeDataProvider.getRouteProgressData(
                primaryRouteProgressData,
                alternativeMetadata
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `getAlternativeLegIndex - fork passed`() = coroutineRule.runBlockingTest {
        val primaryRouteGeometryIndex = 200
        val primaryRouteProgressData = RouteProgressData(4, primaryRouteGeometryIndex, 30)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 5,
            primaryForkRouteGeometryIndex = primaryRouteGeometryIndex - 1,
            primaryForkLegGeometryIndex = 80,
            alternativeForkLegIndex = 3,
            alternativeForkRouteGeometryIndex = 90,
            alternativeForkLegGeometryIndex = 40,
            emptyList()
        )
        val expected = 3

        val actual = AlternativeDataProvider.getAlternativeLegIndex(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `getAlternativeLegIndex - fork passed, alternative is longer than primary`() = coroutineRule.runBlockingTest {
        val primaryRouteGeometryIndex = 200
        val primaryRouteProgressData = RouteProgressData(3, primaryRouteGeometryIndex, 40)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 3,
            primaryForkRouteGeometryIndex = primaryRouteGeometryIndex - 1,
            primaryForkLegGeometryIndex = 40,
            alternativeForkLegIndex = 5,
            alternativeForkRouteGeometryIndex = 130,
            alternativeForkLegGeometryIndex = 80,
            emptyList()
        )
        val expected = 5

        val actual = AlternativeDataProvider.getAlternativeLegIndex(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `getAlternativeLegIndex - on fork`() = coroutineRule.runBlockingTest {
        val primaryRouteGeometryIndex = 200
        val primaryRouteProgressData = RouteProgressData(4, primaryRouteGeometryIndex, 30)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 5,
            primaryForkRouteGeometryIndex = primaryRouteGeometryIndex,
            primaryForkLegGeometryIndex = 80,
            alternativeForkLegIndex = 3,
            alternativeForkRouteGeometryIndex = 90,
            alternativeForkLegGeometryIndex = 40,
            emptyList()
        )
        val expected = 3

        val actual = AlternativeDataProvider.getAlternativeLegIndex(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `getAlternativeLegIndex - before fork, alternative starts after the primary route ahead of current position`() =
        coroutineRule.runBlockingTest {
            val primaryRouteProgressData = RouteProgressData(0, 3, 3)
            val alternativeMetadata = alternativeRouteMetadata(
                primaryForkLegIndex = 0,
                primaryForkRouteGeometryIndex = 100,
                primaryForkLegGeometryIndex = 100,
                alternativeForkLegIndex = 0,
                alternativeForkRouteGeometryIndex = 80,
                alternativeForkLegGeometryIndex = 80,
                listOf(
                    listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                )
            )
            val expected = 0

            val actual = AlternativeDataProvider.getAlternativeLegIndex(
                primaryRouteProgressData,
                alternativeMetadata
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `getAlternativeLegIndex - before fork, alternative starts together with the primary route`() = coroutineRule.runBlockingTest {
        val primaryRouteProgressData = RouteProgressData(4, 200, 30)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 5,
            primaryForkRouteGeometryIndex = 250,
            primaryForkLegGeometryIndex = 80,
            alternativeForkLegIndex = 5,
            alternativeForkRouteGeometryIndex = 250,
            alternativeForkLegGeometryIndex = 70,
            listOf(
                listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                listOf(List(23) { mockk() }),
                listOf(List(32) { mockk() }, List(19) { mockk() }),
                listOf(List(14) { mockk() }, List(23) { mockk() }),
                // 136 points for legs #0-#3
                // (135 if you don't count the current leg's starting point)
                listOf(List(8) { mockk() }),
            )
        )
        val expected = 4

        val actual = AlternativeDataProvider.getAlternativeLegIndex(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `getAlternativeLegIndex - before fork, alternative starts after the primary route on the first leg`() =
        coroutineRule.runBlockingTest {
            val primaryRouteProgressData = RouteProgressData(4, 200, 35)
            val alternativeMetadata = alternativeRouteMetadata(
                primaryForkLegIndex = 5,
                primaryForkRouteGeometryIndex = 250,
                primaryForkLegGeometryIndex = 85,
                alternativeForkLegIndex = 5,
                alternativeForkRouteGeometryIndex = 220,
                alternativeForkLegGeometryIndex = 85,
                listOf(
                    listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                    listOf(List(23) { mockk() }),
                    listOf(List(32) { mockk() }, List(19) { mockk() }),
                    listOf(List(14) { mockk() }, List(23) { mockk() }),
                    // 136 points for legs #0-#3
                    // (135 if you don't count the current leg's starting point)
                    listOf(List(8) { mockk() }),
                )
            )
            val expected = 4

            val actual = AlternativeDataProvider.getAlternativeLegIndex(
                primaryRouteProgressData,
                alternativeMetadata
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `getAlternativeLegIndex - before fork, alternative starts after the primary route on the second leg`() =
        coroutineRule.runBlockingTest {
            val primaryRouteProgressData = RouteProgressData(4, 200, 35)
            val alternativeMetadata = alternativeRouteMetadata(
                primaryForkLegIndex = 5,
                primaryForkRouteGeometryIndex = 250,
                primaryForkLegGeometryIndex = 85,
                alternativeForkLegIndex = 3,
                alternativeForkRouteGeometryIndex = 220,
                alternativeForkLegGeometryIndex = 81,
                listOf(
                    listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                    listOf(List(23) { mockk() }),
                    // 136 points for legs #0-#1
                    // (135 if you don't count the current leg's starting point)
                    listOf(List(32) { mockk() }, List(19) { mockk() }),
                )
            )
            val expected = 2

            val actual = AlternativeDataProvider.getAlternativeLegIndex(
                primaryRouteProgressData,
                alternativeMetadata
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `getAlternativeLegIndex - before fork, alternative starts after the primary route on the current leg`() =
        coroutineRule.runBlockingTest {
            val primaryRouteProgressData = RouteProgressData(4, 200, 60)
            val alternativeMetadata = alternativeRouteMetadata(
                primaryForkLegIndex = 4,
                primaryForkRouteGeometryIndex = 250,
                primaryForkLegGeometryIndex = 110,
                alternativeForkLegIndex = 0,
                alternativeForkRouteGeometryIndex = 90,
                alternativeForkLegGeometryIndex = 90,
                listOf(
                    listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                    listOf(List(23) { mockk() }),
                    listOf(List(32) { mockk() }, List(19) { mockk() }),
                    listOf(List(14) { mockk() }, List(23) { mockk() }),
                )
            )
            val expected = 0

            val actual = AlternativeDataProvider.getAlternativeLegIndex(
                primaryRouteProgressData,
                alternativeMetadata
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `getAlternativeLegIndex - before fork, primary starts after the alternative route on the current leg`() =
        coroutineRule.runBlockingTest {
            // primary leg and route indices diff is 140
            val primaryRouteProgressData = RouteProgressData(0, 40, 40)
            val alternativeMetadata = alternativeRouteMetadata(
                primaryForkLegIndex = 0,
                primaryForkRouteGeometryIndex = 90,
                primaryForkLegGeometryIndex = 90,
                alternativeForkLegIndex = 4,
                alternativeForkRouteGeometryIndex = 250,
                alternativeForkLegGeometryIndex = 115,
                listOf(
                    listOf(List(10) { mockk() }, List(7) { mockk() }, List(15) { mockk() }),
                    listOf(List(23) { mockk() }),
                    listOf(List(32) { mockk() }, List(19) { mockk() }),
                    listOf(
                        List(14) { mockk() },
                        List(23) { mockk() }
                    ),
                    // 136 points for legs #0-#3
                    // (135 if you don't count the current leg's starting point)
                    listOf(List(8) { mockk() }),
                )
            )
            val expected = 4

            val actual = AlternativeDataProvider.getAlternativeLegIndex(
                primaryRouteProgressData,
                alternativeMetadata
            )

            assertEquals(expected, actual)
        }

    private fun alternativeRouteMetadata(
        primaryForkLegIndex: Int,
        primaryForkRouteGeometryIndex: Int,
        primaryForkLegGeometryIndex: Int,
        alternativeForkLegIndex: Int,
        alternativeForkRouteGeometryIndex: Int,
        alternativeForkLegGeometryIndex: Int,
        stepsGeometries: List<List<List<Point>>>
    ): AlternativeRouteMetadata {
        return AlternativeRouteMetadata(
            mockk {
                every { directionsRoute } returns mockk {
                    every { stepsGeometryToPoints() } returns stepsGeometries
                }
            },
            AlternativeRouteIntersection(
                Point.fromLngLat(1.1, 2.2),
                alternativeForkRouteGeometryIndex,
                alternativeForkLegGeometryIndex,
                alternativeForkLegIndex
            ),
            AlternativeRouteIntersection(
                Point.fromLngLat(3.3, 4.4),
                primaryForkRouteGeometryIndex,
                primaryForkLegGeometryIndex,
                primaryForkLegIndex
            ),
            mockk(),
            mockk(),
            0
        )
    }
}
