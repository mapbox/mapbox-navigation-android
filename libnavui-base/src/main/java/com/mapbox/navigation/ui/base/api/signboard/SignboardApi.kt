package com.mapbox.navigation.ui.base.api.signboard

import com.mapbox.api.directions.v5.models.BannerInstructions

interface SignboardApi {

    fun generateSignboard(instructions: BannerInstructions)

    fun cancelGeneration()
}
