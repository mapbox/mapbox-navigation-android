package com.mapbox.navigation.ui.base.api.signboard

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions

/**
 * An Api that allows you to generate signboard based on [BannerInstructions]
 */
interface SignboardApi {

    /**
     * The method takes in [BannerInstructions] and generates a signboard based on the presence of
     * [BannerComponents] of type [BannerComponents.GUIDANCE_VIEW] and subType [BannerComponents.SIGNBOARD].
     * @param instructions object representing [BannerInstructions]
     * @param callback informs about the state of the signboard
     */
    fun generateSignboard(instructions: BannerInstructions, callback: SignboardReadyCallback)

    /**
     * Invoke the method to cancel all ongoing requests to generate a signboard.
     */
    fun cancelAll()
}
