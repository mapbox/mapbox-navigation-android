package com.mapbox.navigation.ui.androidauto.screenmanager

import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.internal.context.MapboxCarContextOwner
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.Deque

/**
 * [MapboxScreenManager] allows you to prepare an experience that includes many screens with a
 * single function. Use the [MapboxCarContext.prepareScreens] to get started.
 *
 * ### How it works
 * Using [String] to [MapboxScreenFactory] pairing, the [MapboxScreenManager] will create a screen
 * based on a string provided. You can customize screens by setting the key value pair.
 *
 * [MapboxScreenManager] can be observed and controlled by the car or app. Observe the screen
 * changes with [MapboxScreenManager.screenEvent], change the screen with operations like
 * [MapboxScreenManager.replaceTop]. See the [MapboxScreenOperation] for all of the available
 * operations, each operation is applied through [MapboxScreenManager] functions.
 *
 * ### Set up
 * The [MapboxScreenManager] is provided to you through the [MapboxCarContext]. It is important
 * to keep the screen manager lifecycle close to [CarContext] to avoid memory leaks.
 * [MapboxScreenManager] does not work without the [ScreenManager] which is also bound to the
 * [CarContext].
 *
 * Example
 * ```
 * class MySession : Session() {
 *   private val mapboxCarContext = MapboxCarContext(lifecycle, MapboxCarMap())
 *       .prepareScreens()
 * --snip--
 * }
 * ```
 *
 * ### Customization
 *
 * At any point, you can change the mapping of the [MapboxScreenManager] with setters. The
 * [MapboxScreenFactory] instances are expected to have a lifecycle as long as the [Session], so
 * an instance of [MapboxScreenManager] is required. You can access this class through the
 *
 * [MapboxScreenFactory.create]
 * Example
 * ```
 * mapboxCarContext.screenManager["MY_SCREEN"] = MyScreenFactory()
 *
 * // When your app is showing on the Android Auto head unit, the MyScreenFactory will be shown
 * // on the top of the back stack.
 * MapboxScreenManager.replaceTop("MY_SCREEN")
 * ```
 */
