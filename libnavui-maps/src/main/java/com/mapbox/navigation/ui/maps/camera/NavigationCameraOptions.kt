package com.mapbox.navigation.ui.maps.camera

import com.mapbox.maps.EdgeInsets

const val MAPBOX_CAMERA_OPTION_FOLLOWING_PITCH = 40.0
const val MAPBOX_CAMERA_OPTION_MAX_ZOOM = 17.0
const val MAPBOX_CAMERA_OPTION_EDGE_INSET = 20.0

class NavigationCameraOptions private constructor(val followingPitch: Double,
                                                  val maxZoom: Double,
                                                  val edgeInsets: EdgeInsets) {
    fun toBuilder(): Builder = Builder().apply {
        followingPitch(followingPitch)
        maxZoom(maxZoom)
        edgeInsets(edgeInsets)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationCameraOptions

        if (followingPitch != other.followingPitch) return false
        if (maxZoom != other.maxZoom) return false
        if (edgeInsets != other.edgeInsets) return false

        return true
    }

    override fun hashCode(): Int {
        var result = followingPitch.hashCode()
        result = 31 * result + maxZoom.hashCode()
        result = 31 * result + edgeInsets.hashCode()
        return result
    }

    class Builder {
        private var followingPitch = MAPBOX_CAMERA_OPTION_FOLLOWING_PITCH
        private var maxZoom = MAPBOX_CAMERA_OPTION_MAX_ZOOM
        private var edgeInsets = EdgeInsets(MAPBOX_CAMERA_OPTION_EDGE_INSET,
            MAPBOX_CAMERA_OPTION_EDGE_INSET,
            MAPBOX_CAMERA_OPTION_EDGE_INSET,
            MAPBOX_CAMERA_OPTION_EDGE_INSET)

        fun followingPitch(followingPitch: Double): Builder = apply {
            this.followingPitch = followingPitch
        }

        fun maxZoom(maxZoom: Double): Builder = apply {
            this.maxZoom = maxZoom
        }

        fun edgeInsets(edgeInsets: EdgeInsets): Builder = apply {
            this.edgeInsets = edgeInsets
        }

        fun build(): NavigationCameraOptions = NavigationCameraOptions(
            followingPitch,
            maxZoom,
            edgeInsets
        )
    }
}
