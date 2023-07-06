package com.mapbox.navigation.ui.voice.options

import androidx.annotation.StringDef

/**
 * Holds available [VoiceGender] types.
 *
 * Available values are:
 * - [VoiceGender.MALE]
 * - [VoiceGender.FEMALE]
 */
object VoiceGender {

    /**
     * Constant for a male voice.
     */
    const val MALE = "male"

    /**
     * Constant for a female voice.
     */
    const val FEMALE = "female"

    /**
     * Voice gender type.
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        MALE,
        FEMALE
    )
    annotation class Type
}
