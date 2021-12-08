package com.mapbox.navigation.core.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.utils.internal.LoggerProvider.logger

@ExperimentalPreviewMapboxNavigationAPI
internal class CarAppLifecycleOwner : LifecycleOwner {

    // Keeps track of the activities created and foregrounded
    private var activitiesCreated = 0
    private var activitiesForegrounded = 0

    // Keeps track of the car session created and foregrounded
    private var lifecycleCreated = 0
    private var lifecycleForegrounded = 0

    // Keeps track of the activities changing configurations
    private var createdChangingConfiguration = 0
    private var foregroundedChangingConfiguration = 0

    private val lifecycleRegistry = LifecycleRegistry(this)
        .apply { currentState = Lifecycle.State.INITIALIZED }

    @VisibleForTesting
    internal val startedReferenceCounter = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            if (createdChangingConfiguration > 0) {
                createdChangingConfiguration--
            } else {
                lifecycleCreated++
                logger.i(TAG, Message("LifecycleOwner ($owner) onCreate"))
                if (activitiesCreated == 0 && lifecycleCreated > 0 && lifecycleForegrounded == 0) {
                    changeState(Lifecycle.State.STARTED)
                }
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            if (foregroundedChangingConfiguration > 0) {
                foregroundedChangingConfiguration--
            } else {
                lifecycleForegrounded++
                logger.i(TAG, Message("LifecycleOwner ($owner) onStart"))
                if (activitiesCreated == 0 && lifecycleForegrounded > 0) {
                    changeState(Lifecycle.State.RESUMED)
                }
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            if (owner.isChangingConfigurations()) {
                foregroundedChangingConfiguration++
            } else {
                lifecycleForegrounded--
                logger.i(TAG, Message("LifecycleOwner ($owner) onStop"))
                if (activitiesForegrounded == 0 && lifecycleForegrounded == 0) {
                    changeState(Lifecycle.State.STARTED)
                }
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            if (owner.isChangingConfigurations()) {
                createdChangingConfiguration++
            } else {
                lifecycleCreated--
                logger.i(TAG, Message("LifecycleOwner ($owner) onDestroy"))
                if (activitiesForegrounded == 0 && lifecycleForegrounded == 0) {
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
            if (createdChangingConfiguration > 0) {
                createdChangingConfiguration--
            } else {
                activitiesCreated++
                logger.i(TAG, Message("app onActivityCreated"))
                if (lifecycleCreated == 0 && activitiesCreated == 1) {
                    changeState(Lifecycle.State.STARTED)
                }
            }
        }

        override fun onActivityStarted(activity: Activity) {
            if (foregroundedChangingConfiguration > 0) {
                foregroundedChangingConfiguration--
            } else {
                activitiesForegrounded++
                logger.i(TAG, Message("app onActivityStarted"))
                if (lifecycleCreated == 0 && activitiesForegrounded == 1) {
                    changeState(Lifecycle.State.RESUMED)
                }
            }
        }

        override fun onActivityResumed(activity: Activity) {
            logger.i(TAG, Message("app onActivityResumed"))
        }

        override fun onActivityPaused(activity: Activity) {
            logger.i(TAG, Message("app onActivityPaused"))
        }

        override fun onActivityStopped(activity: Activity) {
            if (activity.isChangingConfigurations) {
                foregroundedChangingConfiguration++
            } else {
                activitiesForegrounded--
                logger.i(TAG, Message("app onActivityStopped"))
                if (lifecycleCreated == 0 &&
                    activitiesCreated == 0 &&
                    activitiesForegrounded == 0
                ) {
                    check(activitiesCreated == 0 && activitiesForegrounded == 0) {
                        "onActivityStopped when no activities exist"
                    }
                    changeState(Lifecycle.State.STARTED)
                }
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            logger.i(TAG, Message("app onActivitySaveInstanceState"))
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (activity.isChangingConfigurations) {
                createdChangingConfiguration++
            } else {
                activitiesCreated--
                logger.i(TAG, Message("app onActivityDestroyed"))
                if (lifecycleCreated == 0 && activitiesCreated == 0) {
                    changeState(Lifecycle.State.CREATED)
                }
            }
        }
    }

    private fun changeState(state: Lifecycle.State) {
        if (lifecycleRegistry.currentState != state) {
            lifecycleRegistry.currentState = state
            logger.i(TAG, Message("changeState ${lifecycleRegistry.currentState}"))
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    internal fun attachAllActivities(application: Application) {
        logger.i(TAG, Message("attachAllActivities"))
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    fun attach(lifecycleOwner: LifecycleOwner) {
        logger.i(TAG, Message("attach"))
        lifecycleOwner.lifecycle.addObserver(startedReferenceCounter)
    }

    fun detach(lifecycleOwner: LifecycleOwner) {
        logger.i(TAG, Message("detach"))
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
        private val TAG = Tag("MbxCarAppLifecycleOwner")
    }
}
