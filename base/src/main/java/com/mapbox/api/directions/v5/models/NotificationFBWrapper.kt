package com.mapbox.api.directions.v5.models

import com.google.flatbuffers.FlexBuffers
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.DirectionsCriteria.NotificationsRefreshTypeCriteria
import com.mapbox.api.directions.v5.DirectionsCriteria.NotificationsTypeCriteria
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbNotification
import com.mapbox.directions.generated.NotificationRefreshType
import com.mapbox.directions.generated.NotificationType
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class NotificationFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbNotification,
) : Notification(), BaseFBWrapper {

    override val platformAddedUnrecognizedProperties: Map<String, Any?>?
        get() {
            val result = mutableMapOf<String, Any?>()
            if (fb.initialTempOrNull(b) != null) {
                result["initial_temp"] = fb.initialTempOrNull(b)
            }
            if (fb.finalTempOrNull(b) != null) {
                result["final_temp"] = fb.finalTempOrNull(b)
            }
            return result.ifEmpty {
                null
            }
        }

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun type(): String = FBNotificationType.fromByteOrThrow(fb.type(b)).fbToNotificationsTypeCriteria(
        "type",
        unrecognizeFlexBufferMap,
    )

    override fun refreshType(): String = FBNotificationRefreshType
        .fromByteOrThrow(fb.refreshType(b))
        .fbToNotificationsRefreshTypeCriteria("refresh_type", unrecognizeFlexBufferMap)

    override fun subtype(): String? = fb.subtypeOrNull(b)?.fbToNotificationsSubtypeCriteria(
        "subtype",
        unrecognizeFlexBufferMap,
    )

    override fun geometryIndexStart(): Int? = fb.geometryIndexStartOrNull(b)

    override fun geometryIndex(): Int? = fb.geometryIndexOrNull(b)

    override fun geometryIndexEnd(): Int? = fb.geometryIndexEndOrNull(b)

    override fun details(): NotificationDetails? {
        return NotificationDetailsFBWrapper.wrap(b, fb.details(b))
    }

    override fun reason(): String? = fb.reason(b)

    override fun chargingStationId(): String? = fb.stationId(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Notification#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is Notification && other !is NotificationFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is NotificationFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

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
            "chargingStationId=${chargingStationId()}, " +
            "unrecognized=${unrecognized()}" +
            ")"
    }

    internal companion object {

        internal fun wrap(b: FbBuffer, fb: FbNotification): Notification? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> NotificationFBWrapper(b, fb)
            }
        }

        @DirectionsCriteria.NotificationsSubtypeCriteria
        private fun Byte.fbToNotificationsSubtypeCriteria(
            propertyName: String,
            unrecognized: FlexBuffers.Map?,
        ): String? {
            return when (FBNotificationSubtype.fromByteOrThrow(this)) {
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
                // battery_preconditioning_range is not stable yet.
                // It is not currently available in the backend or in mapbox-java.
                FBNotificationSubtype.BatteryPreconditioningRange ->
                    "battery_preconditioning_range"
                FBNotificationSubtype.UnnecessaryChargingAtNextUPCS ->
                    "unnecessaryChargingAtNextUPCS"
                FBNotificationSubtype.UnnecessaryChargingAtNextCPOI ->
                    "unnecessaryChargingAtNextCPOI"
                FBNotificationSubtype.Vignette ->
                    "vignette"
                FBNotificationSubtype.BetterStationsPossible ->
                    "betterStationsPossible"
                FBNotificationSubtype.Unknown -> unrecognized?.get(propertyName)?.asString()
            }
        }

        @NotificationsRefreshTypeCriteria
        private fun NotificationRefreshType.fbToNotificationsRefreshTypeCriteria(
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
            }
        }

        @NotificationsTypeCriteria
        private fun NotificationType.fbToNotificationsTypeCriteria(
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
            }
        }
    }
}
