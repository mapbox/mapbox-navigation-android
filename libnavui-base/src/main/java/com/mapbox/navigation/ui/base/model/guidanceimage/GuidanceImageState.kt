package com.mapbox.navigation.ui.base.model.guidanceimage

import android.graphics.Bitmap
import com.mapbox.navigation.ui.base.MapboxState

sealed class GuidanceImageState : MapboxState {
    /**
     * The state is returned when the Guidance Image is ready to be rendered on the UI
     * @property bitmap Bitmap The [Bitmap] containing the GuidanceImage
     */
    data class GuidanceImagePrepared(val bitmap: Bitmap) : GuidanceImageState()

    /**
     * The state is returned in case of any errors while preparing the GuidanceImage
     */
    sealed class GuidanceImageFailure : GuidanceImageState() {
        /**
         * The state is returned if the intersection doesn't contain GuidanceImage
         */
        object GuidanceImageUnavailable : GuidanceImageFailure()

        /**
         * The state is returned if the [Bitmap] is empty
         * @property exception String Error message.
         */
        data class GuidanceImageEmpty(val exception: String?) : GuidanceImageFailure()

        /**
         * The state is returned if there is an error preparing the [Bitmap]
         * @property exception String Error message.
         */
        data class GuidanceImageError(val exception: String?) : GuidanceImageFailure()
    }
}
