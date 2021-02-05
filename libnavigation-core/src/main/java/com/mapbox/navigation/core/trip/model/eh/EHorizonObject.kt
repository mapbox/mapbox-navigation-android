package com.mapbox.navigation.core.trip.model.eh

class EHorizonObject internal constructor(
    val objectId: String,
    val objectLocation: EHorizonObjectLocation?,
    val objectMetadata: EHorizonObjectMetadata?,
    val objectEnterExitInfo: EHorizonObjectEnterExitInfo?,
) {

}
