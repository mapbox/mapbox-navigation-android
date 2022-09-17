package com.mapbox.navigation.dropin.internal.extensions

import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.view.updateLayoutParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.actor

/**
 * OnClick extension function that uses the actor coroutine to control how the [handler] processes
 * the OnClick events. When a receiving view is clicked, the event is sent to the actor immediately,
 * if it is possible, or discards it otherwise. This happens because the actor is busy with
 * a [handler] coroutine and does not receive from its channel.
 *
 * @param scope a CoroutineScope in which the actor coroutine should launch
 * @param handler the coroutine code which will be invoked when the receiver view is clicked
 */
fun View.onClick(scope: CoroutineScope, handler: suspend CoroutineScope.(View) -> Unit) {
    // launch one actor
    val eventActor = scope.actor<View>(Dispatchers.Main) {
        for (event in channel) handler(event)
    }
    // install a listener to activate this actor
    setOnClickListener {
        // By default, an actor's mailbox is backed by RendezvousChannel,
        // whose trySend operation succeeds only when the receive is active.
        eventActor.trySend(it)
    }
}

/**
 * Updates this view's margins by modifying its layoutParams.
 * This method version allows using named parameters just to set one or more axes.
 *
 * @see ViewGroup.MarginLayoutParams.setMargins
 */
fun View.updateMargins(
    @Px left: Int? = null,
    @Px top: Int? = null,
    @Px right: Int? = null,
    @Px bottom: Int? = null
) {
    updateLayoutParams<ViewGroup.MarginLayoutParams> {
        setMargins(
            left ?: leftMargin,
            top ?: topMargin,
            right ?: rightMargin,
            bottom ?: bottomMargin
        )
    }
}
