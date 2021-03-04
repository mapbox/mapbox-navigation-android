package com.mapbox.navigation.core.trip.model.eh

import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossingInfo
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentInfo
import com.mapbox.navigation.base.trip.model.roadobject.tunnel.TunnelInfo

/**
 * RoadObject metadata
 *
 * @param type type of road object
 * @param objectProvider provider of road object
 * @param incidentInfo will be filled only if [type] is [EHorizonObjectType.INCIDENT] and
 * [objectProvider] is [EHorizonObjectProvider.MAPBOX]
 * @param tunnelInfo will be filled only if [type] is [EHorizonObjectType.TUNNEL_ENTRANCE] and
 * [objectProvider] is [EHorizonObjectProvider.MAPBOX]
 * @param borderCrossingInfo will be filled only if [type] is [EHorizonObjectType.BORDER_CROSSING]
 * and [objectProvider] is [EHorizonObjectProvider.MAPBOX]
 * @param tollCollectionType will be filled only if [type] is
 * [EHorizonObjectType.TOLL_COLLECTION_POINT] and [objectProvider] is
 * [EHorizonObjectProvider.MAPBOX]
 * @param restStopType will be filled only if [type] is [EHorizonObjectType.SERVICE_AREA] and
 * [objectProvider] is [EHorizonObjectProvider.MAPBOX]
 */
class EHorizonObjectMetadata internal constructor(
    @RoadObjectType.Type val type: Int,
    @EHorizonObjectProvider.Type val objectProvider: String,
    val incidentInfo: IncidentInfo?,
    val tunnelInfo: TunnelInfo?,
    val borderCrossingInfo: CountryBorderCrossingInfo?,
    val tollCollectionType: Int?,
    val restStopType: Int?,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonObjectMetadata

        if (type != other.type) return false
        if (objectProvider != other.objectProvider) return false
        if (incidentInfo != other.incidentInfo) return false
        if (tunnelInfo != other.tunnelInfo) return false
        if (borderCrossingInfo != other.borderCrossingInfo) return false
        if (tollCollectionType != other.tollCollectionType) return false
        if (restStopType != other.restStopType) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + objectProvider.hashCode()
        result = 31 * result + (incidentInfo?.hashCode() ?: 0)
        result = 31 * result + (tunnelInfo?.hashCode() ?: 0)
        result = 31 * result + (borderCrossingInfo?.hashCode() ?: 0)
        result = 31 * result + (tollCollectionType ?: 0)
        result = 31 * result + (restStopType ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonObjectMetadata(" +
            "type='$type', " +
            "objectProvider='$objectProvider', " +
            "incidentInfo=$incidentInfo, " +
            "tunnelInfo=$tunnelInfo, " +
            "borderCrossingInfo=$borderCrossingInfo, " +
            "tollCollectionType=$tollCollectionType, " +
            "restStopType=$restStopType)"
    }
}
