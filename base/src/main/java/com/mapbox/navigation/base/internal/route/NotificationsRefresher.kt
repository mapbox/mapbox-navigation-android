package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.mapbox.navigation.base.internal.utils.Constants.NotificationRefreshType.DYNAMIC

internal class NotificationsRefresher {

    fun getRefreshedNotifications(
        oldNotifications: JsonArray?,
        newNotifications: JsonArray?,
        startingLegGeometryIndex: Int,
        lastRefreshLegGeometryIndex: Int,
    ): JsonArray? {
        return when {
            oldNotifications == null && newNotifications == null -> null
            oldNotifications == null -> adjustNotificationIndices(
                newNotifications,
                startingLegGeometryIndex,
            )
            newNotifications == null -> filterNotificationsByGeometryRange(
                oldNotifications,
                startingLegGeometryIndex,
                lastRefreshLegGeometryIndex,
            )
            else -> mergeNotifications(
                oldNotifications,
                newNotifications,
                startingLegGeometryIndex,
                lastRefreshLegGeometryIndex,
            )
        }
    }

    private fun mergeNotifications(
        oldNotifications: JsonArray,
        newNotifications: JsonArray,
        startingLegGeometryIndex: Int,
        lastRefreshLegGeometryIndex: Int,
    ): JsonArray {
        // Filter old notifications to keep static and dynamic notifications outside refresh range
        val result = filterNotificationsByGeometryRange(
            oldNotifications,
            startingLegGeometryIndex,
            lastRefreshLegGeometryIndex,
        )

        // Add new dynamic notifications with adjusted indices
        result.addAll(
            adjustNotificationIndices(
                newNotifications,
                startingLegGeometryIndex,
            ),
        )

        return result
    }

    private fun filterNotificationsByGeometryRange(
        notifications: JsonArray,
        startingLegGeometryIndex: Int,
        lastRefreshLegGeometryIndex: Int,
    ): JsonArray {
        val result = JsonArray()

        notifications.forEach { notification ->
            if (notification.isDynamicNotification()) {
                // Dynamic notifications: only keep those outside the refresh range
                val geometryIndex = notification.getGeometryIndex()
                if (geometryIndex != null) {
                    val isOutsideRange = geometryIndex < startingLegGeometryIndex ||
                        geometryIndex > lastRefreshLegGeometryIndex
                    if (isOutsideRange) {
                        result.add(notification)
                    }
                }
                // Dynamic notifications without geometry_index are filtered out
            } else {
                // Static notifications: always keep them
                result.add(notification)
            }
        }

        return result
    }

    /**
     * Adjusts geometry indices for new notifications using the provided starting leg geometry index.
     */
    private fun adjustNotificationIndices(
        notifications: JsonArray?,
        startingLegGeometryIndex: Int,
    ): JsonArray? {
        if (notifications == null || notifications.isEmpty) {
            return notifications
        }

        val result = JsonArray()
        notifications.forEach { notification ->
            val adjustedNotification = adjustNotificationIndex(
                notification,
                startingLegGeometryIndex,
            )
            result.add(adjustedNotification)
        }
        return result
    }

    private fun adjustNotificationIndex(
        notification: JsonElement,
        startingLegGeometryIndex: Int,
    ): JsonElement {
        if (!notification.isJsonObject) {
            return notification
        }

        val notificationObject = notification.asJsonObject
        val geometryIndex = notificationObject.get("geometry_index")?.asInt

        if (geometryIndex != null) {
            val adjustedNotification = notificationObject.deepCopy()
            adjustedNotification.addProperty(
                "geometry_index",
                startingLegGeometryIndex + geometryIndex,
            )
            return adjustedNotification
        }

        return notification
    }

    private fun JsonElement.getGeometryIndex(): Int? {
        return if (isJsonObject) {
            asJsonObject.get("geometry_index")?.asInt
        } else {
            null
        }
    }

    /**
     * Determines if a notification should be treated as dynamic (refreshable)
     * based on the refresh_type field
     */
    private fun JsonElement.isDynamicNotification(): Boolean {
        if (!isJsonObject) return false

        val refreshType = asJsonObject.get("refresh_type")?.asJsonPrimitive?.asString
        return refreshType == DYNAMIC
    }
}
