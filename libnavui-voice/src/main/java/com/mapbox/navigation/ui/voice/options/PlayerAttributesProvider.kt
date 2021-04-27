package com.mapbox.navigation.ui.voice.options

import android.os.Build

internal object PlayerAttributesProvider {

    fun retrievePlayerAttributes(): PlayerAttributes {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PlayerAttributes.OreoAndLaterAttributes()
        } else PlayerAttributes.PreOreoAttributes()
    }
}
