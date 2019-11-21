package com.mapbox.services.android.navigation.testapp.utils

import android.app.Activity
import android.view.View
import androidx.annotation.IdRes

fun <ViewT : View> Activity.bindView(@IdRes idRes: Int): Lazy<ViewT> {
    return lazyUnsychronized {
        findViewById(idRes)
    }
}
