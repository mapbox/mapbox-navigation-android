package com.mapbox.navigation.ui.androidauto.screenmanager

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.internal.AndroidAutoLog
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
    private val screenEvent = MapboxScreenManager.createScreenEventFlow()

    private val mapboxScreenManager = MapboxScreenManager(carContextOwner)

    @Before
    fun setup() {
        mockkObject(AndroidAutoLog)
        mockkObject(MapboxScreenManager)
        mockkStatic(MapboxScreenManager::class)
        every { MapboxScreenManager.screenKeyMutable } returns screenEvent
        every { MapboxScreenManager.screenEvent } returns screenEvent
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

    @Test
    fun `events emitted before the lifecycle is created are replayed to registered factories`() {
        val screenA: Screen = mockk(relaxed = true)
        val screenB: Screen = mockk(relaxed = true)
        every { screenManager.stackSize } returns 0
        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { screenB },
        )
        MapboxScreenManager.replaceTop("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")

        val uncaught = collectUncaughtExceptions {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        assertEquals(emptyList<Throwable>(), uncaught)
        verifyOrder {
            screenManager.push(screenA)
            screenManager.push(screenB)
        }
        assertEquals(
            listOf("SCREEN_B", "SCREEN_A"),
            mapboxScreenManager.screenStack.map { it.first },
        )

        // Session.onCreateScreen restores the replayed top without pushing it again.
        every { screenManager.stackSize } returns 2
        every { screenManager.top } returns screenB
        val restored = mapboxScreenManager.createScreen(MapboxScreenManager.current()!!.key)

        assertEquals(screenB, restored)
        verify(exactly = 1) { screenManager.push(screenB) }
    }

    @Test
    fun `constructing after the lifecycle is created ignores a replayed replaceTop for an unregistered key`() {
        val session = IsolatedSession()
        val screenA: Screen = mockk(relaxed = true)
        val screenB: Screen = mockk(relaxed = true)
        MapboxScreenManager.replaceTop("SCREEN_A")
        session.lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val uncaught = collectUncaughtExceptions {
            // Subscribes, and receives the replayed event, from inside the constructor.
            val lateScreenManager = MapboxScreenManager(session.carContextOwner)

            verify(exactly = 0) { session.screenManager.push(any()) }
            assertTrue(lateScreenManager.screenStack.isEmpty())

            lateScreenManager.putAll(
                "SCREEN_A" to MapboxScreenFactory { screenA },
                "SCREEN_B" to MapboxScreenFactory { screenB },
            )
            assertEquals(screenA, lateScreenManager.createScreen("SCREEN_A"))
            MapboxScreenManager.push("SCREEN_B")
        }

        assertEquals(emptyList<Throwable>(), uncaught)
        verifyOrder {
            session.screenManager.push(screenA)
            session.screenManager.push(screenB)
        }
    }

    @Test
    fun `constructing after the lifecycle is created ignores a replayed push for an unregistered key`() {
        val session = IsolatedSession()
        val screenA: Screen = mockk(relaxed = true)
        val screenB: Screen = mockk(relaxed = true)
        MapboxScreenManager.push("SCREEN_A")
        session.lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val uncaught = collectUncaughtExceptions {
            val lateScreenManager = MapboxScreenManager(session.carContextOwner)

            verify(exactly = 0) { session.screenManager.push(any()) }
            assertTrue(lateScreenManager.screenStack.isEmpty())

            lateScreenManager.putAll(
                "SCREEN_A" to MapboxScreenFactory { screenA },
                "SCREEN_B" to MapboxScreenFactory { screenB },
            )
            lateScreenManager.createScreen("SCREEN_A")
            MapboxScreenManager.push("SCREEN_B")
        }

        assertEquals(emptyList<Throwable>(), uncaught)
        verifyOrder {
            session.screenManager.push(screenA)
            session.screenManager.push(screenB)
        }
    }

    @Test
    fun `replaceTop to an unregistered key is ignored and later events are still handled`() {
        val screenA: Screen = mockk(relaxed = true)
        every { screenManager.stackSize } returns 0
        mapboxScreenManager.putAll("SCREEN_A" to MapboxScreenFactory { screenA })

        val uncaught = collectUncaughtExceptions {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            MapboxScreenManager.replaceTop("SCREEN_DOES_NOT_EXIST")

            verify(exactly = 0) { screenManager.push(any()) }
            assertTrue(mapboxScreenManager.screenStack.isEmpty())

            MapboxScreenManager.replaceTop("SCREEN_A")
        }

        assertEquals(emptyList<Throwable>(), uncaught)
        verify(exactly = 1) { screenManager.push(screenA) }
        assertEquals("SCREEN_A", mapboxScreenManager.screenStack.peek()?.first)
    }

    @Test
    fun `push to an unregistered key is ignored and later events are still handled`() {
        val screenA: Screen = mockk(relaxed = true)
        every { screenManager.stackSize } returns 0
        mapboxScreenManager.putAll("SCREEN_A" to MapboxScreenFactory { screenA })

        val uncaught = collectUncaughtExceptions {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            MapboxScreenManager.push("SCREEN_DOES_NOT_EXIST")

            verify(exactly = 0) { screenManager.push(any()) }
            assertTrue(mapboxScreenManager.screenStack.isEmpty())

            MapboxScreenManager.push("SCREEN_A")
        }

        assertEquals(emptyList<Throwable>(), uncaught)
        verify(exactly = 1) { screenManager.push(screenA) }
        assertEquals("SCREEN_A", mapboxScreenManager.screenStack.peek()?.first)
    }

    @Test
    fun `events emitted before the manager is created are not executed`() {
        val session = IsolatedSession()
        val screenA: Screen = mockk(relaxed = true)
        MapboxScreenManager.replaceTop("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")

        val uncaught = collectUncaughtExceptions {
            val newScreenManager = MapboxScreenManager(session.carContextOwner)
            newScreenManager.putAll(
                "SCREEN_A" to MapboxScreenFactory { screenA },
                "SCREEN_B" to MapboxScreenFactory { error("state of a previous session") },
            )
            session.lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

            verify(exactly = 0) { session.screenManager.push(any()) }
            assertTrue(newScreenManager.screenStack.isEmpty())
            assertEquals(
                MapboxScreenEvent("SCREEN_B", MapboxScreenOperation.PUSH),
                MapboxScreenManager.current(),
            )
            assertEquals(screenA, newScreenManager.createScreen("SCREEN_A"))
        }

        assertEquals(emptyList<Throwable>(), uncaught)
        verify(exactly = 1) { session.screenManager.push(screenA) }
        verify {
            AndroidAutoLog.logAndroidAuto(
                match { it.contains("REPLACE_TOP SCREEN_A ignored, it was emitted before") },
            )
            AndroidAutoLog.logAndroidAuto(
                match { it.contains("PUSH SCREEN_B ignored, it was emitted before") },
            )
        }
    }

    @Test
    fun `replaceTop with a factory that throws is ignored and later events are still handled`() {
        val screenA: Screen = mockk(relaxed = true)
        every { screenManager.stackSize } returns 0
        mapboxScreenManager.putAll(
            "BROKEN" to MapboxScreenFactory { error("missing state") },
            "SCREEN_A" to MapboxScreenFactory { screenA },
        )

        val uncaught = collectUncaughtExceptions {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            MapboxScreenManager.replaceTop("BROKEN")

            verify(exactly = 0) { screenManager.push(any()) }
            assertTrue(mapboxScreenManager.screenStack.isEmpty())

            MapboxScreenManager.replaceTop("SCREEN_A")
        }

        assertEquals(emptyList<Throwable>(), uncaught)
        verify(exactly = 1) { screenManager.push(screenA) }
        assertEquals(listOf("SCREEN_A"), mapboxScreenManager.screenStack.map { it.first })
    }

    @Test
    fun `push with a factory that throws is ignored and later events are still handled`() {
        val screenA: Screen = mockk(relaxed = true)
        mapboxScreenManager.putAll(
            "BROKEN" to MapboxScreenFactory { error("missing state") },
            "SCREEN_A" to MapboxScreenFactory { screenA },
        )

        val uncaught = collectUncaughtExceptions {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            MapboxScreenManager.push("BROKEN")

            verify(exactly = 0) { screenManager.push(any()) }
            assertTrue(mapboxScreenManager.screenStack.isEmpty())

            MapboxScreenManager.push("SCREEN_A")
        }

        assertEquals(emptyList<Throwable>(), uncaught)
        verify(exactly = 1) { screenManager.push(screenA) }
        assertEquals(listOf("SCREEN_A"), mapboxScreenManager.screenStack.map { it.first })
    }

    @Test
    fun `replaceTop keeps only the new screen in the back-stack`() {
        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { mockk(relaxed = true) },
            "SCREEN_B" to MapboxScreenFactory { mockk(relaxed = true) },
            "SCREEN_C" to MapboxScreenFactory { mockk(relaxed = true) },
            "SCREEN_D" to MapboxScreenFactory { mockk(relaxed = true) },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")
        MapboxScreenManager.replaceTop("SCREEN_C")

        assertEquals(listOf("SCREEN_C"), mapboxScreenManager.screenStack.map { it.first })
        assertFalse(mapboxScreenManager.isScreenBelowTop("SCREEN_A"))
        assertFalse(mapboxScreenManager.isScreenBelowTop("SCREEN_B"))

        MapboxScreenManager.push("SCREEN_D")

        assertEquals(
            listOf("SCREEN_D", "SCREEN_C"),
            mapboxScreenManager.screenStack.map { it.first },
        )
        assertTrue(mapboxScreenManager.isScreenBelowTop("SCREEN_C"))
    }

    @Test
    fun `createScreen on an empty ScreenManager drops stale back-stack entries`() {
        val screenA: Screen = mockk(relaxed = true)
        every { screenManager.stackSize } returns 0
        mapboxScreenManager.putAll("SCREEN_A" to MapboxScreenFactory { screenA })
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        mapboxScreenManager.screenStack.push("STALE" to mockk())

        mapboxScreenManager.createScreen("SCREEN_A")

        assertEquals(listOf("SCREEN_A"), mapboxScreenManager.screenStack.map { it.first })
    }

    @Test
    fun `destroying the lifecycle clears the back-stack`() {
        mapboxScreenManager.putAll("SCREEN_A" to MapboxScreenFactory { mockk(relaxed = true) })
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        assertEquals(1, mapboxScreenManager.screenStack.size)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertTrue(mapboxScreenManager.screenStack.isEmpty())
    }

    @Test
    fun `a burst of events emitted while an event is handled is not dropped`() {
        val keys = (1..10).map { "SCREEN_$it" }
        val screens = keys.associateWith { mockk<Screen>(relaxed = true) }
        val triggerScreen: Screen = mockk(relaxed = true)
        keys.forEach { key -> mapboxScreenManager[key] = MapboxScreenFactory { screens[key]!! } }
        // Emits from the main thread while the collector is busy with the current event.
        mapboxScreenManager["TRIGGER"] = MapboxScreenFactory {
            keys.forEach { MapboxScreenManager.push(it) }
            triggerScreen
        }
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        MapboxScreenManager.push("TRIGGER")

        verifyOrder {
            screenManager.push(triggerScreen)
            keys.forEach { screenManager.push(screens[it]!!) }
        }
        assertEquals(keys.size + 1, mapboxScreenManager.screenStack.size)
        verify(exactly = 0) {
            AndroidAutoLog.logAndroidAutoFailure(match { it.contains("dropped") }, any())
        }
    }

    @Test
    fun `an event that does not fit the buffer is logged`() {
        val triggerScreen: Screen = mockk(relaxed = true)
        val burstSize = MapboxScreenManager.REPLAY_CACHE +
            MapboxScreenManager.EXTRA_BUFFER_CAPACITY + 1
        mapboxScreenManager["TRIGGER"] = MapboxScreenFactory {
            repeat(burstSize) { MapboxScreenManager.push("SCREEN_$it") }
            triggerScreen
        }
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        MapboxScreenManager.push("TRIGGER")

        verify(atLeast = 1) {
            AndroidAutoLog.logAndroidAutoFailure(match { it.contains("dropped") }, any())
        }
    }

    @Test
    fun `recreateTop returns false when the top is another screen`() {
        val screenA: Screen = mockk(relaxed = true)
        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory { mockk(relaxed = true) },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        every { screenManager.stackSize } returns 1
        every { screenManager.top } returns screenA

        assertFalse(mapboxScreenManager.recreateTop("SCREEN_B"))

        verify(exactly = 1) { screenManager.push(any()) }
        assertEquals(listOf("SCREEN_A"), mapboxScreenManager.screenStack.map { it.first })
    }

    @Test
    fun `recreateTop replaces the top screen and keeps the screens below`() {
        val screenA: Screen = mockk(relaxed = true)
        val createdB = mutableListOf<Screen>()
        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "SCREEN_B" to MapboxScreenFactory {
                mockk<Screen>(relaxed = true).also { createdB += it }
            },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        MapboxScreenManager.push("SCREEN_B")
        val oldB = createdB.single()
        every { screenManager.stackSize } returns 2
        every { screenManager.top } returns oldB

        assertTrue(mapboxScreenManager.recreateTop("SCREEN_B"))

        val newB = createdB.last()
        verifyOrder {
            screenManager.push(newB)
            oldB.finish()
        }
        assertEquals(2, createdB.size)
        assertEquals(
            listOf("SCREEN_B" to newB, "SCREEN_A" to screenA),
            mapboxScreenManager.screenStack.toList(),
        )
        assertEquals(
            MapboxScreenEvent("SCREEN_B", MapboxScreenOperation.PUSH),
            MapboxScreenManager.current(),
        )
    }

    @Test
    fun `recreateTop replaces the root screen`() {
        val created = mutableListOf<Screen>()
        every { screenManager.stackSize } returns 0
        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory {
                mockk<Screen>(relaxed = true).also { created += it }
            },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val oldA = mapboxScreenManager.createScreen("SCREEN_A")
        every { screenManager.stackSize } returns 1
        every { screenManager.top } returns oldA

        assertTrue(mapboxScreenManager.recreateTop("SCREEN_A"))

        verify { oldA.finish() }
        assertEquals(listOf("SCREEN_A" to created.last()), mapboxScreenManager.screenStack.toList())
    }

    @Test
    fun `recreateTop leaves the back-stack unchanged when the factory throws`() {
        val screenA: Screen = mockk(relaxed = true)
        var fail = false
        mapboxScreenManager.putAll(
            "SCREEN_A" to MapboxScreenFactory {
                if (fail) error("missing state") else screenA
            },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        every { screenManager.stackSize } returns 1
        every { screenManager.top } returns screenA
        fail = true

        assertFalse(mapboxScreenManager.recreateTop("SCREEN_A"))

        verify(exactly = 0) { screenA.finish() }
        verify(exactly = 1) { screenManager.push(any()) }
        assertEquals(listOf("SCREEN_A" to screenA), mapboxScreenManager.screenStack.toList())
    }

    @Test
    fun `recreateTop returns false when the ScreenManager top is not the tracked screen`() {
        val screenA: Screen = mockk(relaxed = true)
        mapboxScreenManager.putAll("SCREEN_A" to MapboxScreenFactory { screenA })
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("SCREEN_A")
        every { screenManager.stackSize } returns 2
        every { screenManager.top } returns mockk()

        assertFalse(mapboxScreenManager.recreateTop("SCREEN_A"))

        verify(exactly = 1) { screenManager.push(any()) }
        verify(exactly = 0) { screenA.finish() }
    }

    @Test
    fun `a first screen created as the default map screen is tracked under its key`() {
        val mapScreen: Screen = mockk(relaxed = true)
        every { carContext.carAppApiLevel } returns 7
        every { screenManager.stackSize } returns 0
        mapboxScreenManager.putAll(
            MapboxScreen.NAVIGATION to MapboxScreenFactory { mapScreen },
            "NEEDS_STATE" to MapboxScreenFactory {
                mapboxScreenManager.createDefaultMapScreenInstead(it, "state is missing")
            },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        assertEquals(mapScreen, mapboxScreenManager.createScreen("NEEDS_STATE"))

        assertEquals(
            listOf(MapboxScreen.NAVIGATION to mapScreen),
            mapboxScreenManager.screenStack.toList(),
        )
        assertEquals(
            MapboxScreenEvent(MapboxScreen.NAVIGATION, MapboxScreenOperation.CREATED),
            MapboxScreenManager.current(),
        )
    }

    @Test
    fun `a first screen pushed as the default map screen is tracked under its key`() {
        val mapScreen: Screen = mockk(relaxed = true)
        every { carContext.carAppApiLevel } returns 7
        every { screenManager.stackSize } returns 0
        mapboxScreenManager.putAll(
            MapboxScreen.NAVIGATION to MapboxScreenFactory { mapScreen },
            "NEEDS_STATE" to MapboxScreenFactory {
                mapboxScreenManager.createDefaultMapScreenInstead(it, "state is missing")
            },
        )
        // Emitted by this session before it is created, so the event is executed on ON_CREATE.
        MapboxScreenManager.push("NEEDS_STATE")

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        verify(exactly = 1) { screenManager.push(mapScreen) }
        assertEquals(
            listOf(MapboxScreen.NAVIGATION to mapScreen),
            mapboxScreenManager.screenStack.toList(),
        )
    }

    @Test
    fun `createScreen returns the default map screen that is already shown in its place`() {
        val mapScreen: Screen = mockk(relaxed = true)
        every { carContext.carAppApiLevel } returns 7
        every { screenManager.stackSize } returns 0
        mapboxScreenManager.putAll(
            MapboxScreen.NAVIGATION to MapboxScreenFactory { mapScreen },
            "NEEDS_STATE" to MapboxScreenFactory {
                mapboxScreenManager.createDefaultMapScreenInstead(it, "state is missing")
            },
        )
        MapboxScreenManager.replaceTop("NEEDS_STATE")
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        every { screenManager.stackSize } returns 1
        every { screenManager.top } returns mapScreen

        assertEquals(mapScreen, mapboxScreenManager.createScreen("NEEDS_STATE"))

        verify(exactly = 1) { screenManager.push(mapScreen) }
        assertEquals(
            listOf(MapboxScreen.NAVIGATION to mapScreen),
            mapboxScreenManager.screenStack.toList(),
        )
    }

    @Test
    fun `a screen created while the default map screen is created keeps its own key`() {
        val mapScreen: Screen = mockk(relaxed = true)
        val screenB: Screen = mockk(relaxed = true)
        every { carContext.carAppApiLevel } returns 7
        every { screenManager.stackSize } returns 0
        mapboxScreenManager.putAll(
            MapboxScreen.NAVIGATION to MapboxScreenFactory { mapScreen },
            "SCREEN_B" to MapboxScreenFactory { screenB },
            "NEEDS_STATE" to MapboxScreenFactory {
                mapboxScreenManager.createDefaultMapScreenInstead(it, "state is missing").also {
                    MapboxScreenManager.push("SCREEN_B")
                }
            },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        mapboxScreenManager.createScreen("NEEDS_STATE")

        assertEquals(MapboxScreen.NAVIGATION to mapScreen, mapboxScreenManager.screenStack.peek())
        assertTrue(
            MapboxScreenEvent(MapboxScreen.NAVIGATION, MapboxScreenOperation.CREATED) in
                screenEvent.replayCache,
        )
    }

    @Test
    fun `a factory that throws after the default map screen is created does not affect later screens`() {
        val screenA: Screen = mockk(relaxed = true)
        every { carContext.carAppApiLevel } returns 7
        every { screenManager.stackSize } returns 0
        mapboxScreenManager.putAll(
            MapboxScreen.NAVIGATION to MapboxScreenFactory { mockk(relaxed = true) },
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "NEEDS_STATE" to MapboxScreenFactory {
                mapboxScreenManager.createDefaultMapScreenInstead(it, "state is missing")
                error("failed after the fallback")
            },
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        MapboxScreenManager.push("NEEDS_STATE")

        mapboxScreenManager.createScreen("SCREEN_A")

        assertEquals(listOf("SCREEN_A" to screenA), mapboxScreenManager.screenStack.toList())
    }

    @Test
    fun `a pushed screen that can only be created first is ignored`() {
        val screenA: Screen = mockk(relaxed = true)
        every { carContext.carAppApiLevel } returns 7
        mapboxScreenManager.putAll(
            MapboxScreen.NAVIGATION to MapboxScreenFactory { mockk(relaxed = true) },
            "SCREEN_A" to MapboxScreenFactory { screenA },
            "NEEDS_STATE" to MapboxScreenFactory {
                mapboxScreenManager.createDefaultMapScreenInstead(it, "state is missing")
            },
        )

        val uncaught = collectUncaughtExceptions {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            MapboxScreenManager.push("SCREEN_A")
            every { screenManager.stackSize } returns 1
            MapboxScreenManager.push("NEEDS_STATE")
        }

        assertEquals(emptyList<Throwable>(), uncaught)
        verify(exactly = 1) { screenManager.push(any()) }
        assertEquals(listOf("SCREEN_A" to screenA), mapboxScreenManager.screenStack.toList())
    }

    @Test(expected = IllegalStateException::class)
    fun `the default map screen cannot be created when its factory is not registered`() {
        every { carContext.carAppApiLevel } returns 6
        every { screenManager.stackSize } returns 0
        mapboxScreenManager["NEEDS_STATE"] = MapboxScreenFactory {
            mapboxScreenManager.createDefaultMapScreenInstead(it, "state is missing")
        }
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        mapboxScreenManager.createScreen("NEEDS_STATE")
    }

    @Test
    fun `defaultMapScreenKey is NAVIGATION when the host supports it and it is registered`() {
        every { carContext.carAppApiLevel } returns 7
        mapboxScreenManager[MapboxScreen.NAVIGATION] = MapboxScreenFactory { mockk() }

        assertEquals(MapboxScreen.NAVIGATION, mapboxScreenManager.defaultMapScreenKey())
    }

    @Suppress("DEPRECATION")
    @Test
    fun `defaultMapScreenKey is FREE_DRIVE when NAVIGATION is not registered`() {
        every { carContext.carAppApiLevel } returns 7

        assertEquals(MapboxScreen.FREE_DRIVE, mapboxScreenManager.defaultMapScreenKey())
    }

    @Suppress("DEPRECATION")
    @Test
    fun `defaultMapScreenKey is FREE_DRIVE when the host does not support NAVIGATION`() {
        every { carContext.carAppApiLevel } returns 6
        mapboxScreenManager[MapboxScreen.NAVIGATION] = MapboxScreenFactory { mockk() }

        assertEquals(MapboxScreen.FREE_DRIVE, mapboxScreenManager.defaultMapScreenKey())
    }

    /**
     * A car session whose lifecycle and [ScreenManager] are not shared with the class-level
     * [mapboxScreenManager], so a test observes only the instance it constructs.
     */
    private class IsolatedSession {
        val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.INITIALIZED)
        val screenManager: ScreenManager = mockk(relaxed = true)
        private val carContext: CarContext = mockk {
            every { getCarService(ScreenManager::class.java) } returns screenManager
        }
        val carContextOwner: MapboxCarContextOwner = mockk {
            every { carContext() } returns carContext
            every { lifecycle } returns lifecycleOwner.lifecycle
        }
    }

    /**
     * Screen events are collected on the main dispatcher, so an exception thrown while handling
     * one does not reach the caller: it goes to the thread's uncaught exception handler, which
     * crashes the app on a device. Capture those here so a test can assert there were none.
     *
     * This relies on [MainCoroutineRule] dispatching eagerly, so every event emitted inside
     * [block] is handled before it returns, and on coroutine failures outside `runTest` being
     * delivered to [Thread.getUncaughtExceptionHandler]. Keep the lifecycle transitions and
     * emissions under test inside [block].
     */
    private fun collectUncaughtExceptions(block: () -> Unit): List<Throwable> {
        val thread = Thread.currentThread()
        val original = thread.uncaughtExceptionHandler
        val uncaught = mutableListOf<Throwable>()
        thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e -> uncaught += e }
        try {
            block()
        } finally {
            thread.uncaughtExceptionHandler = original
        }
        return uncaught
    }

    @Test
    fun `isScreenBelowTop returns true for immediate parent`() {
        mapboxScreenManager.screenStack.push("SCREEN_A" to mockk())
        mapboxScreenManager.screenStack.push("SCREEN_B" to mockk())

        assertTrue(mapboxScreenManager.isScreenBelowTop("SCREEN_A"))
    }

    @Test
    fun `isScreenBelowTop ignores stale entries deeper in stack`() {
        mapboxScreenManager.screenStack.push("SCREEN_A" to mockk())
        mapboxScreenManager.screenStack.push("SCREEN_B" to mockk())
        mapboxScreenManager.screenStack.push("SCREEN_C" to mockk())

        assertFalse(mapboxScreenManager.isScreenBelowTop("SCREEN_A"))
    }

    @Test(expected = IllegalStateException::class)
    fun `requireScreenManager will crash accessed after the lifecycle is destroyed`() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertNotNull(mapboxScreenManager.requireScreenManager())
    }
}
