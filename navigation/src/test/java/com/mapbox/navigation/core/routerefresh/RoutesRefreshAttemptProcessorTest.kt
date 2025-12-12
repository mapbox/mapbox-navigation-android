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
                mockk { every { status } returns RouteRefresherStatus.Success(mockk()) },
                emptyList(),
            ),
        )

        verify(exactly = 0) { observersManager.onRoutesInvalidated(any()) }
    }

    @Test
    fun onRoutesInvalidated_singlePrimaryInvalidatedRoute() {
        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.Invalidated),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.Failure,
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        mockk(),
                        RouteRefresherStatus.Success(mockk()),
                    ),
                    RouteRefresherResult(
                        alternativeRoute3,
                        mockk(),
                        RouteRefresherStatus.Invalid,
                    ),
                ),
            ),
        )

        verify(exactly = 1) {
            observersManager.onRoutesInvalidated(RoutesInvalidatedParams(listOf(primaryRoute)))
        }
    }

    @Test
    fun onRoutesInvalidated_singleAlternativeInvalidatedRoute() {
        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.Success(mockk())),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.Failure,
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                    RouteRefresherResult(
                        alternativeRoute3,
                        mockk(),
                        RouteRefresherStatus.Invalid,
                    ),
                ),
            ),
        )

        verify(exactly = 1) {
            observersManager.onRoutesInvalidated(RoutesInvalidatedParams(listOf(alternativeRoute2)))
        }
    }

    @Test
    fun onRoutesInvalidated_multipleInvalidatedRoutes() {
        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.Invalidated),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                    RouteRefresherResult(
                        alternativeRoute3,
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                ),
            ),
        )

        verify(exactly = 1) {
            observersManager.onRoutesInvalidated(
                RoutesInvalidatedParams(
                    listOf(primaryRoute, alternativeRoute1, alternativeRoute2, alternativeRoute3),
                ),
            )
        }
    }

    @Test
    fun onRoutesInvalidated_allInvalidatedRoutesAlreadyProcessed() {
        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.Invalidated),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                ),
            ),
        )
        clearAllMocks(answers = false)

        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.Invalidated),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                    RouteRefresherResult(
                        mockk { every { id } returns "id#2" },
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                    RouteRefresherResult(
                        alternativeRoute3,
                        mockk(),
                        RouteRefresherStatus.Success(mockk()),
                    ),
                ),
            ),
        )

        verify(exactly = 0) { observersManager.onRoutesInvalidated(any()) }
    }

    @Test
    fun onRoutesInvalidated_someInvalidatedRoutesAlreadyProcessed() {
        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.Invalidated),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        mockk(),
                        RouteRefresherStatus.Success(mockk()),
                    ),
                ),
            ),
        )
        clearAllMocks(answers = false)

        processor.onRoutesRefreshAttemptFinished(
            RoutesRefresherResult(
                RouteRefresherResult(primaryRoute, mockk(), RouteRefresherStatus.Invalidated),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                    RouteRefresherResult(
                        alternativeRoute3,
                        mockk(),
                        RouteRefresherStatus.Invalidated,
                    ),
                ),
            ),
        )

        verify(exactly = 1) {
            observersManager.onRoutesInvalidated(
                RoutesInvalidatedParams(
                    listOf(alternativeRoute2, alternativeRoute3),
                ),
            )
        }
    }
}
