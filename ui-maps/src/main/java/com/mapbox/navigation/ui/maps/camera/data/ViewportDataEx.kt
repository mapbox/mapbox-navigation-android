package com.mapbox.navigation.ui.maps.camera.data

internal fun ViewportData.isStandstill(other: ViewportData) =
    this.cameraForFollowing.isStandstill(other.cameraForFollowing) &&
        this.cameraForOverview.isStandstill(other.cameraForOverview)
