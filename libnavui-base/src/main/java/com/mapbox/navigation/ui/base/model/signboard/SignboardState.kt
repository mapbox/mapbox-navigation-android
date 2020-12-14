package com.mapbox.navigation.ui.base.model.signboard

import com.mapbox.navigation.ui.base.MapboxState
import java.io.InputStream

sealed class SignboardState : MapboxState {

    /**
     * The state is returned when the signboard is ready to be rendered on the UI
     * @property stream contains the signboard
     */
    data class SignboardReady(val bytes: ByteArray) : SignboardState()

    /**
     * The state is returned in case of any errors while preparing the GuidanceImage
     */
    sealed class SignboardFailure : SignboardState() {
        /**
         * The state is returned if the intersection doesn't contain signboard
         */
        object SignboardUnavailable : SignboardFailure()

        /**
         * The state is returned if there is an error preparing the signboard
         * @property exception String Error message.
         */
        data class SignboardError(val exception: String?) : SignboardFailure()
    }
}
