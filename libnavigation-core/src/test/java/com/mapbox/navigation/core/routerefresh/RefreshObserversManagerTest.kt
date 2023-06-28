package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.core.RoutesInvalidatedObserver
import com.mapbox.navigation.core.RoutesInvalidatedParams
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class RefreshObserversManagerTest {

    private val sut = RefreshObserversManager()
    private val observer = mockk<RouteRefreshObserver>(relaxed = true)
    private val invalidatedObserver = mockk<RoutesInvalidatedObserver>(relaxed = true)
    private val result = RoutesRefresherResult(mockk(), listOf(mockk()))
    private val invalidatedRoutesParams = mockk<RoutesInvalidatedParams>()

    @Test
    fun registerObserverThenReceiveUpdate() {
        sut.registerRefreshObserver(observer)

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { observer.onRoutesRefreshed(result) }
    }

    @Test
    fun registerUnregisteredObserver() {
        sut.registerRefreshObserver(observer)
        sut.unregisterRefreshObserver(observer)
        sut.registerRefreshObserver(observer)

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { observer.onRoutesRefreshed(result) }
    }

    @Test
    fun receiveUpdateThenRegisterObserver() {
        sut.onRoutesRefreshed(result)

        sut.registerRefreshObserver(observer)

        verify(exactly = 0) { observer.onRoutesRefreshed(any()) }
    }

    @Test
    fun receiveUpdateAfterUnregisterObserver() {
        sut.registerRefreshObserver(observer)
        sut.unregisterRefreshObserver(observer)

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { observer.onRoutesRefreshed(any()) }
    }

    @Test
    fun receiveUpdateAfterUnregisterAllObservers() {
        val observer2 = mockk<RouteRefreshObserver>(relaxed = true)
        sut.registerRefreshObserver(observer)
        sut.registerRefreshObserver(observer2)
        sut.unregisterAllObservers()

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) {
            observer.onRoutesRefreshed(any())
            observer2.onRoutesRefreshed(any())
        }
    }

    @Test
    fun receiveUpdateToNotifyMultipleObservers() {
        val observer2 = mockk<RouteRefreshObserver>(relaxed = true)
        sut.registerRefreshObserver(observer)
        sut.registerRefreshObserver(observer2)

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) {
            observer.onRoutesRefreshed(result)
            observer2.onRoutesRefreshed(result)
        }
    }

    @Test
    fun receiveUpdateWithOneRegisteredAndOneUnregisteredObserver() {
        val observer2 = mockk<RouteRefreshObserver>(relaxed = true)
        sut.registerRefreshObserver(observer)
        sut.registerRefreshObserver(observer2)
        sut.unregisterRefreshObserver(observer2)

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) {
            observer.onRoutesRefreshed(result)
        }
        verify(exactly = 0) {
            observer2.onRoutesRefreshed(any())
        }
    }

    @Test
    fun receiveMultipleUpdates() {
        val result2 = RoutesRefresherResult(
            mockk(),
            listOf(mockk(), mockk())
        )

        sut.registerRefreshObserver(observer)
        sut.onRoutesRefreshed(result)
        clearAllMocks(answers = false)

        sut.onRoutesRefreshed(result2)

        verify { observer.onRoutesRefreshed(result2) }
    }

    @Test
    fun unregisterUnknownObserver() {
        sut.unregisterRefreshObserver(observer)
    }

    @Test
    fun unregisterAllObserversWhenNoneRegistered() {
        sut.unregisterAllObservers()
    }

    @Test
    fun registerInvalidatedObserverThenReceiveUpdate() {
        sut.registerInvalidatedObserver(invalidatedObserver)

        sut.onRoutesInvalidated(invalidatedRoutesParams)

        verify(exactly = 1) { invalidatedObserver.onRoutesInvalidated(invalidatedRoutesParams) }
    }

    @Test
    fun registerUnregisteredInvalidatedObserver() {
        sut.registerInvalidatedObserver(invalidatedObserver)
        sut.unregisterInvalidatedObserver(invalidatedObserver)
        sut.registerInvalidatedObserver(invalidatedObserver)

        sut.onRoutesInvalidated(invalidatedRoutesParams)

        verify(exactly = 1) { invalidatedObserver.onRoutesInvalidated(invalidatedRoutesParams) }
    }

    @Test
    fun receiveUpdateThenRegisterInvalidatedObserver() {
        sut.onRoutesInvalidated(invalidatedRoutesParams)

        sut.registerInvalidatedObserver(invalidatedObserver)

        verify(exactly = 0) { invalidatedObserver.onRoutesInvalidated(invalidatedRoutesParams) }
    }

    @Test
    fun receiveUpdateAfterUnregisterInvalidatedObserver() {
        sut.registerInvalidatedObserver(invalidatedObserver)
        sut.unregisterInvalidatedObserver(invalidatedObserver)

        sut.onRoutesInvalidated(invalidatedRoutesParams)

        verify(exactly = 0) { invalidatedObserver.onRoutesInvalidated(any()) }
    }

    @Test
    fun receiveInvalidatedUpdateAfterUnregisterAllObservers() {
        val observer2 = mockk<RoutesInvalidatedObserver>(relaxed = true)
        sut.registerInvalidatedObserver(invalidatedObserver)
        sut.registerInvalidatedObserver(observer2)
        sut.unregisterAllObservers()

        sut.onRoutesInvalidated(invalidatedRoutesParams)

        verify(exactly = 0) {
            invalidatedObserver.onRoutesInvalidated(any())
            observer2.onRoutesInvalidated(any())
        }
    }

    @Test
    fun receiveUpdateToNotifyMultipleInvalidatedObservers() {
        val observer2 = mockk<RoutesInvalidatedObserver>(relaxed = true)
        sut.registerInvalidatedObserver(invalidatedObserver)
        sut.registerInvalidatedObserver(observer2)

        sut.onRoutesInvalidated(invalidatedRoutesParams)

        verify(exactly = 1) {
            invalidatedObserver.onRoutesInvalidated(invalidatedRoutesParams)
            observer2.onRoutesInvalidated(invalidatedRoutesParams)
        }
    }

    @Test
    fun receiveUpdateWithOneRegisteredAndOneUnregisteredInvalidatedObserver() {
        val observer2 = mockk<RoutesInvalidatedObserver>(relaxed = true)
        sut.registerInvalidatedObserver(invalidatedObserver)
        sut.registerInvalidatedObserver(observer2)
        sut.unregisterInvalidatedObserver(observer2)

        sut.onRoutesInvalidated(invalidatedRoutesParams)

        verify(exactly = 1) {
            invalidatedObserver.onRoutesInvalidated(invalidatedRoutesParams)
        }
        verify(exactly = 0) {
            observer2.onRoutesInvalidated(any())
        }
    }

    @Test
    fun receiveMultipleInvalidatedUpdates() {
        val invalidatedRoutesParams2 = mockk<RoutesInvalidatedParams>()

        sut.registerInvalidatedObserver(invalidatedObserver)
        sut.onRoutesInvalidated(invalidatedRoutesParams)
        clearAllMocks(answers = false)

        sut.onRoutesInvalidated(invalidatedRoutesParams2)

        verify { invalidatedObserver.onRoutesInvalidated(invalidatedRoutesParams2) }
    }

    @Test
    fun unregisterUnknownInvalidatedObserver() {
        sut.unregisterInvalidatedObserver(invalidatedObserver)
    }

    @Test
    fun refreshUpdateDoesNotAffectInvalidatedObserver() {
        sut.registerInvalidatedObserver(invalidatedObserver)

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) {
            invalidatedObserver.onRoutesInvalidated(any())
        }
    }

    @Test
    fun invalidatedUpdateDoesNotAffectRefreshObserver() {
        sut.registerRefreshObserver(observer)

        sut.onRoutesInvalidated(invalidatedRoutesParams)

        verify(exactly = 0) {
            observer.onRoutesRefreshed(any())
        }
    }
}
