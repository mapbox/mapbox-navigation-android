package com.mapbox.navigation.ui.maps.recenterbutton

internal sealed class RecenterButtonResult {

    object RecenterButtonVisible: RecenterButtonResult()

    object RecenterButtonInvisible: RecenterButtonResult()

    object OnRecenterButtonClicked: RecenterButtonResult()
}
