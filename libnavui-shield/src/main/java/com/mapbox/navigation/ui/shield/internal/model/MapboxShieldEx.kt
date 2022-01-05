package com.mapbox.navigation.ui.shield.internal.model

import com.mapbox.api.directions.v5.models.MapboxShield

private const val MINIMUM_DISPLAY_REF_LENGTH = 2
private const val MAXIMUM_DISPLAY_REF_LENGTH = 6

fun MapboxShield.getRefLen(): Int {
    return when {
        this.displayRef().length <= 1 -> {
            MINIMUM_DISPLAY_REF_LENGTH
        }
        displayRef().length > 6 -> {
            MAXIMUM_DISPLAY_REF_LENGTH
        }
        else -> {
            displayRef().length
        }
    }
}
