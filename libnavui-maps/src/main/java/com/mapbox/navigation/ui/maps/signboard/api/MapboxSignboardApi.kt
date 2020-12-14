package com.mapbox.navigation.ui.maps.signboard.api

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.ui.base.api.signboard.SignboardApi
import com.mapbox.navigation.ui.base.model.signboard.SignboardState
import com.mapbox.navigation.ui.maps.internal.FetchImageProcessor
import com.mapbox.navigation.ui.maps.internal.ImageResponse
import com.mapbox.navigation.ui.maps.signboard.internal.SignboardAction
import com.mapbox.navigation.ui.maps.signboard.internal.SignboardProcessor
import com.mapbox.navigation.ui.maps.signboard.internal.SignboardResult
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch

class MapboxSignboardApi(
    private val accessToken: String,
    private val onSignboardCallback: SignboardReadyCallback
) : SignboardApi {

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }

    override fun generateSignboard(instructions: BannerInstructions) {
        val result =
            SignboardProcessor.process(SignboardAction.CheckSignboardAvailability(instructions))
        when (result) {
            is SignboardResult.SignboardUnavailable -> {
                onSignboardCallback.onFailure(SignboardState.SignboardFailure.SignboardUnavailable)
            }
            is SignboardResult.SignboardAvailable -> {
                mainJobController.scope.launch {
                    when (val response =
                        FetchImageProcessor.fetchImage(
                            result.signboardUrl.plus("?access_token=$accessToken"))
                        ) {
                        is ImageResponse.Success -> {
                            onSignboardCallback.onSignboardReady(
                                SignboardState.SignboardReady(
                                    response.bytes
                                )
                            )
                        }
                        is ImageResponse.Failure -> {
                            onSignboardCallback.onFailure(
                                SignboardState.SignboardFailure.SignboardError(
                                    response.error
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun cancelGeneration() {
        mainJobController.job.cancel()
    }
}
