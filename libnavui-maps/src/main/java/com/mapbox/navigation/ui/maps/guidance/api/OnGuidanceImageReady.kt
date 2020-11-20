package com.mapbox.navigation.ui.maps.guidance.api

import com.mapbox.navigation.ui.base.model.guidanceimage.GuidanceImageState

/**
 * Callback invoked by the API when state of GuidanceImage is modified.
 */
interface OnGuidanceImageReady {

    /**
     * Invoked when GuidanceImage is ready
     * @param bitmap GuidanceImagePrepared Represents GuidanceImage to be rendered on the view.
     */
    fun onGuidanceImagePrepared(bitmap: GuidanceImageState.GuidanceImagePrepared)

    /**
     * Invoked P
     * @param error GuidanceImageFailure error message.
     */
    fun onFailure(error: GuidanceImageState.GuidanceImageFailure)
}
