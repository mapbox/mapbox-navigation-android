package com.mapbox.navigation.ui.utils.internal.resource

import com.mapbox.common.NetworkRestriction
import com.mapbox.common.ResourceLoadFlags

/**
 * Resource Load Request defining resource [url], various [flags] that control resource loading
 * behavior and [networkRestriction] that controls which networks may be used to load the resource.
 */
class ResourceLoadRequest(
    val url: String,
) {
    /**
     * Various flags that control resource loading behavior.
     */
    var flags: ResourceLoadFlags = ResourceLoadFlags.NONE

    /**
     * Controls which networks may be used to load the resource.
     *
     * By default, all networks are allowed.
     */
    var networkRestriction: NetworkRestriction = NetworkRestriction.NONE
}
