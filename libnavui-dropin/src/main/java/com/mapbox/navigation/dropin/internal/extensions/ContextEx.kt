@file:JvmName("ContextEx")

package com.mapbox.navigation.dropin.internal.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.mapbox.navigation.utils.internal.logW
import java.lang.ref.WeakReference

internal tailrec fun Context.recursiveUnwrap(): Context =
    if (this !is Activity && this is ContextWrapper) {
        this.baseContext.recursiveUnwrap()
    } else {
        this
    }

internal fun Context.toLifecycleOwner(): LifecycleOwner {
    val lifecycleOwner = this.recursiveUnwrap() as? LifecycleOwner
    checkNotNull(lifecycleOwner) {
        "Please ensure that the hosting Context is a valid LifecycleOwner"
    }
    return lifecycleOwner
}

internal fun Context.toViewModelStoreOwner(): ViewModelStoreOwner {
    val viewModelStoreOwner = this.recursiveUnwrap() as? ViewModelStoreOwner
    checkNotNull(viewModelStoreOwner) {
        "Please ensure that the hosting Context is a valid ViewModelStoreOwner"
    }
    return viewModelStoreOwner
}

internal fun Context.toComponentActivityRef(): WeakReference<ComponentActivity>? {
    val componentActivity = this.recursiveUnwrap() as? ComponentActivity
    if (componentActivity == null) {
        logW("Unable to find ComponentActivity to request location permissions")
    }
    return componentActivity?.let { WeakReference(it) }
}
