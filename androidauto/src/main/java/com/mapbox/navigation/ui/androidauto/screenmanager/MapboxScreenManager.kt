package com.mapbox.navigation.ui.androidauto.screenmanager

import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.model.Template
import androidx.car.app.versioning.CarAppApiLevels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.internal.context.MapboxCarContextOwner
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.Deque
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong

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

    /**
     * The screens this instance added to the [ScreenManager] back-stack, top first. It matches the
     * [ScreenManager] as long as screens are only changed through this class.
     */
    @VisibleForTesting
    internal val screenStack: Deque<Pair<String, Screen>> = ArrayDeque()

    /**
     * Events with a lower sequence were emitted before this instance existed, for example by a
     * previous [Session]. They stay available through [current] and the [screenEvent] replay, but
     * this instance does not execute them: the screens they refer to belong to another
     * [CarContext] and may depend on state that no longer exists.
     */
    private val firstOwnSequence = lastEmittedSequence() + 1

    /**
     * The key of each screen [createDefaultMapScreenInstead] returned, read by [createTracked].
     * Keyed by the screen, so a screen created while another factory runs cannot take its key.
     */
    private val defaultMapScreenKeys = WeakHashMap<Screen, String>()

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            screenManager = carContextOwner.carContext().getCarService(ScreenManager::class.java)
            owner.lifecycleScope.launch {
                screenEvent.collect { event ->
                    if (event.sequence < firstOwnSequence) {
                        onEventEmittedBeforeCreation(event)
                    } else {
                        onScreenEvent(event)
                    }
                }
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            screenFactoryMap.clear()
            screenStack.clear()
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
        val (createdKey, screen) = createTracked(factory, screenKey)
        if (screenManager.stackSize > 0 && screenManager.top == screen) {
            logAndroidAuto("$TAG createScreen $createdKey is already the top")
            return screen
        }
        logAndroidAuto("$TAG createScreen Push $createdKey ${screen.javaClass.simpleName}")
        if (screenManager.stackSize == 0) {
            screenStack.clear()
        }
        screenManager.push(screen)
        screenStack.push(Pair(createdKey, screen))
        emit(createdKey, MapboxScreenOperation.CREATED)
        return screen
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
            emit(newTop.first, MapboxScreenOperation.GO_BACK)
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
     * Returns true when the screen directly below the top of the back-stack has the [key].
     */
    internal fun isScreenBelowTop(key: String): Boolean {
        return screenStack.elementAtOrNull(1)?.first == key
    }

    /**
     * The screen that shows the map when there is nothing else to show:
     * [MapboxScreen.NAVIGATION] when the host supports it and a factory is registered for it,
     * otherwise [MapboxScreen.FREE_DRIVE]. [MapboxScreen.FREE_DRIVE] is returned even when no
     * factory is registered for it, in which case screen changes to it are ignored.
     */
    @Suppress("DEPRECATION")
    internal fun defaultMapScreenKey(): String {
        val carAppApiLevel = carContextOwner.carContext().carAppApiLevel
        return if (
            carAppApiLevel >= CarAppApiLevels.LEVEL_7 && contains(MapboxScreen.NAVIGATION)
        ) {
            MapboxScreen.NAVIGATION
        } else {
            MapboxScreen.FREE_DRIVE
        }
    }

    /**
     * For a [MapboxScreenFactory] that cannot create its screen as the first screen of a
     * [Session], for example because the state it reads belongs to a previous [Session]: creates
     * the [defaultMapScreenKey] screen instead, which is recorded under that key. When that map
     * screen is already the only screen, it is returned again and nothing changes.
     *
     * @throws IllegalStateException when other screens are shown, or when there is no factory for
     * the [defaultMapScreenKey]. A screen change event is then logged and ignored, while
     * [createScreen] throws.
     */
    @Throws(IllegalStateException::class)
    internal fun createDefaultMapScreenInstead(carContext: CarContext, reason: String): Screen {
        val key = defaultMapScreenKey()
        val hostStackSize = screenManager?.stackSize
        val shownMapScreen = screenStack.peek()?.takeIf { (shownKey, shownScreen) ->
            shownKey == key && screenStack.size == 1 && hostStackSize == 1 &&
                screenManager?.top == shownScreen
        }?.second
        if (shownMapScreen != null) {
            logAndroidAuto("$TAG $reason, $key is already shown")
            defaultMapScreenKeys[shownMapScreen] = key
            return shownMapScreen
        }
        check(hostStackSize == 0) {
            "$reason. Only the first screen can show $key instead"
        }
        logAndroidAutoFailure("$TAG $reason, showing $key instead")
        return requireScreenFactory<MapboxScreenFactory>(key).create(carContext).also {
            defaultMapScreenKeys[it] = key
        }
    }

    /**
     * Replaces the top screen with a new instance from its factory when the top is [key], keeping
     * the screens below it. Use this when the top screen has to reload its content, for example
     * after the state its factory reads has changed. A [MapboxScreenOperation.PUSH] event is
     * emitted for observers.
     *
     * @return false when the top is not [key] or a new screen could not be created, in which case
     * nothing changes.
     */
    @UiThread
    internal fun recreateTop(key: String): Boolean {
        val screenManager = screenManager ?: return false
        val currentTop = screenStack.peek()
        if (currentTop == null || currentTop.first != key) return false
        if (screenManager.stackSize == 0 || screenManager.top != currentTop.second) {
            logAndroidAuto("$TAG recreateTop exit, $key is not the top of the ScreenManager")
            return false
        }
        val factory = findScreenFactoryForEvent(key, RECREATE_TOP) ?: return false
        val (createdKey, screen) = createScreenForEvent(factory, key, RECREATE_TOP)
            ?: return false
        logAndroidAuto("$TAG recreateTop $key")
        screenManager.push(screen)
        currentTop.second.finish()
        screenStack.pop()
        screenStack.push(Pair(createdKey, screen))
        emit(createdKey, MapboxScreenOperation.PUSH)
        return true
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

    private fun onEventEmittedBeforeCreation(event: MapboxScreenEvent) {
        when (event.operation) {
            MapboxScreenOperation.REPLACE_TOP,
            MapboxScreenOperation.PUSH,
            -> logAndroidAuto(
                "$TAG ${event.operation} ${event.key} ignored, it was emitted before this " +
                    "MapboxScreenManager was created",
            )
        }
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
        val factory = findScreenFactoryForEvent(key, MapboxScreenOperation.REPLACE_TOP) ?: return
        val screenManager = requireScreenManager()
        val (createdKey, screen) =
            createScreenForEvent(factory, key, MapboxScreenOperation.REPLACE_TOP) ?: return
        logAndroidAuto("$TAG replaceTop $key remove ${screenManager.stackSize} screens")
        screenManager.replaceTop(screen)
        screenStack.clear()
        screenStack.push(Pair(createdKey, screen))
    }

    /**
     * This instance can receive an event it has no factory for, for example when the key was
     * never registered, or when an event is emitted after the lifecycle is CREATED and before the
     * factories are registered. Throwing here would crash the app from the event collector with
     * no way for the caller to intervene, so the event is logged and ignored instead.
     */
    private fun findScreenFactoryForEvent(
        key: String,
        operation: String,
    ): MapboxScreenFactory? {
        val factory = screenFactoryMap[key]
        if (factory == null) {
            logAndroidAutoFailure(
                "$TAG $operation ignored, there is no MapboxScreenFactory for $key. Make sure " +
                    "the factory is registered before screen changes to it are requested.",
            )
        }
        return factory
    }

    /**
     * A factory can depend on state that is not available, and an exception thrown here would
     * crash the app from the event collector, so the event is logged and ignored instead.
     */
    private fun createScreenForEvent(
        factory: MapboxScreenFactory,
        key: String,
        operation: String,
    ): Pair<String, Screen>? {
        val created = try {
            createTracked(factory, key)
        } catch (e: Exception) {
            logAndroidAutoFailure(
                "$TAG $operation ignored, the MapboxScreenFactory for $key failed to create a " +
                    "screen",
                e,
            )
            return null
        }
        if (screenStack.any { it.second == created.second }) {
            logAndroidAuto("$TAG $operation $key ignored, ${created.first} is already shown")
            return null
        }
        return created
    }

    /**
     * Creates the screen for [key] and returns it with the key it is shown for, which differs
     * from [key] when the factory used [createDefaultMapScreenInstead].
     */
    private fun createTracked(factory: MapboxScreenFactory, key: String): Pair<String, Screen> {
        val screen = factory.create(carContextOwner.carContext())
        return Pair(defaultMapScreenKeys.remove(screen) ?: key, screen)
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
        val factory = findScreenFactoryForEvent(key, MapboxScreenOperation.PUSH) ?: return
        val screenManager = requireScreenManager()
        val (createdKey, screen) =
            createScreenForEvent(factory, key, MapboxScreenOperation.PUSH) ?: return
        logAndroidAuto("$TAG Push $createdKey on top of ${screenManager.stackSize} screens")
        screenManager.push(screen)
        screenStack.push(Pair(createdKey, screen))
    }

    companion object {
        private const val TAG = "MapboxScreenManager"
        private const val RECREATE_TOP = "RECREATE_TOP"

        /**
         * The [ScreenManager] allows for 4 or less [Screen]. The replay lets late observers see
         * the most recent screen changes.
         */
        @VisibleForTesting
        internal const val REPLAY_CACHE = 4

        /**
         * Holds the events a collector has not handled yet, for example while it is busy with
         * the previous event or waiting for its dispatcher, so that a burst of screen changes is
         * not dropped.
         */
        @VisibleForTesting
        internal const val EXTRA_BUFFER_CAPACITY = 16

        private val lastSequence = AtomicLong(0)

        @VisibleForTesting
        internal val screenKeyMutable by lazy { createScreenEventFlow() }

        @VisibleForTesting
        internal fun createScreenEventFlow() = MutableSharedFlow<MapboxScreenEvent>(
            replay = REPLAY_CACHE,
            extraBufferCapacity = EXTRA_BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.SUSPEND,
        )

        @VisibleForTesting
        internal fun lastEmittedSequence(): Long = lastSequence.get()

        /**
         * No lock is held while emitting, because collectors can run inside [tryEmit]. A
         * [MapboxScreenManager] only compares the sequence with the one it was created at, so two
         * threads emitting at the same time may deliver their events in either order.
         */
        private fun emit(key: String, @MapboxScreenOperation.Type operation: String) {
            val event = MapboxScreenEvent(key, operation, lastSequence.incrementAndGet())
            if (!screenKeyMutable.tryEmit(event)) {
                logAndroidAutoFailure(
                    "$TAG dropped $event, a screenEvent collector is not keeping up",
                )
            }
        }

        /**
         * This gives you the ability to observe the MapboxCarScreen in use. If the [ScreenManager]
         * is used directly this state will become inconsistent.
         *
         * The events are shared by every [MapboxScreenManager] instance and up to the last 4
         * events are replayed to new observers. A [MapboxScreenManager] only executes the
         * [MapboxScreenOperation.REPLACE_TOP] and [MapboxScreenOperation.PUSH] events emitted
         * after it was created, so the [MapboxScreenManager] of a newly created [Session] does
         * not repeat the screen changes of a previous one. An event for a key without a
         * registered [MapboxScreenFactory], or whose factory fails to create a screen, is
         * ignored by the [MapboxScreenManager].
         */
        @JvmStatic
        val screenEvent: SharedFlow<MapboxScreenEvent> by lazy { screenKeyMutable.asSharedFlow() }

        /**
         * Get the last [MapboxScreenEvent]. This will give you the top of the backstack.
         *
         * This is the last requested event, which may not be shown: a
         * [MapboxScreenOperation.REPLACE_TOP] or [MapboxScreenOperation.PUSH] for a key without a
         * registered [MapboxScreenFactory] is ignored by the [MapboxScreenManager] but is still
         * returned here.
         *
         * When restoring a new [Session] from this event, note that a
         * [MapboxScreenOperation.PUSH] refers to a screen that was shown on top of another one,
         * and it may not work as the only screen on the back-stack.
         */
        @JvmStatic
        fun current(): MapboxScreenEvent? = screenEvent.replayCache.lastOrNull()

        /**
         * Replace the back stack with a screen on top.
         */
        @JvmStatic
        fun replaceTop(key: String) {
            emit(key, MapboxScreenOperation.REPLACE_TOP)
        }

        /**
         * Push a screen to the back stack. Be aware that there must be less than 5 [Template]s at
         * a time.
         */
        @JvmStatic
        fun push(key: String) {
            emit(key, MapboxScreenOperation.PUSH)
        }
    }
}
