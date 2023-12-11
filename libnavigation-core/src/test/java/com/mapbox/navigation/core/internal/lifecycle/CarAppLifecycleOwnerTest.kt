package com.mapbox.navigation.core.internal.lifecycle

import android.app.Activity
import androidx.core.app.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalPreviewMapboxNavigationAPI
@RunWith(RobolectricTestRunner::class)
@Suppress("MaxLineLength")
class CarAppLifecycleOwnerTest {
    private val testLifecycleObserver: DefaultLifecycleObserver = mockk(relaxUnitFun = true)
    private val carAppLifecycleOwner = CarAppLifecycleOwner()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Before
    fun setup() {
        carAppLifecycleOwner.lifecycle.addObserver(testLifecycleObserver)
    }

    @Test
    fun `verify order when the app is started without the car`() {
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activity: Activity = mockActivity()
            onActivityCreated(activity, mockk())
            onActivityStarted(activity)
            onActivityResumed(activity)
        }

        verifyOrder {
            testLifecycleObserver.onCreate(any())
            testLifecycleObserver.onStart(any())
            testLifecycleObserver.onResume(any())
        }
    }

    @Test
    fun `verify order when the app is foregrounded and backgrounded`() {
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activity: Activity = mockActivity()
            onActivityCreated(activity, mockk())
            onActivityStarted(activity)
            onActivityResumed(activity)
            onActivityPaused(activity)
            onActivityStopped(activity)
            onActivityStarted(activity)
            onActivityResumed(activity)
        }

        verifyOrder {
            testLifecycleObserver.onCreate(any())
            testLifecycleObserver.onStart(any())
            testLifecycleObserver.onResume(any())
            testLifecycleObserver.onPause(any())
            testLifecycleObserver.onResume(any())
        }
    }

    @Test
    fun `verify order when the lifecycle is backgrounded and foregrounded`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_CREATE)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_START)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_RESUME)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_PAUSE)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_STOP)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_START)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_RESUME)
        }

        verifyOrder {
            testLifecycleObserver.onCreate(any())
            testLifecycleObserver.onStart(any())
            testLifecycleObserver.onResume(any())
            testLifecycleObserver.onPause(any())
            testLifecycleObserver.onResume(any())
        }
    }

    @Test
    fun `verify order when the car is started without the app`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_CREATE)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_START)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_RESUME)
        }

        verifyOrder {
            testLifecycleObserver.onCreate(any())
            testLifecycleObserver.onStart(any())
            testLifecycleObserver.onResume(any())
        }
    }

    @Test
    fun `verify the lifecycle is not stopped when the activities are destroyed`() {
        val activity = mockActivity()
        carAppLifecycleOwner.startedReferenceCounter.apply {
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_CREATE)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_START)
        }
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            onActivityCreated(activity, mockk())
            onActivityStarted(activity)
            onActivityStopped(activity)
        }
        carAppLifecycleOwner.startedReferenceCounter
            .onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_STOP)
        carAppLifecycleOwner.activityLifecycleCallbacks.onActivityDestroyed(activity)

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 1) { testLifecycleObserver.onPause(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `verify the lifecycle is not stopped when the car session is destroyed`() {
        val activity = mockActivity()
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            onActivityCreated(activity, mockk())
            onActivityStarted(activity)
            onActivityResumed(activity)
        }
        carAppLifecycleOwner.startedReferenceCounter.apply {
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_CREATE)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_START)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_RESUME)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_PAUSE)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_STOP)
        }
        carAppLifecycleOwner.activityLifecycleCallbacks.onActivityStopped(activity)
        carAppLifecycleOwner.startedReferenceCounter
            .onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_DESTROY)

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 1) { testLifecycleObserver.onPause(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `verify the lifecycle is not stopped when only one attached lifecycle is destroyed`() {
        val testLifecycleOwnerA = TestLifecycleOwner()
        val testLifecycleOwnerB = TestLifecycleOwner()
        with (carAppLifecycleOwner.startedReferenceCounter) {
            onStateChanged(testLifecycleOwnerA, Lifecycle.Event.ON_CREATE)
            onStateChanged(testLifecycleOwnerA, Lifecycle.Event.ON_START)
            onStateChanged(testLifecycleOwnerA, Lifecycle.Event.ON_RESUME)
            onStateChanged(testLifecycleOwnerB, Lifecycle.Event.ON_CREATE)
            onStateChanged(testLifecycleOwnerB, Lifecycle.Event.ON_START)
            onStateChanged(testLifecycleOwnerB, Lifecycle.Event.ON_RESUME)
            onStateChanged(testLifecycleOwnerB, Lifecycle.Event.ON_PAUSE)
            onStateChanged(testLifecycleOwnerB, Lifecycle.Event.ON_STOP)
            onStateChanged(testLifecycleOwnerA, Lifecycle.Event.ON_STOP)
            onStateChanged(testLifecycleOwnerB, Lifecycle.Event.ON_DESTROY)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 1) { testLifecycleObserver.onPause(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `verify activity switching does not stop the app lifecycle`() {
        /**
         * https://developer.android.com/guide/components/activities/activity-lifecycle#coordinating-activities
         *
         * Activity A's onPause() method executes.
         * Activity B's onCreate(), onStart(), and onResume() methods execute in sequence. (Activity B now has user focus.)
         * Then, if Activity A is no longer visible on screen, its onStop() method executes.
         */
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activityA: Activity = mockActivity()
            onActivityCreated(activityA, mockk())
            onActivityStarted(activityA)
            onActivityResumed(activityA)
            onActivityPaused(activityA)
            val activityB: Activity = mockActivity()
            onActivityCreated(activityB, mockk())
            onActivityStarted(activityB)
            onActivityResumed(activityB)
            onActivityStopped(activityA)
            onActivityDestroyed(activityA)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `activityLifecycleCallbacks verify orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activityA = mockActivity()
            val activityB = mockActivity()
            onActivityCreated(activityA, mockk())
            onActivityStarted(activityA)
            onActivityResumed(activityA)
            onActivityCreated(activityB, mockk())
            onActivityStarted(activityB)
            onActivityResumed(activityB)
            every { activityA.isChangingConfigurations } returns true
            onActivityPaused(activityA)
            onActivityStopped(activityA)
            onActivityDestroyed(activityA)
            onActivityPaused(activityB)
            onActivityStopped(activityB)
            onActivityDestroyed(activityB)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `activityLifecycleCallbacks verify backgrounded orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activityA = mockActivity()
            val activityB = mockActivity()
            onActivityCreated(activityA, mockk())
            onActivityCreated(activityB, mockk())
            every { activityA.isChangingConfigurations } returns true
            onActivityDestroyed(activityA)
            onActivityDestroyed(activityB)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 0) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `startedReferenceCounter verify Activity orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            val activityA = mockComponentActivity()
            val activityB = mockComponentActivity()
            onStateChanged(activityA, Lifecycle.Event.ON_CREATE)
            onStateChanged(activityA, Lifecycle.Event.ON_START)
            onStateChanged(activityA, Lifecycle.Event.ON_RESUME)
            onStateChanged(activityB, Lifecycle.Event.ON_CREATE)
            onStateChanged(activityB, Lifecycle.Event.ON_START)
            onStateChanged(activityB, Lifecycle.Event.ON_RESUME)
            every { activityA.isChangingConfigurations } returns true
            onStateChanged(activityA, Lifecycle.Event.ON_PAUSE)
            onStateChanged(activityA, Lifecycle.Event.ON_STOP)
            onStateChanged(activityA, Lifecycle.Event.ON_DESTROY)
            onStateChanged(activityB, Lifecycle.Event.ON_PAUSE)
            onStateChanged(activityB, Lifecycle.Event.ON_STOP)
            onStateChanged(activityB, Lifecycle.Event.ON_DESTROY)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `startedReferenceCounter verify Activity backgrounded orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            val activityA = mockComponentActivity()
            val activityB = mockComponentActivity()
            onStateChanged(activityA, Lifecycle.Event.ON_CREATE)
            onStateChanged(activityB, Lifecycle.Event.ON_CREATE)
            every { activityA.isChangingConfigurations } returns true
            onStateChanged(activityA, Lifecycle.Event.ON_DESTROY)
            onStateChanged(activityB, Lifecycle.Event.ON_DESTROY)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 0) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `startedReferenceCounter verify Fragment orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            val parentActivityA = mockActivity()
            val parentActivityB = mockActivity()
            val fragmentA = mockk<Fragment> { every { activity } returns parentActivityA }
            val fragmentB = mockk<Fragment> { every { activity } returns parentActivityB }
            onStateChanged(fragmentA, Lifecycle.Event.ON_CREATE)
            onStateChanged(fragmentA, Lifecycle.Event.ON_START)
            onStateChanged(fragmentA, Lifecycle.Event.ON_RESUME)
            onStateChanged(fragmentB, Lifecycle.Event.ON_CREATE)
            onStateChanged(fragmentB, Lifecycle.Event.ON_START)
            onStateChanged(fragmentB, Lifecycle.Event.ON_RESUME)
            every { parentActivityA.isChangingConfigurations } returns true
            onStateChanged(fragmentA, Lifecycle.Event.ON_PAUSE)
            onStateChanged(fragmentA, Lifecycle.Event.ON_STOP)
            onStateChanged(fragmentA, Lifecycle.Event.ON_DESTROY)
            onStateChanged(fragmentB, Lifecycle.Event.ON_PAUSE)
            onStateChanged(fragmentB, Lifecycle.Event.ON_STOP)
            onStateChanged(fragmentB, Lifecycle.Event.ON_DESTROY)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `startedReferenceCounter verify Fragment backgrounded orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            val parentActivityA = mockActivity()
            val activityB = mockComponentActivity()
            val fragmentA = mockk<Fragment> { every { activity } returns parentActivityA }
            onStateChanged(fragmentA, Lifecycle.Event.ON_CREATE)
            onStateChanged(activityB, Lifecycle.Event.ON_CREATE)
            every { parentActivityA.isChangingConfigurations } returns true
            onStateChanged(fragmentA, Lifecycle.Event.ON_DESTROY)
            onStateChanged(activityB, Lifecycle.Event.ON_DESTROY)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 0) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `verify app can restart after everything is destroyed`() {
        val activity = mockActivity()
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            onActivityCreated(activity, mockk())
            onActivityStarted(activity)
            onActivityResumed(activity)
        }
        carAppLifecycleOwner.startedReferenceCounter.apply {
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_CREATE)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_START)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_RESUME)
        }
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            onActivityPaused(activity)
            onActivityStopped(activity)
            onActivityDestroyed(activity)
        }
        carAppLifecycleOwner.startedReferenceCounter.apply {
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_PAUSE)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_STOP)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_DESTROY)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_CREATE)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_START)
            onStateChanged(carAppLifecycleOwner, Lifecycle.Event.ON_RESUME)
        }

        verifyOrder {
            testLifecycleObserver.onCreate(any())
            testLifecycleObserver.onStart(any())
            testLifecycleObserver.onResume(any())
            testLifecycleObserver.onStop(any())
            testLifecycleObserver.onStart(any())
            testLifecycleObserver.onResume(any())
        }
        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 2) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 2) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `verify a single LifecycleOwner calls all events except destroy`() {
        val testLifecycleOwner = TestLifecycleOwner()
        carAppLifecycleOwner.attach(testLifecycleOwner)

        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `verify multiple LifecycleOwner will call events once`() {
        val testLifecycleOwnerA = TestLifecycleOwner()
        val testLifecycleOwnerB = TestLifecycleOwner()
        carAppLifecycleOwner.attach(testLifecycleOwnerA)
        carAppLifecycleOwner.attach(testLifecycleOwnerB)

        testLifecycleOwnerA.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        testLifecycleOwnerB.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        testLifecycleOwnerA.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        testLifecycleOwnerB.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `verify multiple LifecycleOwner started will call events once`() {
        val testLifecycleOwnerA = TestLifecycleOwner()
        val testLifecycleOwnerB = TestLifecycleOwner()
        carAppLifecycleOwner.attach(testLifecycleOwnerA)
        carAppLifecycleOwner.attach(testLifecycleOwnerB)

        testLifecycleOwnerA.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        testLifecycleOwnerB.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testLifecycleOwnerA.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        testLifecycleOwnerB.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `verify detach will cause destruction states`() {
        val testLifecycleOwner = TestLifecycleOwner()

        carAppLifecycleOwner.attach(testLifecycleOwner)
        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        carAppLifecycleOwner.detach(testLifecycleOwner)

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `activityLifecycleCallbacks verify repeated attach will not affect subsequent detach`() {
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activity = mockActivity()
            onActivityCreated(activity, mockk())
            onActivityStarted(activity)
            onActivityResumed(activity)
            onActivityCreated(activity, mockk())
            onActivityStarted(activity)
            onActivityResumed(activity)
            onActivityPaused(activity)
            onActivityStopped(activity)
            onActivityDestroyed(activity)

            verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
            verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
            verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
            verify(exactly = 1) { testLifecycleObserver.onPause(any()) }
            verify(exactly = 1) { testLifecycleObserver.onStop(any()) }
            verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
        }
    }

    @Test
    fun `startedReferenceCounter verify repeated attach will not affect subsequent detach`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            val activity = mockComponentActivity()
            onStateChanged(activity, Lifecycle.Event.ON_CREATE)
            onStateChanged(activity, Lifecycle.Event.ON_START)
            onStateChanged(activity, Lifecycle.Event.ON_RESUME)
            onStateChanged(activity, Lifecycle.Event.ON_CREATE)
            onStateChanged(activity, Lifecycle.Event.ON_START)
            onStateChanged(activity, Lifecycle.Event.ON_RESUME)
            onStateChanged(activity, Lifecycle.Event.ON_PAUSE)
            onStateChanged(activity, Lifecycle.Event.ON_STOP)
            onStateChanged(activity, Lifecycle.Event.ON_DESTROY)

            verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
            verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
            verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
            verify(exactly = 1) { testLifecycleObserver.onPause(any()) }
            verify(exactly = 1) { testLifecycleObserver.onStop(any()) }
            verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
        }
    }

    @Test
    fun `activityLifecycleCallbacks verify detach without attach will not affect subsequent attach`() {
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activity = mockActivity()
            onActivityPaused(activity)
            onActivityStopped(activity)
            onActivityDestroyed(activity)
            onActivityCreated(activity, mockk())
            onActivityStarted(activity)
            onActivityResumed(activity)

            verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
            verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
            verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
            verify(exactly = 0) { testLifecycleObserver.onPause(any()) }
            verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
            verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
        }
    }

    @Test
    fun `startedReferenceCounter verify detach without attach will not affect subsequent attach`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            val activity = mockComponentActivity()
            onStateChanged(activity, Lifecycle.Event.ON_PAUSE)
            onStateChanged(activity, Lifecycle.Event.ON_STOP)
            onStateChanged(activity, Lifecycle.Event.ON_DESTROY)
            onStateChanged(activity, Lifecycle.Event.ON_CREATE)
            onStateChanged(activity, Lifecycle.Event.ON_START)
            onStateChanged(activity, Lifecycle.Event.ON_RESUME)

            verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
            verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
            verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
            verify(exactly = 0) { testLifecycleObserver.onPause(any()) }
            verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
            verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
        }
    }

    class TestLifecycleOwner : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
            .also { it.currentState = Lifecycle.State.INITIALIZED }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    }

    private fun mockActivity(isChangingConfig: Boolean = false): FragmentActivity = mockk {
        every { isChangingConfigurations } returns isChangingConfig
    }

    private fun mockComponentActivity(isChangingConfig: Boolean = false): ComponentActivity =
        mockk { every { isChangingConfigurations } returns isChangingConfig }
}
