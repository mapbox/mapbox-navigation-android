package com.mapbox.navigation.ui.androidauto.notification

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.notification.CarPendingIntent

/**
 * Created for unit tests.
 */
internal object CarPendingIntentFactory {
    fun create(
        carContext: CarContext,
        carStartAppClass: Class<out CarAppService>,
    ): PendingIntent {
        return CarPendingIntent.getCarApp(
            carContext,
            0,
            Intent(Intent.ACTION_VIEW).setComponent(
                ComponentName(carContext, carStartAppClass),
            ),
            PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
