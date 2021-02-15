package com.mapbox.navigation.core.trip.model.eh

import androidx.annotation.StringDef
import com.mapbox.navigation.core.trip.session.RoadObjectsStore

/**
 * RoadObject type
 */
object EHorizonObjectType {

    /**
     * Road object represents some road incident
     */
    const val INCIDENT = "INCIDENT"

    /**
     * Road object represents some toll collection point
     */
    const val TOLL_COLLECTION_POINT = "TOLL_COLLECTION_POINT"

    /**
     * Road object represents some border crossing
     */
    const val BORDER_CROSSING = "BORDER_CROSSING"

    /**
     * Road object represents some tunnel entrance
     */
    const val TUNNEL_ENTRANCE = "TUNNEL_ENTRANCE"

    /**
     * Road object represents some tunnel exit
     */
    const val TUNNEL_EXIT = "TUNNEL_EXIT"

    /**
     * Road object represents some restricted area entrance
     */
    const val RESTRICTED_AREA_ENTRANCE = "RESTRICTED_AREA_ENTRANCE"

    /**
     * Road object represents some restricted area exit
     */
    const val RESTRICTED_AREA_EXIT = "RESTRICTED_AREA_EXIT"

    /**
     * Road object represents some service area
     */
    const val SERVICE_AREA = "SERVICE_AREA"

    /**
     * Road object represents some bridge entrance
     */
    const val BRIDGE_ENTRANCE = "BRIDGE_ENTRANCE"

    /**
     * Road object represents some bridge exit
     */
    const val BRIDGE_EXIT = "BRIDGE_EXIT"

    /**
     * Road object was added by user
     * (via [RoadObjectsStore.addCustomRoadObject])
     */
    const val CUSTOM = "CUSTOM"

    /**
     * Retention policy for the EHorizonObjectType
     */
    @Retention
    @StringDef(
        INCIDENT,
        TOLL_COLLECTION_POINT,
        BORDER_CROSSING,
        TUNNEL_ENTRANCE,
        TUNNEL_EXIT,
        RESTRICTED_AREA_ENTRANCE,
        RESTRICTED_AREA_EXIT,
        SERVICE_AREA,
        BRIDGE_ENTRANCE,
        BRIDGE_EXIT,
        CUSTOM
    )
    annotation class Type
}
