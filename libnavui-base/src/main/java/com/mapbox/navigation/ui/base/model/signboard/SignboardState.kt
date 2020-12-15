package com.mapbox.navigation.ui.base.model.signboard

import com.mapbox.navigation.ui.base.MapboxState

/**
 * Immutable object representing the signboard data to be rendered.
 */
sealed class SignboardState : MapboxState {

    /**
     * The state is returned when the signboard is ready to be rendered on the UI
     * @property bytes contains the signboard
     */
    data class SignboardReady(val bytes: ByteArray) : SignboardState() {
        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SignboardReady

            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }

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
