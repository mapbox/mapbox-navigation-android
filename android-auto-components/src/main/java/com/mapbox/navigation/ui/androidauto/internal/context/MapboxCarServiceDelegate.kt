package com.mapbox.navigation.ui.androidauto.internal.context

import androidx.car.app.CarContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Delegate function for the [MapboxCarServiceDelegate]. This gives you the ability to scope a
 * service to the [CarContext] lifecycle. If the service is accessed when the [CarContext]
 * is not available, it throw an [IllegalStateException].
 */
fun <T> MapboxCarContext.mapboxCarService(
    name: String,
    onCreate: () -> T,
): ReadOnlyProperty<Any, T> = MapboxCarServiceDelegate(
    mapboxCarContext = this,
    name = name,
    onCreate = onCreate,
    onDestroy = { },
)

/**
 * Delegate function for the [MapboxCarServiceDelegate]. Similar to [mapboxCarService] except that
 * it will register and unregister from [MapboxNavigationApp].
 */
fun <T : MapboxNavigationObserver> MapboxCarContext.mapboxCarNavigationService(
    name: String,
    onCreate: () -> T,
): ReadOnlyProperty<Any, T> {
    return MapboxCarServiceDelegate(
        mapboxCarContext = this,
        name = name,
        onCreate = {
            val value = onCreate()
            MapboxNavigationApp.registerObserver(value)
            value
        },
        onDestroy = {
            MapboxNavigationApp.unregisterObserver(it)
        },
    )
}

class MapboxCarServiceDelegate<T>(
    mapboxCarContext: MapboxCarContext,
    private val name: String,
    private val onCreate: () -> T,
    private val onDestroy: (T) -> Unit,
) : ReadOnlyProperty<Any, T> {
    private var value: T? = null

    init {
        mapboxCarContext.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    value = onCreate()
                }
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    value?.let { onDestroy(it) }
                    this@MapboxCarServiceDelegate.value = null
                }
            },
        )
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val value = value
        checkNotNull(value) {
            "$name cannot be accessed unless the lifecycle is CREATED."
        }
        return value
    }
}
