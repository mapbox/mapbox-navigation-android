package com.mapbox.navigation.base.trip.model.alert

/**
 * Holds available [RouteAlert] types.
 *
 * Available values are:
 * - [RouteAlertType.TunnelEntrance]
 * - [RouteAlertType.CountryBorderCrossing]
 * - [RouteAlertType.TollCollection]
 * - [RouteAlertType.RestStop]
 * - [RouteAlertType.RestrictedArea]
 * - [RouteAlertType.Incident]
 */
object RouteAlertType {

    /**
     * Type of the [TunnelEntranceAlert].
     */
    const val TunnelEntrance = 0

    /**
     * Type of the [CountryBorderCrossingAlert].
     */
    const val CountryBorderCrossing = 1

    /**
     * Type of the [TollCollectionAlert].
     */
    const val TollCollection = 2

    /**
     * Type of the [RestStopAlert].
     */
    const val RestStop = 3

    /**
     * Type of the [RestrictedAreaAlert].
     */
    const val RestrictedArea = 4

    /**
     * Type of the [IncidentAlert].
     */
    const val Incident = 5
}
