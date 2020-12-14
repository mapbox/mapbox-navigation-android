package com.mapbox.navigation.ui.maps.signboard.internal

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.ui.base.MapboxProcessor
import com.mapbox.navigation.ui.base.domain.BannerInstructionsApi
import com.mapbox.navigation.ui.utils.internal.ifNonNull

object SignboardProcessor : MapboxProcessor<SignboardAction, SignboardResult>,
    BannerInstructionsApi {

    override fun process(action: SignboardAction): SignboardResult {
        return when (action) {
            is SignboardAction.CheckSignboardAvailability -> {
                isSignboardAvailable(action.instructions)
            }
        }
    }

    private fun isSignboardAvailable(instruction: BannerInstructions): SignboardResult {
        return ifNonNull(getSignboardUrl(instruction)) { signboardUrl ->
            SignboardResult.SignboardAvailable(signboardUrl)
        } ?: SignboardResult.SignboardUnavailable
    }

    private fun getSignboardUrl(
        bannerInstructions: BannerInstructions
    ): String? {
        val bannerComponents = getBannerComponents(bannerInstructions)
        return when {
            bannerComponents != null -> {
                findSignboardComponent(bannerComponents)
            }
            else -> {
                null
            }
        }
    }

    private fun findSignboardComponent(
        componentList: MutableList<BannerComponents>
    ): String? {
        val component = componentList.find {
            it.type() == BannerComponents.GUIDANCE_VIEW &&
                it.subType() == BannerComponents.SIGNBOARD
        }
        return ifNonNull(component?.imageUrl()) {
            it
        }
    }
}
