package com.mapbox.navigation.examples.core.camera

enum class AnimationType(val text: String? = null) {
    Following,
    Overview,
    ToPOI,
    LookAtPOIWhenFollowing("Look at POI")
}
