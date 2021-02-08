package com.mapbox.navigation.ui.base.model.signboard

import com.mapbox.navigation.ui.base.MapboxState

/**
 * Immutable object representing the signboard data to be rendered.
 */
sealed class SignboardState : MapboxState {

    /**
     * The structure represents different state for a Signboard.
     */
    sealed class Signboard : SignboardState() {

        /**
         * The state is returned is their is no signboard in the route request or the
         * request to fetch signboard returns an empty data.
         */
        object Empty : Signboard()

        /**
         * The state is returned if there is an error preparing the signboard
         * @property exception String Error message.
         */
        data class Error(val exception: String?) : Signboard()

        /**
         * The state is returned when the signboard is ready to be rendered on the UI
         * @property desiredSignboardWidth used to calculate the height to maintain the aspect ratio.
         * If not specified it defaults to 400px.
         * @property bytes contains the signboard
         */
        data class Available(
            val desiredSignboardWidth: Int,
            val bytes: ByteArray
        ) : Signboard() {

            /**
             * @param bytes ByteArray data holding the signboard.
             * @constructor
             */
            constructor(bytes: ByteArray) : this(400, bytes)

            /**
             * Indicates whether some other object is "equal to" this one.
             * @param other Any?
             * @return Boolean
             */
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Available

                if (desiredSignboardWidth != other.desiredSignboardWidth) return false
                if (!bytes.contentEquals(other.bytes)) return false

                return true
            }

            /**
             * Returns a hash code value for the object.
             */
            override fun hashCode(): Int {
                var result = desiredSignboardWidth
                result = 31 * result + bytes.contentHashCode()
                return result
            }
        }
    }

    /**
     * The state is invoked to change the state of the view to show itself.
     */
    object Show : SignboardState()

    /**
     * The state is invoked to change the state of the view to hide itself.
     */
    object Hide : SignboardState()
}
