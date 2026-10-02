package com.mapbox.navigation.ui.androidauto.notification

import android.content.Context
import androidx.car.app.notification.CarAppExtender
import com.mapbox.navigation.ui.androidauto.R

internal class IdleExtenderUpdater(private val context: Context) {

    fun update(extenderBuilder: CarAppExtender.Builder) {
        extenderBuilder.setContentTitle(context.getString(R.string.mapbox_navigation_is_starting))
    }
}
