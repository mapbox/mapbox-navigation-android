package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteRefreshStateHolderTest {

    private val observer = mockk<RouteRefreshStatesObserver>(relaxed = true)
    private val sut = RouteRefreshStateHolder()

    @Before
    fun setUp() {
        sut.registerRouteRefreshStateObserver(observer)
    }

    @Test
    fun `null to started`() {
        sut.onStarted()

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_STARTED, null),
            )
        }
    }

    @Test
    fun `failed to started`() {
        sut.onFailure(null)
        clearAllMocks(answers = false)

        sut.onStarted()

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(
                    RouteRefreshExtra.REFRESH_STATE_STARTED,
                    null,
                ),
            )
        }
    }

    @Test
    fun `started to started`() {
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.onStarted()

        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `null to success`() {
        sut.onSuccess()

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    null,
                ),
            )
        }
    }

    @Test
    fun `failed to success`() {
        sut.onFailure(null)
        clearAllMocks(answers = false)

        sut.onSuccess()

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    null,
                ),
            )
        }
    }

    @Test
    fun `success to success`() {
        sut.onSuccess()
        clearAllMocks(answers = false)

        sut.onSuccess()

        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `null to failed`() {
        val message = "some message"
        sut.onFailure(message)

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    message,
                ),
            )
        }
    }

    @Test
    fun `started to failed `() {
        val message = "some message"
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.onFailure(message)

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED, message),
            )
        }
    }

    @Test
    fun `failed to failed`() {
        val message = "come message"
        sut.onFailure(message)
        clearAllMocks(answers = false)

        sut.onFailure(message)

        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `null to cleared_expired`() {
        sut.onClearedExpired()

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_CLEARED_EXPIRED),
            )
        }
    }

    @Test
    fun `started to cleared_expired`() {
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.onClearedExpired()

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_CLEARED_EXPIRED),
            )
        }
    }

    @Test
    fun `cleared_expired to cleared_expired`() {
        sut.onClearedExpired()
        clearAllMocks(answers = false)

        sut.onClearedExpired()

        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `null to cancelled`() {
        sut.onCancel()

        verify(exactly = 0) {
            observer.onNewState(any())
        }
    }

    @Test
    fun `started to cancelled can change`() {
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.onCancel()

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_CANCELED, null),
            )
        }
    }

    @Test
    fun `success to cancelled cannot change`() {
        sut.onStarted()
        sut.onSuccess()
        clearAllMocks(answers = false)

        sut.onCancel()

        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `cancelled to cancelled`() {
        sut.onCancel()
        clearAllMocks(answers = false)

        sut.onCancel()

        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `null to null`() {
        sut.reset()

        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `started to null`() {
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.reset()

        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun observersNotification() {
        val secondObserver = mockk<RouteRefreshStatesObserver>(relaxed = true)
        sut.onStarted()

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_STARTED, null),
            )
        }
        clearAllMocks(answers = false)

        sut.registerRouteRefreshStateObserver(secondObserver)

        sut.onCancel()

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_CANCELED, null),
            )
            secondObserver.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_CANCELED, null),
            )
        }
        clearAllMocks(answers = false)

        sut.unregisterRouteRefreshStateObserver(observer)

        sut.onFailure(null)

        verify(exactly = 1) {
            secondObserver.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED, null),
            )
        }
        verify(exactly = 0) { observer.onNewState(any()) }
        clearAllMocks(answers = false)

        sut.unregisterAllRouteRefreshStateObservers()

        sut.onStarted()

        verify(exactly = 0) {
            observer.onNewState(any())
            secondObserver.onNewState(any())
        }
    }
}
