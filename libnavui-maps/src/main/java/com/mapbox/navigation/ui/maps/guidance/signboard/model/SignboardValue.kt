package com.mapbox.navigation.ui.maps.guidance.signboard.model

import android.graphics.Bitmap

/**
 * The state is returned when the signboard is ready to be rendered on the UI.
 * @property bitmap contains the signboard.
 */
class SignboardValue internal constructor(
    val bitmap: Bitmap,
)
