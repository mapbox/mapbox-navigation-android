package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteRefreshStateHolderTest {

    private val observer = mockk<RouteRefreshStatesObserver>(relaxed = true)
    private val sut = RouteRefreshStateHolder()

    @Before
    fun setUp() {
        sut.registerRouteRefreshStateObserver(observer)
        mockkObject(RouteRefreshStateChanger)
        every { RouteRefreshStateChanger.canChange(any(), any()) } returns true
    }

    @After
    fun tearDown() {
        unmockkObject(RouteRefreshStateChanger)
    }

    @Test
    fun `null to started`() {
        sut.onStarted()

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(null, RouteRefreshExtra.REFRESH_STATE_STARTED)
        }
        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_STARTED, null)
            )
        }
    }

    @Test
    fun `failed to started can change`() {
        sut.onFailure(null)
        clearAllMocks(answers = false)

        sut.onStarted()

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                RouteRefreshExtra.REFRESH_STATE_STARTED
            )
        }
        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(
                    RouteRefreshExtra.REFRESH_STATE_STARTED,
                    null
                )
            )
        }
    }

    @Test
    fun `failed to started cannot change`() {
        every {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                RouteRefreshExtra.REFRESH_STATE_STARTED
            )
        } returns false
        sut.onFailure(null)
        clearAllMocks(answers = false)

        sut.onStarted()

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                RouteRefreshExtra.REFRESH_STATE_STARTED
            )
        }
        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `started to started`() {
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.onStarted()

        verify(exactly = 0) { RouteRefreshStateChanger.canChange(any(), any()) }
        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `null to success`() {
        sut.onSuccess()

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(
                null,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            )
        }
        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    null
                )
            )
        }
    }

    @Test
    fun `failed to success can change`() {
        sut.onFailure(null)
        clearAllMocks(answers = false)

        sut.onSuccess()

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            )
        }
        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    null
                )
            )
        }
    }

    @Test
    fun `failed to success cannot change`() {
        every {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            )
        } returns false
        sut.onFailure(null)
        clearAllMocks(answers = false)

        sut.onSuccess()

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            )
        }
        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `success to success`() {
        sut.onSuccess()
        clearAllMocks(answers = false)

        sut.onSuccess()

        verify(exactly = 0) { RouteRefreshStateChanger.canChange(any(), any()) }
        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `null to failed`() {
        val message = "some message"
        sut.onFailure(message)

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(
                null,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED
            )
        }
        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    message
                )
            )
        }
    }

    @Test
    fun `started to failed can change`() {
        val message = "some message"
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.onFailure(message)

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED
            )
        }
        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED, message)
            )
        }
    }

    @Test
    fun `started to failed cannot change`() {
        val message = "some message"
        every {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED
            )
        } returns false
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.onFailure(message)

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED
            )
        }
        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `failed to failed`() {
        val message = "come message"
        sut.onFailure(message)
        clearAllMocks(answers = false)

        sut.onFailure(message)

        verify(exactly = 0) { RouteRefreshStateChanger.canChange(any(), any()) }
        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `null to cancelled`() {
        sut.onCancel()

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(null, RouteRefreshExtra.REFRESH_STATE_CANCELED)
        }
        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_CANCELED, null)
            )
        }
    }

    @Test
    fun `started to cancelled can change`() {
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.onCancel()

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED
            )
        }
        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_CANCELED, null)
            )
        }
    }

    @Test
    fun `started to cancelled cannot change`() {
        every {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED
            )
        } returns false
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.onCancel()

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED
            )
        }
        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `cancelled to cancelled`() {
        sut.onCancel()
        clearAllMocks(answers = false)

        sut.onCancel()

        verify(exactly = 0) { RouteRefreshStateChanger.canChange(any(), any()) }
        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `null to null`() {
        sut.reset()

        verify(exactly = 0) { RouteRefreshStateChanger.canChange(any(), any()) }
        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `started to null can change`() {
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.reset()

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(RouteRefreshExtra.REFRESH_STATE_STARTED, null)
        }
        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun `started to null cannot change`() {
        every {
            RouteRefreshStateChanger.canChange(RouteRefreshExtra.REFRESH_STATE_STARTED, null)
        } returns false
        sut.onStarted()
        clearAllMocks(answers = false)

        sut.reset()

        verify(exactly = 1) {
            RouteRefreshStateChanger.canChange(RouteRefreshExtra.REFRESH_STATE_STARTED, null)
        }
        verify(exactly = 0) { observer.onNewState(any()) }
    }

    @Test
    fun observersNotification() {
        val secondObserver = mockk<RouteRefreshStatesObserver>(relaxed = true)
        sut.onStarted()

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_STARTED, null)
            )
        }
        clearAllMocks(answers = false)

        sut.registerRouteRefreshStateObserver(secondObserver)

        sut.onCancel()

        verify(exactly = 1) {
            observer.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_CANCELED, null)
            )
            secondObserver.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_CANCELED, null)
            )
        }
        clearAllMocks(answers = false)

        sut.unregisterRouteRefreshStateObserver(observer)

        sut.onFailure(null)

        verify(exactly = 1) {
            secondObserver.onNewState(
                RouteRefreshStateResult(RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED, null)
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
