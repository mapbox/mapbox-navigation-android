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

internal fun Context.toViewModelStoreOwner(): ViewModelStoreOwner =
    checkNotNull(this.recursiveUnwrap() as? ViewModelStoreOwner) {
        "Please ensure that the hosting Context is a valid ViewModelStoreOwner"
    }

internal fun Context.toComponentActivity(): ComponentActivity =
    checkNotNull(this.recursiveUnwrap() as? ComponentActivity) {
        "Please ensure that the hosting Context is a valid ComponentActivity"
    }
