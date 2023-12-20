package com.mapbox.navigation.core.internal.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.navigation.utils.internal.DefaultLifecycleObserver
import com.mapbox.navigation.utils.internal.logI

class CarAppLifecycleOwner : LifecycleOwner {

    // Keeps track of the activities created and foregrounded
    private val activitiesCreated = hashSetOf<Activity>()
    private val activitiesForegrounded = hashSetOf<Activity>()

    // Keeps track of the car session created and foregrounded
    private val lifecycleCreated = hashSetOf<LifecycleOwner>()
    private val lifecycleForegrounded = hashSetOf<LifecycleOwner>()

    // Keeps track of the activities changing configurations
    private var createdChangingConfiguration = 0
    private var foregroundedChangingConfiguration = 0

    private val lifecycleRegistry = LifecycleRegistry(this)
        .apply { currentState = Lifecycle.State.INITIALIZED }

    @VisibleForTesting
    internal val startedReferenceCounter = object : DefaultLifecycleObserver() {
        override fun onCreate(owner: LifecycleOwner) {
            if (!lifecycleCreated.add(owner)) return
            if (createdChangingConfiguration > 0) {
                createdChangingConfiguration--
            } else {
                logI("LifecycleOwner ($owner) onCreate", LOG_CATEGORY)
                if (activitiesCreated.size == 0 && lifecycleCreated.size == 1) {
                    changeState(Lifecycle.State.STARTED)
                }
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            if (!lifecycleForegrounded.add(owner)) return
            if (foregroundedChangingConfiguration > 0) {
                foregroundedChangingConfiguration--
            } else {
                logI("LifecycleOwner ($owner) onStart", LOG_CATEGORY)
                if (activitiesForegrounded.size == 0 && lifecycleForegrounded.size == 1) {
                    changeState(Lifecycle.State.RESUMED)
                }
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            if (!lifecycleForegrounded.remove(owner)) return
            if (owner.isChangingConfigurations()) {
                foregroundedChangingConfiguration++
            } else {
                logI("LifecycleOwner ($owner) onStop", LOG_CATEGORY)
                if (activitiesForegrounded.size == 0 &&
                    lifecycleForegrounded.size == 0 &&
                    foregroundedChangingConfiguration == 0
                ) {
                    changeState(Lifecycle.State.STARTED)
                }
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            if (!lifecycleCreated.remove(owner)) return
            if (owner.isChangingConfigurations()) {
                createdChangingConfiguration++
            } else {
                logI("LifecycleOwner ($owner) onDestroy", LOG_CATEGORY)
                if (activitiesCreated.size == 0 &&
                    lifecycleCreated.size == 0 &&
                    createdChangingConfiguration == 0
                ) {
                    changeState(Lifecycle.State.CREATED)
                }
            }
        }

        private fun LifecycleOwner.isChangingConfigurations(): Boolean =
            (this is Activity && this.isChangingConfigurations) ||
                (this is Fragment && this.activity?.isChangingConfigurations == true)
    }

    @VisibleForTesting
    internal val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (!activitiesCreated.add(activity)) return
            if (createdChangingConfiguration > 0) {
                createdChangingConfiguration--
            } else {
                logI("app onActivityCreated", LOG_CATEGORY)
                if (lifecycleCreated.size == 0 && activitiesCreated.size == 1) {
                    changeState(Lifecycle.State.STARTED)
                }
            }
        }

        override fun onActivityStarted(activity: Activity) {
            if (!activitiesForegrounded.add(activity)) return
            if (foregroundedChangingConfiguration > 0) {
                foregroundedChangingConfiguration--
            } else {
                logI("app onActivityStarted", LOG_CATEGORY)
                if (lifecycleForegrounded.size == 0 && activitiesForegrounded.size == 1) {
                    changeState(Lifecycle.State.RESUMED)
                }
            }
        }

        override fun onActivityResumed(activity: Activity) {
            logI("app onActivityResumed", LOG_CATEGORY)
        }

        override fun onActivityPaused(activity: Activity) {
            logI("app onActivityPaused", LOG_CATEGORY)
        }

        override fun onActivityStopped(activity: Activity) {
            if (!activitiesForegrounded.remove(activity)) return
            if (activity.isChangingConfigurations) {
                foregroundedChangingConfiguration++
            } else {
                logI("app onActivityStopped", LOG_CATEGORY)
                if (lifecycleForegrounded.size == 0 &&
                    activitiesForegrounded.size == 0 &&
                    foregroundedChangingConfiguration == 0
                ) {
                    changeState(Lifecycle.State.STARTED)
                }
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            logI("app onActivitySaveInstanceState", LOG_CATEGORY)
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (!activitiesCreated.remove(activity)) return
            if (activity.isChangingConfigurations) {
                createdChangingConfiguration++
            } else {
                logI("app onActivityDestroyed", LOG_CATEGORY)
                if (lifecycleCreated.size == 0 &&
                    activitiesCreated.size == 0 &&
                    createdChangingConfiguration == 0
                ) {
                    changeState(Lifecycle.State.CREATED)
                }
            }
        }
    }

    private fun changeState(state: Lifecycle.State) {
        if (lifecycleRegistry.currentState != state) {
            lifecycleRegistry.currentState = state
            logI("changeState ${lifecycleRegistry.currentState}", LOG_CATEGORY)
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    fun attachAllActivities(application: Application) {
        logI("attachAllActivities", LOG_CATEGORY)
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    fun attach(lifecycleOwner: LifecycleOwner) {
        logI("attach", LOG_CATEGORY)
        lifecycleOwner.lifecycle.addObserver(startedReferenceCounter)
    }

    fun detach(lifecycleOwner: LifecycleOwner) {
        logI("detach", LOG_CATEGORY)
        lifecycleOwner.lifecycle.removeObserver(startedReferenceCounter)
        val currentState = lifecycleOwner.lifecycle.currentState
        if (currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            startedReferenceCounter.onPause(lifecycleOwner)
        }
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) {
            startedReferenceCounter.onStop(lifecycleOwner)
        }
        if (currentState.isAtLeast(Lifecycle.State.CREATED)) {
            startedReferenceCounter.onDestroy(lifecycleOwner)
        }
    }

    fun isConfigurationChanging(): Boolean =
        createdChangingConfiguration > 0 || foregroundedChangingConfiguration > 0

    private companion object {
        private const val LOG_CATEGORY = "CarAppLifecycleOwner"
    }
}
