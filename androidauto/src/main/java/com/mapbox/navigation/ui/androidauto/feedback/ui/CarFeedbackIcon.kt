package com.mapbox.navigation.ui.androidauto.feedback.ui

import android.net.Uri
import androidx.car.app.model.CarIcon

/**
 * Represents an icon of a feedback category
 */
sealed class CarFeedbackIcon {

    data class Local(val icon: CarIcon) : CarFeedbackIcon()

    data class Remote(val uri: Uri) : CarFeedbackIcon()
}
