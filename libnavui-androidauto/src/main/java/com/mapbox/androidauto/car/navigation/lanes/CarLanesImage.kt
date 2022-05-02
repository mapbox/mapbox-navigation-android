package com.mapbox.androidauto.car.navigation.lanes

import androidx.car.app.model.CarIcon
import androidx.car.app.navigation.model.Lane

/**
 * Represents everything that the android auto library needs to show lane guidance.
 */
class CarLanesImage internal constructor(
    val lanes: List<Lane>,
    val carIcon: CarIcon
)
