package com.mapbox.navigation.ui.utils.internal.lifecycle

import android.view.View
import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
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
) : LifecycleRegistry(localLifecycleOwner) {

    private var isAttached = false
    private var hostingLifecycleOwner: LifecycleOwner? = null

    private val hostingLifecycleObserver = LifecycleEventObserver { _, event ->
        val isAtLeastCreated = currentState.isAtLeast(State.CREATED)
        if (isAttached || (isAtLeastCreated && event == Event.ON_DESTROY)) {
            handleLifecycleEvent(event)
        }
    }

    private val attachStateChangeListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(p0: View) {
            doOnAttached(p0)
        }

        override fun onViewDetachedFromWindow(p0: View) {
            doOnDetached()
        }
    }

    init {
        view.addOnAttachStateChangeListener(attachStateChangeListener)
        if (view.isAttachedToWindow) {
            doOnAttached(view)
        }
    }

    private fun doOnAttached(view: View) {
        if (isAttached) return

        hostingLifecycleOwner?.lifecycle?.removeObserver(hostingLifecycleObserver)

        val hostingLifecycleOwner = ViewTreeLifecycleOwner.get(view)
            ?: throw IllegalStateException(
                "Please ensure that the hosting activity/fragment is a valid LifecycleOwner",
            )
        currentState = hostingLifecycleOwner.lifecycle.currentState
        hostingLifecycleOwner.lifecycle.addObserver(hostingLifecycleObserver)
        this@ViewLifecycleRegistry.hostingLifecycleOwner = hostingLifecycleOwner
        isAttached = true
    }

    private fun doOnDetached() {
        if (!isAttached) return

        isAttached = false
        val hostingLifecycleOwner: LifecycleOwner = checkNotNull(hostingLifecycleOwner)
        if (hostingLifecycleOwner.lifecycle.currentState.isAtLeast(State.CREATED)) {
            currentState = State.CREATED
        }
    }
}
