package com.mapbox.api.directions.v5.models

import com.google.flatbuffers.FlexBuffers
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.DirectionsCriteria.NotificationsRefreshTypeCriteria
import com.mapbox.api.directions.v5.DirectionsCriteria.NotificationsTypeCriteria
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.unhandledEnumMapping
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class NotificationFBWrapper private constructor(
    private val fb: FBNotification,
) : Notification(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun type(): String = fb.type.fbToNotificationsTypeCriteria(
        "type",
        unrecognizeFlexBufferMap,
    )

    override fun refreshType(): String = fb.refreshType.fbToNotificationsRefreshTypeCriteria(
        "refresh_type",
        unrecognizeFlexBufferMap,
    )

    override fun subtype(): String? = fb.subtype?.fbToNotificationsSubtypeCriteria(
        "subtype",
        unrecognizeFlexBufferMap,
    )

    override fun geometryIndexStart(): Int? = fb.geometryIndexStart

    override fun geometryIndex(): Int? = fb.geometryIndex

    override fun geometryIndexEnd(): Int? = fb.geometryIndexEnd

    override fun details(): NotificationDetails? {
        return NotificationDetailsFBWrapper.wrap(fb.details)
    }

    override fun reason(): String? = fb.reason

    override fun chargingStationId(): String? = fb.stationId

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Notification#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is NotificationFBWrapper && other.fb === fb) return true
        if (other is NotificationFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "Notification(" +
            "type=${type()}, " +
            "refreshType=${refreshType()}, " +
            "subtype=${subtype()}, " +
            "geometryIndexStart=${geometryIndexStart()}, " +
            "geometryIndex=${geometryIndex()}, " +
            "geometryIndexEnd=${geometryIndexEnd()}, " +
            "details=${details()}, " +
            "reason=${reason()}, " +
            "chargingStationId=${chargingStationId()}" +
            ")"
    }

    internal companion object {

        internal fun wrap(fb: FBNotification?): Notification? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> NotificationFBWrapper(fb)
            }
        }

        @DirectionsCriteria.NotificationsSubtypeCriteria
        private fun Byte.fbToNotificationsSubtypeCriteria(
            propertyName: String,
            unrecognized: FlexBuffers.Map?,
        ): String? {
            return when (this) {
                FBNotificationSubtype.MaxHeight ->
                    DirectionsCriteria.NOTIFICATION_SUBTYPE_MAX_HEIGHT
                FBNotificationSubtype.MaxWidth ->
                    DirectionsCriteria.NOTIFICATION_SUBTYPE_MAX_WIDTH
                FBNotificationSubtype.MaxWeight ->
                    DirectionsCriteria.NOTIFICATION_SUBTYPE_MAX_WEIGHT
                FBNotificationSubtype.Unpaved ->
                    DirectionsCriteria.NOTIFICATION_SUBTYPE_UNPAVED
                FBNotificationSubtype.PointExclusion ->
                    DirectionsCriteria.NOTIFICATION_SUBTYPE_POINT_EXCLUSION
                FBNotificationSubtype.CountryBorderCrossing ->
                    DirectionsCriteria.NOTIFICATION_SUBTYPE_COUNTRY_BORDER_CROSSING
                FBNotificationSubtype.StateBorderCrossing ->
                    DirectionsCriteria.NOTIFICATION_SUBTYPE_STATE_BORDER_CROSSING
                FBNotificationSubtype.EvMinChargeAtChargingStation ->
                    DirectionsCriteria.NOTIFICATION_SUBTYPE_EV_MIN_CHARGE_AT_CHARGING_STATION
                FBNotificationSubtype.EvMinChargeAtDestination ->
                    DirectionsCriteria.NOTIFICATION_SUBTYPE_EV_MIN_CHARGE_AT_DESTINATION
                FBNotificationSubtype.Tunnel -> DirectionsCriteria.NOTIFICATION_SUBTYPE_TUNNEL
                FBNotificationSubtype.EvInsufficientCharge ->
                    DirectionsCriteria.NOTIFICATION_SUBTYPE_EV_INSUFFICIENT_CHARGE
                FBNotificationSubtype.EvStationUnavailable ->
                    DirectionsCriteria.NOTIFICATION_SUBTYPE_EV_STATION_UNAVAILABLE
                FBNotificationSubtype.Unknown -> unrecognized?.get(propertyName)?.asString()
                else -> unhandledEnumMapping(propertyName, this)
            }
        }

        @NotificationsRefreshTypeCriteria
        private fun Byte.fbToNotificationsRefreshTypeCriteria(
            propertyName: String,
            unrecognized: FlexBuffers.Map?,
        ): String {
            return when (this) {
                FBNotificationRefreshType.Static ->
                    DirectionsCriteria.NOTIFICATION_REFRESH_TYPE_STATIC
                FBNotificationRefreshType.Dynamic ->
                    DirectionsCriteria.NOTIFICATION_REFRESH_TYPE_DYNAMIC
                FBNotificationRefreshType.Unknown -> unrecognized?.get(propertyName)?.asString()
                    ?: throw IllegalStateException(
                        "$propertyName is Unknown in fb, but missing in unrecognized map",
                    )
                else -> unhandledEnumMapping(propertyName, this)
            }
        }

        @NotificationsTypeCriteria
        private fun Byte.fbToNotificationsTypeCriteria(
            propertyName: String,
            unrecognized: FlexBuffers.Map?,
        ): String {
            return when (this) {
                FBNotificationType.Violation -> DirectionsCriteria.NOTIFICATION_TYPE_VIOLATION
                FBNotificationType.Alert -> DirectionsCriteria.NOTIFICATION_TYPE_ALERT
                FBNotificationType.Unknown -> unrecognized?.get(propertyName)?.asString()
                    ?: throw IllegalStateException(
                        "$propertyName is Unknown in fb, but missing in unrecognized map",
                    )
                else -> unhandledEnumMapping(propertyName, this)
            }
        }
    }
}
