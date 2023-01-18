package com.mapbox.navigation.core

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.routealternatives.AlternativeMetadataProvider
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.core.routealternatives.AlternativeRouteProgressDataProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RoutesProgressDataProviderTest {

    private val primaryRouteProgressDataProvider =
        mockk<PrimaryRouteProgressDataProvider>(relaxed = true)
    private val alternativeMetadataProvider = mockk<AlternativeMetadataProvider>(relaxed = true)
    private val sut = RoutesProgressDataProvider(
        primaryRouteProgressDataProvider,
        alternativeMetadataProvider
    )

    @Before
    fun setUp() {
        mockkObject(AlternativeRouteProgressDataProvider)
    }

    @After
    fun tearDown() {
        unmockkObject(AlternativeRouteProgressDataProvider)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getRoutesProgressData for empty routes`() = runBlockingTest {
        sut.getRoutesProgressData(emptyList())
    }

    @Test
    fun `getRoutesProgressData for primary route only`() = runBlockingTest {
        val primaryRouteProgressData = RouteProgressData(4, 5, 6)
        val primaryRoute = mockk<NavigationRoute>(relaxed = true)
        coEvery {
            primaryRouteProgressDataProvider.getRouteRefreshRequestDataOrWait()
        } returns primaryRouteProgressData
        val expected = RoutesProgressData(primaryRoute, primaryRouteProgressData, emptyList())

        val actual = sut.getRoutesProgressData(listOf(primaryRoute))

        assertEquals(expected, actual)
    }

    @Test
    fun `getRoutesProgressData for valid alternatives`() = runBlockingTest {
        val primaryRouteProgressData = RouteProgressData(4, 5, 6)
        val alternativeRoute1ProgressData = RouteProgressData(7, 8, 9)
        val alternativeRoute2ProgressData = RouteProgressData(10, 11, 12)
        val primaryRoute = mockk<NavigationRoute>(relaxed = true)
        val alternativeRoute1 = mockk<NavigationRoute>(relaxed = true)
        val alternativeRoute2 = mockk<NavigationRoute>(relaxed = true)
        val alternativeMetadata1 = mockk<AlternativeRouteMetadata>(relaxed = true)
        val alternativeMetadata2 = mockk<AlternativeRouteMetadata>(relaxed = true)
        coEvery {
            primaryRouteProgressDataProvider.getRouteRefreshRequestDataOrWait()
        } returns primaryRouteProgressData
        every {
            alternativeMetadataProvider.getMetadataFor(alternativeRoute1)
        } returns alternativeMetadata1
        every {
            alternativeMetadataProvider.getMetadataFor(alternativeRoute2)
        } returns alternativeMetadata2
        every {
            AlternativeRouteProgressDataProvider.getRouteProgressData(
                primaryRouteProgressData,
                alternativeMetadata1
            )
        } returns alternativeRoute1ProgressData
        every {
            AlternativeRouteProgressDataProvider.getRouteProgressData(
                primaryRouteProgressData,
                alternativeMetadata2
            )
        } returns alternativeRoute2ProgressData
        val expected = RoutesProgressData(
            primaryRoute,
            primaryRouteProgressData,
            listOf(
                alternativeRoute1 to alternativeRoute1ProgressData,
                alternativeRoute2 to alternativeRoute2ProgressData
            )
        )

        val actual = sut.getRoutesProgressData(
            listOf(primaryRoute, alternativeRoute1, alternativeRoute2)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `getRoutesProgressData for one invalid alternative`() = runBlockingTest {
        val primaryRouteProgressData = RouteProgressData(4, 5, 6)
        val alternativeRoute2ProgressData = RouteProgressData(10, 11, 12)
        val primaryRoute = mockk<NavigationRoute>(relaxed = true)
        val alternativeRoute1 = mockk<NavigationRoute>(relaxed = true)
        val alternativeRoute2 = mockk<NavigationRoute>(relaxed = true)
        val alternativeMetadata2 = mockk<AlternativeRouteMetadata>(relaxed = true)
        coEvery {
            primaryRouteProgressDataProvider.getRouteRefreshRequestDataOrWait()
        } returns primaryRouteProgressData
        every { alternativeMetadataProvider.getMetadataFor(alternativeRoute1) } returns null
        every {
            alternativeMetadataProvider.getMetadataFor(alternativeRoute2)
        } returns alternativeMetadata2
        every {
            AlternativeRouteProgressDataProvider.getRouteProgressData(
                primaryRouteProgressData,
                alternativeMetadata2
            )
        } returns alternativeRoute2ProgressData
        val expected = RoutesProgressData(
            primaryRoute,
            primaryRouteProgressData,
            listOf(alternativeRoute1 to null, alternativeRoute2 to alternativeRoute2ProgressData)
        )

        val actual = sut.getRoutesProgressData(
            listOf(primaryRoute, alternativeRoute1, alternativeRoute2)
        )

        assertEquals(expected, actual)
    }
}
