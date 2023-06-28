package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.RoutesInvalidatedParams
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class RoutesRefreshAttemptProcessorTest {

    private val primaryRoute = mockk<NavigationRoute> {
        every { id } returns "id#0"
    }
    private val alternativeRoute1 = mockk<NavigationRoute> {
        every { id } returns "id#1"
    }
    private val alternativeRoute2 = mockk<NavigationRoute> {
        every { id } returns "id#2"
    }
    private val alternativeRoute3 = mockk<NavigationRoute> {
        every { id } returns "id#3"
    }
    private val observersManager = mockk<RefreshObserversManager>(relaxed = true)
    private val processor = RoutesRefreshAttemptProcessor(observersManager)

    @Test
    fun onRoutesInvalidated_nonInvalidatedRoutes() {
        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                mockk { every { status } returns RouteRefresherStatus.SUCCESS },
                emptyList(),
            )
        )

        verify(exactly = 0) { observersManager.onRoutesInvalidated(any()) }
    }

    @Test
    fun onRoutesInvalidated_singlePrimaryInvalidatedRoute() {
        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.INVALIDATED),
                listOf(
                    RouteRefresherResult(alternativeRoute1, mockk(), RouteRefresherStatus.FAILURE),
                    RouteRefresherResult(alternativeRoute2, mockk(), RouteRefresherStatus.SUCCESS),
                    RouteRefresherResult(alternativeRoute3, mockk(), RouteRefresherStatus.INVALID),
                )
            )
        )

        verify(exactly = 1) {
            observersManager.onRoutesInvalidated(RoutesInvalidatedParams(listOf(primaryRoute)))
        }
    }

    @Test
    fun onRoutesInvalidated_singleAlternativeInvalidatedRoute() {
        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.SUCCESS),
                listOf(
                    RouteRefresherResult(alternativeRoute1, mockk(), RouteRefresherStatus.FAILURE),
                    RouteRefresherResult(
                        alternativeRoute2,
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                    RouteRefresherResult(alternativeRoute3, mockk(), RouteRefresherStatus.INVALID),
                )
            )
        )

        verify(exactly = 1) {
            observersManager.onRoutesInvalidated(RoutesInvalidatedParams(listOf(alternativeRoute2)))
        }
    }

    @Test
    fun onRoutesInvalidated_multipleInvalidatedRoutes() {
        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.INVALIDATED),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                    RouteRefresherResult(
                        alternativeRoute3,
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                )
            )
        )

        verify(exactly = 1) {
            observersManager.onRoutesInvalidated(
                RoutesInvalidatedParams(
                    listOf(primaryRoute, alternativeRoute1, alternativeRoute2, alternativeRoute3)
                )
            )
        }
    }

    @Test
    fun onRoutesInvalidated_allInvalidatedRoutesAlreadyProcessed() {
        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.INVALIDATED),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                )
            )
        )
        clearAllMocks(answers = false)

        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.INVALIDATED),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                    RouteRefresherResult(
                        mockk { every { id } returns "id#2" },
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                    RouteRefresherResult(alternativeRoute3, mockk(), RouteRefresherStatus.SUCCESS),
                )
            )
        )

        verify(exactly = 0) { observersManager.onRoutesInvalidated(any()) }
    }

    @Test
    fun onRoutesInvalidated_someInvalidatedRoutesAlreadyProcessed() {
        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.INVALIDATED),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        mockk(),
                        RouteRefresherStatus.SUCCESS
                    ),
                )
            )
        )
        clearAllMocks(answers = false)

        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.INVALIDATED),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                    RouteRefresherResult(
                        alternativeRoute3,
                        mockk(),
                        RouteRefresherStatus.INVALIDATED
                    ),
                )
            )
        )

        verify(exactly = 1) {
            observersManager.onRoutesInvalidated(
                RoutesInvalidatedParams(
                    listOf(alternativeRoute2, alternativeRoute3)
                )
            )
        }
    }
}
