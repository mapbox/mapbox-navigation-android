package com.mapbox.navigation.ui.voice.api

interface AudioFocusDelegate {

    fun requestFocus(): Boolean

    fun abandonFocus()
}
