package com.mapbox.navigation.trip.notification

import android.widget.RemoteViews
import androidx.annotation.LayoutRes

internal object RemoteViewsProvider {

    fun createRemoteViews(packageName: String, @LayoutRes layoutId: Int): RemoteViews {
        return RemoteViews(packageName, layoutId)
    }
}
