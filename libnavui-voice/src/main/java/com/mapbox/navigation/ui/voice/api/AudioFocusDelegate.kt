package com.mapbox.navigation.ui.voice.api

internal interface AudioFocusDelegate {

    fun requestFocus()

    fun abandonFocus()
}
