package com.mapbox.navigation.ui.androidauto.screenmanager

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.internal.context.MapboxCarContextOwner
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxScreenManagerTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.INITIALIZED)
    private val screenManager: ScreenManager = mockk(relaxed = true)
    private val carContext: CarContext = mockk {
        every { getCarService(ScreenManager::class.java) } returns screenManager
    }
    private val carContextOwner: MapboxCarContextOwner = mockk {
        every { carContext() } returns carContext
        every { lifecycle } returns lifecycleOwner.lifecycle
    }
    private val screenEvent = MutableSharedFlow<MapboxScreenEvent>(
        replay = MapboxScreenManager.REPLAY_CACHE,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )

    private val mapboxScreenManager = MapboxScreenManager(carContextOwner)

    @Before
    fun setup() {
        mockkObject(MapboxScreenManager)
        mockkStatic(MapboxScreenManager::class)
        every { MapboxScreenManager.screenKeyMutable } returns screenEvent
        every { MapboxScreenManager.screenEvent } returns screenEvent
        every { MapboxScreenManager.replaceTop(any()) } answers {
            screenEvent.tryEmit(MapboxScreenEvent(firstArg(), MapboxScreenOperation.REPLACE_TOP))
        }
        every { MapboxScreenManager.push(any()) } answers {
            screenEvent.tryEmit(MapboxScreenEvent(firstArg(), MapboxScreenOperation.PUSH))
        }
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `screen manager is available when the lifecycle is created`() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        assertNotNull(mapboxScreenManager.requireScreenManager())
    }

    @Test
    fun `createScreen will return a Screen from the correct MapboxScreenFactory`() {
        val screenA: Screen = mockk()
        val screenB: Screen = mockk()
        every { screenManager.stackSize } returns 0

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        val result = mapboxScreenManager.createScreen("SCREEN_B")

        assertEquals(screenB, result)
    }

    @Test
    fun `createScreen will add a screen to the backstack`() {
        val screenA: Screen = mockk()
        val screenB: Screen = mockk()
        every { screenManager.stackSize } returns 0

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        mapboxScreenManager.createScreen("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")
        every { screenManager.stackSize } returns 2
        every { screenManager.top } returnsMany listOf(screenB, screenA)
        val result = mapboxScreenManager.goBack()

        assertTrue(result)
        verifyOrder {
            screenManager.push(screenA)
            screenManager.push(screenB)
            screenManager.pop()
        }
    }

    @Test
    fun `createScreen push on top of an existing backstack`() {
        val screenA: Screen = mockk(relaxed = true)
        every { screenManager.stackSize } returns 1
        every { screenManager.top } returnsMany listOf(screenA)

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { mockk() },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val screenB = mapboxScreenManager.createScreen("SCREEN_B")

        verify(exactly = 0) { screenManager.push(screenA) }
        verifyOrder {
            screenManager.push(screenB)
        }
    }

    @Test
    fun `createScreen will not re-create a screen if it is already on top`() {
        val screenA: Screen = mockk(relaxed = true)
        val screenB: Screen = mockk(relaxed = true)
        every { screenManager.stackSize } returns 2
        every { screenManager.top } returns screenB
        val screenAFactory = mockk<MapboxScreenFactory> {
            every { create(any()) } returns screenA
        }
        val screenBFactory = mockk<MapboxScreenFactory> {
            every { create(any()) } returns screenB
        }

        mapboxScreenManager.putAll(
            "SCREEN_A" to screenAFactory,
            "SCREEN_B" to screenBFactory,
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")
        val returnedScreen = mapboxScreenManager.createScreen("SCREEN_B")

        assertEquals(screenB, returnedScreen)
        verifyOrder {
            screenAFactory.create(any())
            screenManager.push(screenA)
            screenBFactory.create(any())
            screenManager.push(screenB)
        }
    }

    @Test
    fun `createScreen will push on top of an existing screen stack`() {
        val screenA: Screen = mockk(relaxed = true)
        val screenB: Screen = mockk(relaxed = true)
        every { screenManager.stackSize } returns 1
        every { screenManager.top } returns screenA
        val screenAFactory = mockk<MapboxScreenFactory> {
            every { create(any()) } returns screenA
        }
        val screenBFactory = mockk<MapboxScreenFactory> {
            every { create(any()) } returns screenB
        }

        mapboxScreenManager.putAll(
            "SCREEN_A" to screenAFactory,
            "SCREEN_B" to screenBFactory,
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        val returnedScreen = mapboxScreenManager.createScreen("SCREEN_B")

        assertEquals(screenB, returnedScreen)
        assertEquals(2, mapboxScreenManager.screenStack.size)
        verifyOrder {
            screenAFactory.create(any())
            screenManager.push(screenA)
            screenBFactory.create(any())
        }
    }

    @Test
    fun `replaceTop will popToRoot push and finish`() {
        val screenA: Screen = mockk(relaxed = true)
        val screenB: Screen = mockk()
        every { screenManager.stackSize } returns 1
        every { screenManager.top } returns screenA

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.replaceTop("SCREEN_B")

        verifyOrder {
            screenManager.popToRoot()
            screenManager.top
            screenManager.push(screenB)
            screenA.finish()
        }
    }

    @Test
    fun `replaceTop will not call top when stack is empty`() {
        val screenA: Screen = mockk(relaxed = true)
        val screenB: Screen = mockk()
        every { screenManager.stackSize } returns 0
        every { screenManager.top } throws java.lang.NullPointerException()

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.replaceTop("SCREEN_B")

        verifyOrder {
            screenManager.push(screenB)
        }
    }

    @Test
    fun `replaceTop will not replace backstack if this screen is already on top`() {
        val screenA: Screen = mockk(relaxed = true)
        val screenB: Screen = mockk()

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")
        every { screenManager.stackSize } returns 2
        every { screenManager.top } returns screenB
        MapboxScreenManager.replaceTop("SCREEN_B")

        verify(exactly = 1) {
            screenManager.push(screenA)
            screenManager.push(screenB)
        }
        verify(exactly = 0) { screenManager.popToRoot() }
    }

    @Test
    fun `push will push to the screen manager`() =
        coroutineRule.runBlockingTest {
            val screenA: Screen = mockk()
            val screenB: Screen = mockk()
            every { screenManager.top } returns screenA

            mapboxScreenManager.putAll(
                "SCREEN_A" to MapboxScreenFactory { screenA },
                "SCREEN_B" to MapboxScreenFactory { screenB },
            )
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            MapboxScreenManager.push("SCREEN_B")

            verify(exactly = 1) {
                screenManager.push(screenB)
            }
            verify(exactly = 0) {
                screenManager.push(screenA)
                screenManager.popToRoot()
                screenManager.pop()
            }
        }

    @Test
    fun `push can be used to fill the backstack`() {
        val screenA: Screen = mockk()
        val screenB: Screen = mockk()

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")

        verifyOrder {
            screenManager.push(screenA)
            screenManager.push(screenB)
        }
        assertEquals(2, mapboxScreenManager.screenStack.size)
    }

    @Test
    fun `push will not be added to the backstack when the screen is already on top`() {
        val screenA: Screen = mockk()
        val screenB: Screen = mockk()

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")
        MapboxScreenManager.push("SCREEN_B")

        verify(exactly = 1) {
            screenManager.push(screenA)
            screenManager.push(screenB)
        }
        assertEquals(2, mapboxScreenManager.screenStack.size)
    }

    @Test
    fun `goBack will do nothing when the stack is empty`() {
        val screenA: Screen = mockk()
        val screenB: Screen = mockk()

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val result = mapboxScreenManager.goBack()

        assertFalse(result)
        verify { screenManager wasNot Called }
    }

    @Test
    fun `goBack will not pop if the stack has one screen`() {
        val screenA: Screen = mockk()
        val screenB: Screen = mockk()

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        every { screenManager.top } returnsMany listOf(screenA, screenA)
        every { screenManager.stackSize } returns 1
        val result = mapboxScreenManager.goBack()

        assertFalse(result)
        verify(exactly = 1) { screenManager.push(screenA) }
        verify(exactly = 0) { screenManager.pop() }
    }

    @Test
    fun `goBack will pop the last screen pushed`() {
        val screenA: Screen = mockk()
        val screenB: Screen = mockk()

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")
        every { screenManager.stackSize } returns 2
        every { screenManager.top } returnsMany listOf(screenB, screenA)
        val result = mapboxScreenManager.goBack()

        assertTrue(result)
        verifyOrder {
            screenManager.push(screenA)
            screenManager.push(screenB)
            screenManager.pop()
        }
    }

    @Test
    fun `goBack will return false when the top is not unrecognized`() {
        val screenA: Screen = mockk()
        val screenB: Screen = mockk()
        val screenUnrecognized: Screen = mockk()

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")
        every { screenManager.stackSize } returns 2
        every { screenManager.top } returnsMany listOf(screenUnrecognized, screenB)
        val result = mapboxScreenManager.goBack()

        assertFalse(result)
        verify(exactly = 0) { screenManager.pop() }
    }

    @Test
    fun `goBack will crash if the operation results in an unknown screen`() {
        val screenA: Screen = mockk()
        val screenB: Screen = mockk()
        val screenUnrecognized: Screen = mockk()

        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")
        every { screenManager.stackSize } returns 2
        every { screenManager.top } returnsMany listOf(screenB, screenUnrecognized)

        verify(exactly = 1) { screenManager.push(screenA) }
        verify(exactly = 0) { screenManager.pop() }
    }

    @Test
    fun `lifecycle changes will not trigger screen changes`() =
        coroutineRule.runBlockingTest {
            val screenA: Screen = mockk()
            val screenB: Screen = mockk()

            mapboxScreenManager.putAll(
                "SCREEN_A" to MapboxScreenFactory { screenA },
                "SCREEN_B" to MapboxScreenFactory { screenB },
            )
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            MapboxScreenManager.push("SCREEN_A")
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

            verify(exactly = 1) { screenManager.push(screenA) }
            verify(exactly = 0) { screenManager.push(screenB) }
        }

    @Test(expected = IllegalStateException::class)
    fun `create will crash if the MapboxScreenFactory does not exist`() {
        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { mockk() },
            "SCREEN_B" to MapboxScreenFactory { mockk() },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // This will crash
        mapboxScreenManager.createScreen("SCREEN_DOES_NOT_EXIST")
    }

    @Test(expected = IllegalStateException::class)
    fun `requireScreenManager will crash accessed after the lifecycle is destroyed`() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertNotNull(mapboxScreenManager.requireScreenManager())
    }
}
