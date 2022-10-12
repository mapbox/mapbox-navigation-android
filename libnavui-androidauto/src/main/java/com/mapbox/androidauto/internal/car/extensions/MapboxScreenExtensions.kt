@file:JvmName("MapboxScreenEx")

package com.mapbox.androidauto.internal.car.extensions

import androidx.activity.OnBackPressedCallback
import androidx.car.app.Screen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.androidauto.screenmanager.MapboxScreenManager

/**
 * When you push a [Screen] to the [MapboxScreenManager], you also need handle back pressed.
 *
 * ```
 * init { addBackPressedHandler { mapboxCarContext.mapboxScreenManager.goBack() }
 * ```
 */
fun Screen.addBackPressedHandler(callback: () -> Unit) {
    val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            callback.invoke()
        }
    }
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            carContext.onBackPressedDispatcher.addCallback(
                backPressCallback
            )
        }

        override fun onPause(owner: LifecycleOwner) {
            backPressCallback.remove()
        }
    })
}
