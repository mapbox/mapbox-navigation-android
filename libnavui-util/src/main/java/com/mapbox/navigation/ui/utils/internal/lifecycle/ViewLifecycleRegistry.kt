package com.mapbox.navigation.ui.utils.internal.lifecycle

import android.view.View
import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.Flow

/**
 * A [LifecycleRegistry] that merges the [hostingLifecycleOwner]'s [Lifecycle] (of an Activity or Fragment)
 * with the [View]'s drawing surface lifecycle.
 *
 * This lifecycle is useful for continuously running tasks that obtain the data and draw it in the [View],
 * for example by collecting [Flow]s and drawing results in the nested views or on the map.
 *
 * In general, this lifecycle reflects the [hostingLifecycleOwner]'s [Lifecycle] whenever the `View` is attached
 * but whenever the `View` is detached and we want to stop the coroutines tied to the lifecycle's scope,
 * this lifecycle will transition to [Lifecycle.State.CREATED] (and invoke [Lifecycle.Event.ON_STOP])
 * even if the hosting Activity or Fragment is still running.
 *
 * This lifecycle reaches [Lifecycle.State.DESTROYED] only when the [hostingLifecycleOwner] is destroyed.
 *
 * @see keepExecutingWhenStarted
 */
@UiThread
class ViewLifecycleRegistry(
    view: View,
    localLifecycleOwner: LifecycleOwner,
    hostingLifecycleOwner: LifecycleOwner,
) : LifecycleRegistry(localLifecycleOwner) {

    private var isAttached = view.isAttachedToWindow

    private val hostingLifecycleObserver = LifecycleEventObserver { _, event ->
        val isAtLeastCreated = currentState.isAtLeast(State.CREATED)
        if (isAttached || (isAtLeastCreated && event == Event.ON_DESTROY)) {
            handleLifecycleEvent(event)
        }
    }

    private val attachStateChangeListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(p0: View?) {
            logD("onViewAttachedToWindow ${p0.hashCode()}", "lifecycle_debug")
            currentState = hostingLifecycleOwner.lifecycle.currentState
            isAttached = true
        }

        override fun onViewDetachedFromWindow(p0: View?) {
            logD("onViewDetachedFromWindow ${p0.hashCode()}", "lifecycle_debug")
            isAttached = false
            if (hostingLifecycleOwner.lifecycle.currentState.isAtLeast(State.STARTED)) {
                currentState = State.CREATED
            }
        }
    }

    init {
        hostingLifecycleOwner.lifecycle.addObserver(hostingLifecycleObserver)
        view.addOnAttachStateChangeListener(attachStateChangeListener)
    }
}

/**
 * Launches a coroutine whenever this lifecycle reaches [Lifecycle.State.STARTED] or higher.
 * If the lifecycle state triggers [Lifecycle.Event.ON_STOP] or beyond, the launched coroutines are canceled.
 *
 * If the state reaches [Lifecycle.State.STARTED] again, the coroutines are restarted.
 *
 * This is useful when paired with [ViewLifecycleRegistry] because the coroutines will keep being canceled and restarted
 * as the target [View] is detached and re-attached to window.
 *
 * This function uses [repeatOnLifecycle] under the hood.
 */
fun LifecycleOwner.keepExecutingWhenStarted(block: suspend CoroutineScope.() -> Unit) =
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED, block)
    }
