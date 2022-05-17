package com.mapbox.navigation.instrumentation_tests.ui.dropin

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingResource
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.instrumentation_tests.activity.NavigationViewLifecycleTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.idling.NavigationIdlingResource
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class NavigationViewLifecycleTest : BaseTest<NavigationViewLifecycleTestActivity>(
    NavigationViewLifecycleTestActivity::class.java
) {
    override fun setupMockLocation() = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 50.0
        longitude = 16.0
    }

    @Test
    fun NavigationView_destroyed_when_hosting_Fragment_destroyed() {
        val idling = ViewLifecycleIdlingResource(
            activity.firstFragment!!.navigationView!!,
            Lifecycle.State.DESTROYED
        )
        idling.register()
        runOnMainSync {
            activity.swapFragments()
        }
        Espresso.onIdle()
        idling.unregister()
    }

    @Test
    fun NavigationView_stopped_when_view_detached() {
        val idlingResumed = ViewLifecycleIdlingResource(
            activity.firstFragment!!.navigationView!!,
            Lifecycle.State.RESUMED
        )
        val idlingCreated = ViewLifecycleIdlingResource(
            activity.firstFragment!!.navigationView!!,
            Lifecycle.State.CREATED
        )
        idlingResumed.register()
        Espresso.onIdle()
        idlingResumed.unregister()

        idlingCreated.register()
        runOnMainSync {
            activity.firstFragment!!.detachNavigationView()
        }
        Espresso.onIdle()
        idlingCreated.unregister()
    }

    @Test
    fun NavigationView_resumed_when_view_reattached() {
        val idlingResumed = ViewLifecycleIdlingResource(
            activity.firstFragment!!.navigationView!!,
            Lifecycle.State.RESUMED
        )
        val idlingResumed2 = ViewLifecycleIdlingResource(
            activity.firstFragment!!.navigationView!!,
            Lifecycle.State.RESUMED
        )
        val idlingCreated = ViewLifecycleIdlingResource(
            activity.firstFragment!!.navigationView!!,
            Lifecycle.State.CREATED
        )
        idlingResumed.register()
        Espresso.onIdle()
        idlingResumed.unregister()

        idlingCreated.register()
        runOnMainSync {
            activity.firstFragment!!.detachNavigationView()
        }
        Espresso.onIdle()
        idlingCreated.unregister()

        idlingResumed2.register()
        runOnMainSync {
            activity.firstFragment!!.attachNavigationView()
        }
        Espresso.onIdle()
        idlingResumed2.unregister()
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ViewLifecycleIdlingResource(
    navigationView: NavigationView,
    private val targetState: Lifecycle.State
) : NavigationIdlingResource() {

    private var state: Lifecycle.State? = null
        set(value) {
            field = value
            if (value == targetState) {
                callback?.onTransitionToIdle()
            }
        }
    private var callback: IdlingResource.ResourceCallback? = null

    init {
        runOnMainSync {
            navigationView.lifecycle.addObserver(
                object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        state = event.targetState
                    }
                }
            )
        }
    }

    override fun getName() = this::class.simpleName

    override fun isIdleNow() = state == targetState

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
        if (isIdleNow) {
            callback?.onTransitionToIdle()
        }
    }
}
