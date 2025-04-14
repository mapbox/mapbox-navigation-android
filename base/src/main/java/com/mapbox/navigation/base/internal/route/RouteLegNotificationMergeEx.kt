package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.internal.utils.Constants.NotificationSubtype.EV_NOTIFICATIONS_SUB_TYPES
import com.mapbox.navigation.base.internal.utils.Constants.RouteResponse.KEY_NOTIFICATIONS

internal fun RouteLeg.Builder.updateNotifications(
    oldUnrecognizedProperties: Map<String, JsonElement>?,
    newNotifications: JsonArray?,
    isElectric: Boolean,
): RouteLeg.Builder {
    // Currently backend supports only EV Notifications refresh
    // https://github.com/mapbox/api-valhalla/blob/913aee076e4da6ee713d0b2eef56141cad1723b7/src/handlers/refresh/handleRefresh.ts#L328-L338
    if (!isElectric) {
        return this
    }

    val oldNotifications = oldUnrecognizedProperties?.get(KEY_NOTIFICATIONS) as? JsonArray
    return when {
        newNotifications == null && oldNotifications != null ->
            cleanupOldEvNotifications(oldNotifications, oldUnrecognizedProperties)
        newNotifications != null && oldNotifications == null ->
            updateWithNewNotifications(newNotifications, oldUnrecognizedProperties)
        newNotifications != null && oldNotifications != null ->
            filterAndMergeNotifications(
                newNotifications,
                oldNotifications,
                oldUnrecognizedProperties,
            )
        else ->
            this
    }
}

private fun RouteLeg.Builder.cleanupOldEvNotifications(
    oldNotifications: JsonArray,
    oldUnrecognizedProperties: Map<String, JsonElement>?,
): RouteLeg.Builder {
    val updatedNotifications = JsonArray()
    oldNotifications.forEach {
        if (!it.isEvNotificationSubtype) updatedNotifications.add(it)
    }
    return unrecognizedJsonProperties(
        oldUnrecognizedProperties.orEmpty().toMutableMap().also {
            if (updatedNotifications.isEmpty) {
                it.remove(KEY_NOTIFICATIONS)
            } else {
                it[KEY_NOTIFICATIONS] = updatedNotifications
            }
        },
    )
}

private fun RouteLeg.Builder.updateWithNewNotifications(
    newNotifications: JsonArray,
    oldUnrecognizedProperties: Map<String, JsonElement>?,
): RouteLeg.Builder =
    apply(newNotifications, oldUnrecognizedProperties)

private fun RouteLeg.Builder.filterAndMergeNotifications(
    newNotifications: JsonArray,
    oldNotifications: JsonArray,
    oldUnrecognizedProperties: Map<String, JsonElement>?,
): RouteLeg.Builder {
    val merged = JsonArray()
    oldNotifications.forEach { if (!it.isEvNotificationSubtype) merged.add(it) }
    newNotifications.forEach { if (it.isEvNotificationSubtype) merged.add(it) }
    return apply(merged, oldUnrecognizedProperties)
}

private fun RouteLeg.Builder.apply(
    notifications: JsonArray,
    oldUnrecognizedProperties: Map<String, JsonElement>?,
): RouteLeg.Builder =
    unrecognizedJsonProperties(
        oldUnrecognizedProperties.orEmpty().toMutableMap().also {
            it[KEY_NOTIFICATIONS] = notifications
        },
    )

private val JsonElement.isEvNotificationSubtype: Boolean
    get() = EV_NOTIFICATIONS_SUB_TYPES.contains(
        this.asJsonObject?.get("subtype")?.asJsonPrimitive?.asString,
    )
