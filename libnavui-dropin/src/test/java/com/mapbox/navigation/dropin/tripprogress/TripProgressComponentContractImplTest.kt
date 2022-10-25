package com.mapbox.navigation.dropin.tripprogress

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class TripProgressComponentContractImplTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var sut: TripProgressComponentContractImpl

    @Before
    fun setUp() {
        store = TestStore()
        sut = TripProgressComponentContractImpl(coroutineRule.coroutineScope, store)
    }

    @Test
    fun `previewRoutes should return preview routes from the store`() =
        coroutineRule.runBlockingTest {
            val routes = listOf<NavigationRoute>(mockk())
            store.updateState { it.copy(previewRoutes = RoutePreviewState.Ready(routes)) }
            coroutineRule.testDispatcher.advanceUntilIdle()

            assertEquals(routes, sut.previewRoutes.first())
        }

    @Test
    fun `previewRoutes returns emptyList`() = coroutineRule.runBlockingTest {
        assertTrue(sut.previewRoutes.first().isEmpty())
    }
}
