package com.mapbox.navigation.ui.maps.guidance.signboard.model

/**
 * The state is returned when the signboard is ready to be rendered on the UI.
 * @property bytes contains the signboard.
 * @property options specifies properties of a signboard.
 */
class SignboardValue internal constructor(
    val bytes: ByteArray,
    val options: MapboxSignboardOptions
) {
    /**
     * @param bytes ByteArray data holding the signboard.
     * @constructor
     */
    internal constructor(bytes: ByteArray) : this(bytes, MapboxSignboardOptions.Builder().build())
}
