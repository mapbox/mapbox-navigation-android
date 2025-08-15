package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.DirectionsCriteria.NOTIFICATION_REFRESH_TYPE_DYNAMIC
import com.mapbox.api.directions.v5.models.Notification

internal class NotificationsRefresher {

    fun getRefreshedNotifications(
        oldNotifications: List<Notification>?,
        newNotifications: List<Notification>?,
        startingLegGeometryIndex: Int,
        lastRefreshLegGeometryIndex: Int,
    ): List<Notification>? {
        val result = when {
            oldNotifications == null && newNotifications == null -> null
            oldNotifications == null -> adjustNotificationIndices(
                newNotifications.orEmpty(),
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

        return result?.takeIf { it.isNotEmpty() }
    }

    private fun mergeNotifications(
        oldNotifications: List<Notification>,
        newNotifications: List<Notification>,
        startingLegGeometryIndex: Int,
        lastRefreshLegGeometryIndex: Int,
    ): List<Notification> {
        // Filter old notifications to keep static and dynamic notifications outside refresh range
        val oldFiltered = filterNotificationsByGeometryRange(
            oldNotifications,
            startingLegGeometryIndex,
            lastRefreshLegGeometryIndex,
        )

        // Add new dynamic notifications with adjusted indices
        val newAdjusted = adjustNotificationIndices(
            newNotifications,
            startingLegGeometryIndex,
        )

        return oldFiltered + newAdjusted
    }

    private fun filterNotificationsByGeometryRange(
        notifications: List<Notification>,
        startingLegGeometryIndex: Int,
        lastRefreshLegGeometryIndex: Int,
    ): List<Notification> =
        notifications.mapNotNull { notification ->
            if (notification.isDynamic) {
                // Dynamic notifications: only keep those outside the refresh range
                val geometryIndex = notification.geometryIndex()
                if (geometryIndex != null) {
                    val isOutsideRange = geometryIndex < startingLegGeometryIndex ||
                        geometryIndex > lastRefreshLegGeometryIndex
                    if (isOutsideRange) {
                        return@mapNotNull notification
                    }
                }
                // Dynamic notifications without geometry_index are filtered out
                null
            } else {
                // Static notifications: always keep them
                notification
            }
        }

    /**
     * Adjusts geometry indices for new notifications using the provided starting leg geometry index.
     */
    private fun adjustNotificationIndices(
        notifications: List<Notification>,
        startingLegGeometryIndex: Int,
    ): List<Notification> =
        notifications.map { notification ->
            adjustNotificationIndex(
                notification,
                startingLegGeometryIndex,
            )
        }

    private fun adjustNotificationIndex(
        notification: Notification,
        startingLegGeometryIndex: Int,
    ): Notification {
        return notification.toBuilder().apply {
            notification.geometryIndex()?.let {
                geometryIndex(it + startingLegGeometryIndex)
            }
            notification.geometryIndexStart()?.let {
                geometryIndexStart(it + startingLegGeometryIndex)
            }
            notification.geometryIndexEnd()?.let {
                geometryIndexEnd(it + startingLegGeometryIndex)
            }
        }.build()
    }

    private val Notification.isDynamic: Boolean
        get() = refreshType() == NOTIFICATION_REFRESH_TYPE_DYNAMIC
}
