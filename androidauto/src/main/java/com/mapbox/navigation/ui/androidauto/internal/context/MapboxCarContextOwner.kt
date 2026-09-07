package com.mapbox.navigation.ui.androidauto.internal.context

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/**
 * This will gain access to the [CarContext] and it will lose access when the lifecycle is
 * destroyed.
 */
class MapboxCarContextOwner(val lifecycle: Lifecycle) {
    private var carContext: CarContext? = null

    private val lifecycleObserver: DefaultLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            carContext = (owner as? Session?)?.carContext
                ?: (owner as? Screen)?.carContext
            checkNotNull(carContext) {
                "CarContextOwner can only be used by a Session or Screen."
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            carContext = null
        }
    }

    init {
        lifecycle.addObserver(lifecycleObserver)
    }

    /**
     * Provides access to the [CarContext] to perform manual operations. This will throw an
     * exception if it is accessed when it is not available.
     */
    @Throws(IllegalStateException::class)
    fun carContext(): CarContext {
        val carContext = this.carContext
        checkNotNull(carContext) {
            "Make sure the Lifecycle is CREATED before you can use the CarContext"
        }
        return carContext
    }
}
