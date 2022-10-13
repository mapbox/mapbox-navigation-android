@file:JvmName("ContextEx")

package com.mapbox.navigation.dropin.internal.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelStoreOwner

internal tailrec fun Context.recursiveUnwrap(): Context =
    if (this !is Activity && this is ContextWrapper) {
        this.baseContext.recursiveUnwrap()
    } else {
        this
    }

internal fun Context.toViewModelStoreOwner(): ViewModelStoreOwner {
    val viewModelStoreOwner = this.recursiveUnwrap() as? ViewModelStoreOwner
    checkNotNull(viewModelStoreOwner) {
        "Please ensure that the hosting Context is a valid ViewModelStoreOwner"
    }
    return viewModelStoreOwner
}

internal fun Context.toComponentActivity(): ComponentActivity {
    val componentActivity = this.recursiveUnwrap() as? ComponentActivity
    checkNotNull(componentActivity) {
        "Please ensure that the hosting Context is a valid ComponentActivity"
    }
    return componentActivity
}