class MapboxScreenManager internal constructor(
    private val carContextOwner: MapboxCarContextOwner,
) {
    private var screenManager: ScreenManager? = null
    private val screenFactoryMap = mutableMapOf<String, MapboxScreenFactory>()

    @VisibleForTesting
    internal val screenStack: Deque<Pair<String, Screen>> = ArrayDeque()

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            screenManager = carContextOwner.carContext().getCarService(ScreenManager::class.java)
            owner.lifecycleScope.launch {
                screenEvent.collect { onScreenEvent(it) }
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            screenFactoryMap.clear()
            screenManager = null
        }
    }

    init {
        carContextOwner.lifecycle.addObserver(lifecycleObserver)
    }

    /**
     * This should be used to create a screen from [Session.onCreateScreen]. If the screen changes
     * the [screenEvent] observers will be notified with an [MapboxScreenOperation.CREATED] event.
     */
    @UiThread
    fun createScreen(screenKey: String): Screen {
        val screenManager = requireScreenManager()
        val currentTop = screenStack.peek()
        if (screenManager.stackSize > 0 && screenManager.top == currentTop?.second) {
            if (screenKey == currentTop.first) {
                logAndroidAuto("$TAG createScreen top is already set to $screenKey")
                return screenManager.top
            }
        }
        val factory: MapboxScreenFactory = requireScreenFactory(screenKey)
        return factory.create(carContextOwner.carContext()).also { screen ->
            val event = MapboxScreenEvent(screenKey, MapboxScreenOperation.CREATED)
            logAndroidAuto("$TAG createScreen Push ${screen.javaClass.simpleName}")
            screenManager.push(screen)
            screenStack.push(Pair(screenKey, screen))
            screenKeyMutable.tryEmit(event)
        }
    }

    /**
     * Calling this function will pop the back stack of the [ScreenManager]. If there are no
     * screens on the backstack, it will safely return false. If you are using the [ScreenManager]
     * and the [goBack] operation results in an unknown backstack, this will throw an
     * [IllegalStateException].
     *
     * When this function returns true, all [screenEvent] observers will be notified of an
     * [MapboxScreenOperation.GO_BACK] event. This operation requires an instance of the
     * [MapboxScreenManager] in order to verify the back-stack.
     */
    @Throws(IllegalStateException::class)
    @UiThread
    fun goBack(): Boolean {
        val screenManager = requireScreenManager()
        if (screenStack.size <= 1 || screenManager.stackSize <= 1) return false
        val topMatches = screenManager.top == screenStack.peek()?.second
        return if (topMatches) {
            screenStack.pop()
            screenManager.pop()
            val newTop = screenStack.peek()
            logAndroidAuto("$TAG goBack to ${newTop?.first}.")
            check(newTop != null && screenManager.top == newTop.second) {
                "goBack needs the MapboxScreenManager and ScreenManager to have similar screen " +
                    "back-stacks. ScreenManager top is not equal to ${newTop?.first}."
            }
            screenKeyMutable.tryEmit(
                MapboxScreenEvent(newTop.first, MapboxScreenOperation.GO_BACK),
            )
            true
        } else {
            logAndroidAuto(
                "$TAG goBack cannot remove the top because " +
                    "${screenManager.top::class.simpleName} is not the top of MapboxScreenManager.",
            )
            false
        }
    }

    /**
     * Allows you to put all defined screen factories into the manager in one operation.
     */
    fun putAll(vararg pairs: Pair<String, MapboxScreenFactory>) = apply {
        screenFactoryMap.putAll(pairs)
    }

    /**
     * Returns the previously set screen factory.
     */
    operator fun <T : MapboxScreenFactory> set(key: String, factory: T): MapboxScreenFactory? {
        return screenFactoryMap.put(key, factory)
    }

    /**
     * Check if there is a factory assigned to the key. This can be used to assign a factory when
     * there is not one set. This can also be used to verify the type of the factory.
     */
    operator fun contains(key: String): Boolean {
        return screenFactoryMap.contains(key)
    }

    /**
     * Provides access to the [MapboxScreenFactory] for the specified screen key. This will throw
     * an exception if it is accessed when it is not available.
     */
    @Throws(IllegalStateException::class)
    @Suppress("UNCHECKED_CAST")
    internal fun <T : MapboxScreenFactory> requireScreenFactory(key: String): T {
        val factory = screenFactoryMap[key] as? T
        checkNotNull(factory) {
            "CarScreenFactory was not found for $key. Make sure the car" +
                " Session is created and the MapboxScreenManager contains this factory key."
        }
        return factory
    }

    /**
     * Provides access to the [ScreenManager] to perform manual operations. This will throw an
     * exception if it is accessed when it is not available.
     */
    @Throws(IllegalStateException::class)
    internal fun requireScreenManager(): ScreenManager {
        val screenManager = this.screenManager
        checkNotNull(screenManager) {
            "You cannot use the ScreenManager when it does not exist. Make sure the car Session" +
                " is created and the MapboxScreenManager has been attached."
        }
        return screenManager
    }

    private fun onScreenEvent(event: MapboxScreenEvent) {
        when (event.operation) {
            MapboxScreenOperation.REPLACE_TOP -> onReplaceTop(event.key)
            MapboxScreenOperation.PUSH -> onPush(event.key)
            MapboxScreenOperation.GO_BACK,
            MapboxScreenOperation.CREATED,
            -> {
                // Handled by goBack and createScreen functions.
            }
        }
    }

    private fun onReplaceTop(key: String) {
        if (key == screenStack.peek()?.first) {
            logAndroidAuto("$TAG replaceTop exit, the top is already set to $key")
            return
        }
        val factory: MapboxScreenFactory = requireScreenFactory(key)
        val screen = factory.create(carContextOwner.carContext())
        screenStack.push(Pair(key, screen))
        val screenManager = requireScreenManager()
        logAndroidAuto("$TAG replaceTop $key remove ${screenManager.stackSize} screens")
        screenManager.replaceTop(screen)
    }

    private fun ScreenManager.replaceTop(screen: Screen) {
        if (stackSize > 0) {
            popToRoot()
            val root = top
            push(screen)
            root.finish()
        } else {
            push(screen)
        }
    }

    private fun onPush(key: String) {
        if (key == screenStack.peek()?.first) {
            logAndroidAuto("$TAG push exit, the top is already set to $key")
            return
        }
        val factory: MapboxScreenFactory = requireScreenFactory(key)
        val screen = factory.create(carContextOwner.carContext())
        screenStack.push(Pair(key, screen))
        logAndroidAuto("$TAG Push $key on top of ${screenManager?.stackSize} screens")
        requireScreenManager().push(screen)
    }

    companion object {
        private const val TAG = "MapboxScreenManager"

        /**
         * The [ScreenManager] allows for 4 or less [Screen]. This makes it possible to fill the
         * backstack with screens.
         */
        @VisibleForTesting
        internal const val REPLAY_CACHE = 4

        @VisibleForTesting
        internal val screenKeyMutable by lazy {
            MutableSharedFlow<MapboxScreenEvent>(
                replay = REPLAY_CACHE,
                onBufferOverflow = BufferOverflow.SUSPEND,
            )
        }

        /**
         * This gives you the ability to observe the MapboxCarScreen in use. If the [ScreenManager]
         * is used directly this state will become inconsistent. Use [clear] in these cases.
         */
        @JvmStatic
        val screenEvent: SharedFlow<MapboxScreenEvent> by lazy { screenKeyMutable.asSharedFlow() }

        /**
         * Get the last [MapboxScreenEvent]. This will give you the top of the backstack.
         */
        @JvmStatic
        fun current(): MapboxScreenEvent? = screenEvent.replayCache.lastOrNull()

        /**
         * Replace the back stack with a screen on top.
         */
        @JvmStatic
        fun replaceTop(key: String) {
            screenKeyMutable.tryEmit(MapboxScreenEvent(key, MapboxScreenOperation.REPLACE_TOP))
        }

        /**
         * Push a screen to the back stack. Be aware that there must be less than 5 [Template]s at
         * a time.
         */
        @JvmStatic
        fun push(key: String) {
            screenKeyMutable.tryEmit(MapboxScreenEvent(key, MapboxScreenOperation.PUSH))
        }
    }
}
