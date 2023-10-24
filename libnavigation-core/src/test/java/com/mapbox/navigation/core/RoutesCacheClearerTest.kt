package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.clearCache
import com.mapbox.navigation.base.utils.DecodeUtils
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.preview.RoutesPreviewUpdate
import com.mapbox.navigation.core.testutil.createRoutesUpdatedResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoutesCacheClearerTest {

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    private val sut = RoutesCacheClearer()

    @Before
    fun setUp() {
        mockkStatic(DecodeUtils::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(DecodeUtils::class)
    }

    @Test
    fun onRoutesChanged_emptyNoPreviewedRoutes() {
        sut.onRoutesChanged(createRoutesUpdatedResult(emptyList(), ""))

        verify(exactly = 1) { DecodeUtils.clearCache() }
    }

    @Test
    fun onRoutesChanged_nonEmptyNoPreviewedRoutes() {
        sut.onRoutesChanged(createRoutesUpdatedResult(createNavigationRoutes(), ""))

        verify(exactly = 0) { DecodeUtils.clearCache() }
    }

    @Test
    fun onRoutesChanged_emptyHasPreviewedRoutes() {
        sut.routesPreviewUpdated(
            RoutesPreviewUpdate("", RoutesPreview(listOf(mockk()), emptyList(), listOf(mockk()), 0))
        )
        clearStaticMockk(DecodeUtils::class)

        sut.onRoutesChanged(createRoutesUpdatedResult(emptyList(), ""))

        verify(exactly = 0) { DecodeUtils.clearCache() }
    }

    @Test
    fun onRoutesChanged_emptyClearedPreviewedRoutes() {
        sut.routesPreviewUpdated(
            RoutesPreviewUpdate("", RoutesPreview(listOf(mockk()), emptyList(), listOf(mockk()), 0))
        )
        sut.routesPreviewUpdated(
            RoutesPreviewUpdate("", mockk { every { routesList } returns emptyList() })
        )
        clearStaticMockk(DecodeUtils::class)

        sut.onRoutesChanged(createRoutesUpdatedResult(emptyList(), ""))

        verify(exactly = 1) { DecodeUtils.clearCache() }
    }

    @Test
    fun routesPreviewUpdated_nullPreviewAndNoActiveRoutes() {
        sut.routesPreviewUpdated(RoutesPreviewUpdate("", null))

        verify(exactly = 1) { DecodeUtils.clearCache() }
    }

    @Test
    fun routesPreviewUpdated_emptyRoutesAndNoActiveRoutes() {
        sut.routesPreviewUpdated(
            RoutesPreviewUpdate("", mockk { every { routesList } returns emptyList() })
        )

        verify(exactly = 1) { DecodeUtils.clearCache() }
    }

    @Test
    fun routesPreviewUpdated_nonEmptyRoutesAndNoActiveRoutes() {
        sut.routesPreviewUpdated(
            RoutesPreviewUpdate("", RoutesPreview(listOf(mockk()), emptyList(), listOf(mockk()), 0))
        )

        verify(exactly = 0) { DecodeUtils.clearCache() }
    }

    @Test
    fun routesPreviewUpdated_nullPreviewAndHasActiveRoutes() {
        sut.onRoutesChanged(createRoutesUpdatedResult(createNavigationRoutes(), ""))
        clearStaticMockk(DecodeUtils::class)

        sut.routesPreviewUpdated(RoutesPreviewUpdate("", null))

        verify(exactly = 0) { DecodeUtils.clearCache() }
    }

    @Test
    fun routesPreviewUpdated_emptyRoutesAndHasActiveRoutes() {
        sut.onRoutesChanged(createRoutesUpdatedResult(createNavigationRoutes(), RoutesExtra.ROUTES_UPDATE_REASON_NEW))
        clearStaticMockk(DecodeUtils::class)

        sut.routesPreviewUpdated(
            RoutesPreviewUpdate("", mockk { every { routesList } returns emptyList() })
        )

        verify(exactly = 0) { DecodeUtils.clearCache() }
    }

    @Test
    fun routesPreviewUpdated_nullPreviewAndClearedActiveRoutes() {
        sut.onRoutesChanged(createRoutesUpdatedResult(createNavigationRoutes(), ""))
        sut.onRoutesChanged(createRoutesUpdatedResult(emptyList(), ""))
        clearStaticMockk(DecodeUtils::class)

        sut.routesPreviewUpdated(RoutesPreviewUpdate("", null))

        verify(exactly = 1) { DecodeUtils.clearCache() }
    }

    @Test
    fun routesPreviewUpdated_emptyRoutesAndClearedActiveRoutes() {
        sut.onRoutesChanged(createRoutesUpdatedResult(createNavigationRoutes(), ""))
        sut.onRoutesChanged(createRoutesUpdatedResult(emptyList(), ""))
        clearStaticMockk(DecodeUtils::class)

        sut.routesPreviewUpdated(
            RoutesPreviewUpdate("", mockk { every { routesList } returns emptyList() })
        )

        verify(exactly = 1) { DecodeUtils.clearCache() }
    }
}
