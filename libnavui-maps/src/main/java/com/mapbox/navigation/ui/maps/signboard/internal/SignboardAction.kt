package com.mapbox.navigation.ui.maps.signboard.internal

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.ui.base.MapboxAction

sealed class SignboardAction : MapboxAction {

    data class CheckSignboardAvailability(
        val instructions: BannerInstructions
    ) : SignboardAction()
}
