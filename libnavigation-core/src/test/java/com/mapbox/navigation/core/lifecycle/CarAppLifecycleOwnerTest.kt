@file:Suppress("NoMockkVerifyImport")

package com.mapbox.navigation.core.lifecycle

import android.app.Activity
import androidx.core.app.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
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
            onCreate(carAppLifecycleOwner)
            onStart(carAppLifecycleOwner)
            onResume(carAppLifecycleOwner)
            onPause(carAppLifecycleOwner)
            onStop(carAppLifecycleOwner)
            onStart(carAppLifecycleOwner)
            onResume(carAppLifecycleOwner)
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
            onCreate(carAppLifecycleOwner)
            onStart(carAppLifecycleOwner)
            onResume(carAppLifecycleOwner)
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
            onCreate(carAppLifecycleOwner)
            onStart(carAppLifecycleOwner)
        }
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            onActivityCreated(activity, mockk())
            onActivityStarted(activity)
            onActivityStopped(activity)
        }
        carAppLifecycleOwner.startedReferenceCounter.onStop(carAppLifecycleOwner)
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
            onCreate(carAppLifecycleOwner)
            onStart(carAppLifecycleOwner)
            onResume(carAppLifecycleOwner)
            onPause(carAppLifecycleOwner)
            onStop(carAppLifecycleOwner)
        }
        carAppLifecycleOwner.activityLifecycleCallbacks.onActivityStopped(activity)
        carAppLifecycleOwner.startedReferenceCounter.onDestroy(carAppLifecycleOwner)

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
        carAppLifecycleOwner.startedReferenceCounter.onCreate(testLifecycleOwnerA)
        carAppLifecycleOwner.startedReferenceCounter.onStart(testLifecycleOwnerA)
        carAppLifecycleOwner.startedReferenceCounter.onResume(testLifecycleOwnerA)
        carAppLifecycleOwner.startedReferenceCounter.onCreate(testLifecycleOwnerB)
        carAppLifecycleOwner.startedReferenceCounter.onStart(testLifecycleOwnerB)
        carAppLifecycleOwner.startedReferenceCounter.onResume(testLifecycleOwnerB)
        carAppLifecycleOwner.startedReferenceCounter.onPause(testLifecycleOwnerB)
        carAppLifecycleOwner.startedReferenceCounter.onStop(testLifecycleOwnerB)
        carAppLifecycleOwner.startedReferenceCounter.onStop(testLifecycleOwnerA)
        carAppLifecycleOwner.startedReferenceCounter.onDestroy(testLifecycleOwnerB)

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 1) { testLifecycleObserver.onPause(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    @Suppress("MaxLineLength")
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
    @Suppress("MaxLineLength")
    fun `activityLifecycleCallbacks verify orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activityA: Activity = mockActivity()
            every { activityA.isChangingConfigurations } returns false
            onActivityCreated(activityA, mockk())
            onActivityStarted(activityA)
            onActivityResumed(activityA)
            every { activityA.isChangingConfigurations } returns true
            onActivityPaused(activityA)
            onActivityStopped(activityA)
            onActivityDestroyed(activityA)
            val activityB: Activity = mockActivity()
            every { activityB.isChangingConfigurations } returns false
            onActivityCreated(activityB, mockk())
            onActivityStarted(activityB)
            onActivityResumed(activityB)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `activityLifecycleCallbacks verify backgrounded orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activityA: Activity = mockActivity()
            every { activityA.isChangingConfigurations } returns false
            onActivityCreated(activityA, mockk())
            every { activityA.isChangingConfigurations } returns true
            onActivityDestroyed(activityA)
            val activityB: Activity = mockActivity()
            every { activityB.isChangingConfigurations } returns false
            onActivityCreated(activityB, mockk())
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 0) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startedReferenceCounter verify Activity orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            val activityA: ComponentActivity = mockComponentActivity()
            every { activityA.isChangingConfigurations } returns false
            onCreate(activityA)
            onStart(activityA)
            onResume(activityA)
            every { activityA.isChangingConfigurations } returns true
            onPause(activityA)
            onStop(activityA)
            onDestroy(activityA)
            val activityB: ComponentActivity = mockComponentActivity()
            every { activityB.isChangingConfigurations } returns false
            onCreate(activityB)
            onStart(activityB)
            onResume(activityB)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startedReferenceCounter verify Activity backgrounded orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            val activityA: ComponentActivity = mockComponentActivity()
            every { activityA.isChangingConfigurations } returns false
            onCreate(activityA)
            every { activityA.isChangingConfigurations } returns true
            onDestroy(activityA)
            val activityB: ComponentActivity = mockComponentActivity()
            every { activityB.isChangingConfigurations } returns false
            onCreate(activityB)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 0) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startedReferenceCounter verify Fragment orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            val parentActivityA = mockActivity(false)
            val fragmentA: Fragment = mockk { every { activity } returns parentActivityA }
            onCreate(fragmentA)
            onStart(fragmentA)
            onResume(fragmentA)
            every { parentActivityA.isChangingConfigurations } returns true
            onPause(fragmentA)
            onStop(fragmentA)
            onDestroy(fragmentA)
            val parentActivityB = mockActivity(false)
            val fragmentB: Fragment = mockk { every { activity } returns parentActivityB }
            onCreate(fragmentB)
            onStart(fragmentB)
            onResume(fragmentB)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startedReferenceCounter verify Fragment backgrounded orientation switching does not stop the app lifecycle`() {
        carAppLifecycleOwner.startedReferenceCounter.apply {
            val parentActivityA = mockActivity(false)
            val fragmentA: Fragment = mockk { every { activity } returns parentActivityA }
            onCreate(fragmentA)
            every { parentActivityA.isChangingConfigurations } returns true
            onDestroy(fragmentA)
            val activityB: ComponentActivity = mockComponentActivity()
            every { activityB.isChangingConfigurations } returns false
            onCreate(activityB)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 0) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `verify app can restart after everything is destroyed`() {
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activity: Activity = mockActivity()
            onActivityCreated(activity, mockk())
            onActivityStarted(activity)
            onActivityResumed(activity)
        }
        carAppLifecycleOwner.startedReferenceCounter.apply {
            onCreate(carAppLifecycleOwner)
            onStart(carAppLifecycleOwner)
            onResume(carAppLifecycleOwner)
        }
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activity: Activity = mockActivity()
            onActivityPaused(activity)
            onActivityStopped(activity)
            onActivityDestroyed(activity)
        }
        carAppLifecycleOwner.startedReferenceCounter.apply {
            onPause(carAppLifecycleOwner)
            onStop(carAppLifecycleOwner)
            onDestroy(carAppLifecycleOwner)
            onCreate(carAppLifecycleOwner)
            onStart(carAppLifecycleOwner)
            onResume(carAppLifecycleOwner)
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

        testLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        testLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

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

        testLifecycleOwnerA.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        testLifecycleOwnerB.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        testLifecycleOwnerA.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        testLifecycleOwnerB.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

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

        testLifecycleOwnerA.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        testLifecycleOwnerB.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        testLifecycleOwnerA.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        testLifecycleOwnerB.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

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
        testLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        carAppLifecycleOwner.detach(testLifecycleOwner)

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    private fun mockActivity(isChangingConfig: Boolean = false): FragmentActivity = mockk {
        every { isChangingConfigurations } returns isChangingConfig
    }

    private fun mockComponentActivity(isChangingConfig: Boolean = false): ComponentActivity =
        mockk { every { isChangingConfigurations } returns isChangingConfig }
}
