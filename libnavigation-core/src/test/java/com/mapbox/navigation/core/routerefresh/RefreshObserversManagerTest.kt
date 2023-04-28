package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.core.RoutesRefreshData
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class RefreshObserversManagerTest {

    private val sut = RefreshObserversManager()
    private val observer = mockk<RouteRefreshObserver>(relaxed = true)
    private val routesRefreshData = mockk<RoutesRefreshData>()
    private val inputResult = RouteRefresherResult(true, routesRefreshData)
    private val outputResult = routesRefreshData

    @Test
    fun registerObserverThenReceiveUpdate() {
        sut.registerObserver(observer)

        sut.onRoutesRefreshed(inputResult)

        verify(exactly = 1) { observer.onRoutesRefreshed(outputResult) }
    }

    @Test
    fun registerUnregisteredObserver() {
        sut.registerObserver(observer)
        sut.unregisterObserver(observer)
        sut.registerObserver(observer)

        sut.onRoutesRefreshed(inputResult)

        verify(exactly = 1) { observer.onRoutesRefreshed(outputResult) }
    }

    @Test
    fun receiveUpdateThenRegisterObserver() {
        sut.onRoutesRefreshed(inputResult)

        sut.registerObserver(observer)

        verify(exactly = 0) { observer.onRoutesRefreshed(any()) }
    }

    @Test
    fun receiveUpdateAfterUnregisterObserver() {
        sut.registerObserver(observer)
        sut.unregisterObserver(observer)

        sut.onRoutesRefreshed(inputResult)

        verify(exactly = 0) { observer.onRoutesRefreshed(any()) }
    }

    @Test
    fun receiveUpdateAfterUnregisterAllObservers() {
        val observer2 = mockk<RouteRefreshObserver>(relaxed = true)
        sut.registerObserver(observer)
        sut.registerObserver(observer2)
        sut.unregisterAllObservers()

        sut.onRoutesRefreshed(inputResult)

        verify(exactly = 0) {
            observer.onRoutesRefreshed(any())
            observer2.onRoutesRefreshed(any())
        }
    }

    @Test
    fun receiveUpdateToNotifyMultipleObservers() {
        val observer2 = mockk<RouteRefreshObserver>(relaxed = true)
        sut.registerObserver(observer)
        sut.registerObserver(observer2)

        sut.onRoutesRefreshed(inputResult)

        verify(exactly = 1) {
            observer.onRoutesRefreshed(outputResult)
            observer2.onRoutesRefreshed(outputResult)
        }
    }

    @Test
    fun receiveUpdateWithOneRegisteredAndOneUnregisteredObserver() {
        val observer2 = mockk<RouteRefreshObserver>(relaxed = true)
        sut.registerObserver(observer)
        sut.registerObserver(observer2)
        sut.unregisterObserver(observer2)

        sut.onRoutesRefreshed(inputResult)

        verify(exactly = 1) {
            observer.onRoutesRefreshed(outputResult)
        }
        verify(exactly = 0) {
            observer2.onRoutesRefreshed(any())
        }
    }

    @Test
    fun receiveMultipleUpdates() {
        val routesProgressData2 = mockk<RoutesRefreshData>()
        val inputResult2 = RouteRefresherResult(
            true,
            routesProgressData2
        )

        sut.registerObserver(observer)
        sut.onRoutesRefreshed(inputResult)
        clearAllMocks(answers = false)

        sut.onRoutesRefreshed(inputResult2)

        verify { observer.onRoutesRefreshed(routesProgressData2) }
    }

    @Test
    fun unregisterUnknownObserver() {
        sut.unregisterObserver(observer)
    }

    @Test
    fun unregisterAllObserversWhenNoneRegistered() {
        sut.unregisterAllObservers()
    }
}
