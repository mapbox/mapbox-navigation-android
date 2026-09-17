package com.mapbox.navigation.ui.androidauto.internal

import androidx.annotation.DrawableRes
import androidx.car.app.CarContext
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackIcon

internal fun CarContext.getCarIcon(@DrawableRes iconId: Int): CarFeedbackIcon {
    val icon = IconCompat.createWithResource(this, iconId)
    return CarFeedbackIcon.Local(CarIcon.Builder(icon).build())
}
