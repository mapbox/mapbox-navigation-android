package com.mapbox.navigation.ui.base.model.signboard

import android.graphics.Bitmap
import com.mapbox.navigation.ui.base.MapboxState

sealed class SignboardState : MapboxState {

    /**
     * The state is returned when the Guidance Image is ready to be rendered on the UI
     * @property bitmap Bitmap The [Bitmap] containing the GuidanceImage
     */
    data class SignboardReady(val bitmap: Bitmap) : SignboardState()

    /**
     * The state is returned in case of any errors while preparing the GuidanceImage
     */
    sealed class SignboardFailure : SignboardState() {
        /**
         * The state is returned if the intersection doesn't contain GuidanceImage
         */
        object SignboardUnavailable : SignboardFailure()

        /**
         * The state is returned if the [Bitmap] is empty
         * @property exception String Error message.
         */
        data class SignboardEmpty(val exception: String?) : SignboardFailure()

        /**
         * The state is returned if there is an error preparing the [Bitmap]
         * @property exception String Error message.
         */
        data class SignboardError(val exception: String?) : SignboardFailure()
    }
}
