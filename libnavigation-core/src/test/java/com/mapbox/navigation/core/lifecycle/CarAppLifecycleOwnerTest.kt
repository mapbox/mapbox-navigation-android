@file:Suppress("NoMockkVerifyImport")

package com.mapbox.navigation.core.lifecycle

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalPreviewMapboxNavigationAPI
@RunWith(RobolectricTestRunner::class)
class CarAppLifecycleOwnerTest {

    private val testLifecycleObserver: DefaultLifecycleObserver = mockk(relaxUnitFun = true)
    private val carAppLifecycleOwner = CarAppLifecycleOwner()

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
        }

        verifyOrder {
            testLifecycleObserver.onCreate(any())
            testLifecycleObserver.onStart(any())
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
        carAppLifecycleOwner.startedReferenceCounter.apply {
            onCreate(carAppLifecycleOwner)
            onStart(carAppLifecycleOwner)
        }
        carAppLifecycleOwner.activityLifecycleCallbacks.apply {
            val activity: Activity = mockActivity()
            onActivityCreated(activity, mockk())
            onActivityStarted(activity)
            onActivityStopped(activity)
            onActivityDestroyed(activity)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
        verify(exactly = 0) { testLifecycleObserver.onStop(any()) }
        verify(exactly = 0) { testLifecycleObserver.onDestroy(any()) }
    }

    @Test
    fun `verify the lifecycle is not stopped when the car session is destroyed`() {
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
            onPause(carAppLifecycleOwner)
            onStop(carAppLifecycleOwner)
            onDestroy(carAppLifecycleOwner)
        }

        verify(exactly = 1) { testLifecycleObserver.onCreate(any()) }
        verify(exactly = 1) { testLifecycleObserver.onStart(any()) }
        verify(exactly = 1) { testLifecycleObserver.onResume(any()) }
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
    fun `verify orientation switching does not stop the app lifecycle`() {
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
    fun `verify backgrounded orientation switching does not stop the app lifecycle`() {
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

    class TestLifecycleOwner : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
            .also { it.currentState = Lifecycle.State.INITIALIZED }
        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    }

    private fun mockActivity(isChangingConfig: Boolean = false): Activity = mockk {
        every { isChangingConfigurations } returns isChangingConfig
    }
}
