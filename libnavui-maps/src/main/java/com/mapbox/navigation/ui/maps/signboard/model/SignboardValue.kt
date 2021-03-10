package com.mapbox.navigation.ui.maps.signboard.model

/**
 * The state is returned when the signboard is ready to be rendered on the UI
 * @property desiredSignboardWidth used to calculate the height to maintain the aspect ratio.
 * If not specified it defaults to 400px.
 * @property bytes contains the signboard
 */
class SignboardValue internal constructor(
    val desiredSignboardWidth: Int,
    val bytes: ByteArray
) {
    /**
     * @param bytes ByteArray data holding the signboard.
     * @constructor
     */
    internal constructor(bytes: ByteArray) : this(400, bytes)
}
