package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.DirectionsCriteria.NOTIFICATION_REFRESH_TYPE_DYNAMIC
import com.mapbox.api.directions.v5.DirectionsCriteria.NOTIFICATION_REFRESH_TYPE_STATIC
import com.mapbox.api.directions.v5.models.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationsRefresherTest {

    private val refresher = NotificationsRefresher()

    @Test
    fun `getRefreshedNotifications returns null when both inputs are null`() {
        val result = refresher.getRefreshedNotifications(
            oldNotifications = null,
            newNotifications = null,
            startingLegGeometryIndex = 0,
            lastRefreshLegGeometryIndex = 5,
        )
        assertNull(result)
    }

    @Test
    fun `getRefreshedNotifications returns adjusted new notifications when old is null`() {
        val newNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "test",
                geometryIndex = 2,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
        )
        val result = refresher.getRefreshedNotifications(
            oldNotifications = null,
            newNotifications = newNotifications,
            startingLegGeometryIndex = 5,
            lastRefreshLegGeometryIndex = 10,
        )

        assertEquals(1, result!!.size)
        assertEquals(7, result[0].geometryIndex()) // 2 + 5 offset
        assertEquals("test", result[0].subtype())
        assertEquals(NOTIFICATION_REFRESH_TYPE_DYNAMIC, result[0].refreshType())
    }

    @Test
    fun `getRefreshedNotifications returns filtered old notifications when new is null`() {
        val oldNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "static1",
                geometryIndex = null,
                refreshType = NOTIFICATION_REFRESH_TYPE_STATIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamic1",
                geometryIndex = 3,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamic2",
                geometryIndex = 8,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
        )

        val result = refresher.getRefreshedNotifications(
            oldNotifications = oldNotifications,
            newNotifications = null,
            startingLegGeometryIndex = 2,
            lastRefreshLegGeometryIndex = 6,
        )

        // Should keep static notification and dynamic notification outside range (index 8)
        assertEquals(2, result!!.size)
        val resultSubtypes = result.map { it.subtype() ?: "" }.sorted()
        assertEquals(listOf("dynamic2", "static1"), resultSubtypes)
    }

    @Test
    fun `getRefreshedNotifications merges static and dynamic notifications correctly`() {
        val oldNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "static1",
                geometryIndex = null,
                refreshType = NOTIFICATION_REFRESH_TYPE_STATIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamic1",
                geometryIndex = 3,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamic2",
                geometryIndex = 10,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
        )

        val newNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "newDynamic",
                geometryIndex = 2,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
        )

        val result = refresher.getRefreshedNotifications(
            oldNotifications = oldNotifications,
            newNotifications = newNotifications,
            startingLegGeometryIndex = 5,
            lastRefreshLegGeometryIndex = 8,
        )

        // Should contain:
        // 1. Static notification from old (always kept)
        // 2. Dynamic notification from old outside refresh range (index 3, which is < 5)
        // 3. Dynamic notification from old outside refresh range (index 10, which is > 8)
        // 4. New dynamic notification with adjusted index (2 + 5 = 7)

        // Check that we have exactly 4 notifications
        assertEquals(4, result!!.size)

        val resultSubtypes = result.map { it.subtype() ?: "" }.sorted()
        val expectedSubtypes = listOf("static1", "dynamic1", "dynamic2", "newDynamic").sorted()

        assertEquals(expectedSubtypes, resultSubtypes)
    }

    @Test
    fun `getRefreshedNotifications with only static notifications and adding dynamic ones`() {
        val oldNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "static1",
                geometryIndex = null,
                refreshType = NOTIFICATION_REFRESH_TYPE_STATIC,
            ),
            createNotification(
                type = "alert",
                subtype = "static2",
                geometryIndex = 5,
                refreshType = NOTIFICATION_REFRESH_TYPE_STATIC,
            ),
        )

        val newNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "newDynamic1",
                geometryIndex = 2,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "newDynamic2",
                geometryIndex = 8,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
        )

        val result = refresher.getRefreshedNotifications(
            oldNotifications = oldNotifications,
            newNotifications = newNotifications,
            startingLegGeometryIndex = 3,
            lastRefreshLegGeometryIndex = 7,
        )

        // Should contain:
        // 1. Both static notifications from old (always kept)
        // 2. Both new dynamic notifications with adjusted indices (2+3=5, 8+3=11)
        assertEquals(4, result!!.size)

        val resultSubtypes = result.map { it.subtype() ?: "" }.sorted()
        val expectedSubtypes = listOf("static1", "static2", "newDynamic1", "newDynamic2").sorted()

        assertEquals(expectedSubtypes, resultSubtypes)
    }

    @Test
    fun `getRefreshedNotifications with only dynamic notifications mixed inside and outside range`() {
        val oldNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "dynamic1",
                geometryIndex = 1, // outside range (< 5)
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamic2",
                geometryIndex = 6, // inside range (5-10)
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamic3",
                geometryIndex = 12, // outside range (> 10)
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamic4",
                geometryIndex = 8, // inside range (5-10)
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
        )

        val newNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "newDynamic1",
                geometryIndex = 3,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "newDynamic2",
                geometryIndex = 7,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
        )

        val result = refresher.getRefreshedNotifications(
            oldNotifications = oldNotifications,
            newNotifications = newNotifications,
            startingLegGeometryIndex = 5,
            lastRefreshLegGeometryIndex = 10,
        )

        // Should contain:
        // 1. dynamic1 (index 1 < 5, outside range - kept)
        // 2. dynamic3 (index 12 > 10, outside range - kept)
        // 3. newDynamic1 (3 + 5 = 8, adjusted index)
        // 4. newDynamic2 (7 + 5 = 12, adjusted index)
        // Should NOT contain dynamic2 and dynamic4 (inside range 5-10)
        assertEquals(4, result!!.size)

        val resultSubtypes = result.map { it.subtype() ?: "" }.sorted()
        val expectedSubtypes = listOf("dynamic1", "dynamic3", "newDynamic1", "newDynamic2").sorted()

        assertEquals(expectedSubtypes, resultSubtypes)

        // Verify geometry indices are correctly adjusted for new notifications
        val newDynamic1Result = result.find { it.subtype() == "newDynamic1" }?.geometryIndex()
        assertEquals(8, newDynamic1Result) // 3 + 5

        val newDynamic2Result = result.find { it.subtype() == "newDynamic2" }?.geometryIndex()
        assertEquals(12, newDynamic2Result) // 7 + 5
    }

    @Test
    fun `getRefreshedNotifications with mixed static and dynamic, complex geometry ranges`() {
        val oldNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "static1",
                geometryIndex = null,
                refreshType = NOTIFICATION_REFRESH_TYPE_STATIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamic1",
                geometryIndex = 0, // outside range (< 2)
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamic2",
                geometryIndex = 3, // inside range (2-5)
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "static2",
                geometryIndex = 4, // inside range but static
                refreshType = NOTIFICATION_REFRESH_TYPE_STATIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamic3",
                geometryIndex = 7, // outside range (> 5)
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
        )

        val newNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "newDynamic",
                geometryIndex = 1,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
        )

        val result = refresher.getRefreshedNotifications(
            oldNotifications = oldNotifications,
            newNotifications = newNotifications,
            startingLegGeometryIndex = 2,
            lastRefreshLegGeometryIndex = 5,
        )

        // Should contain:
        // 1. static1 (always kept)
        // 2. static2 (always kept)
        // 3. dynamic1 (index 0 < 2, outside range - kept)
        // 4. dynamic3 (index 7 > 5, outside range - kept)
        // 5. newDynamic (1 + 2 = 3, adjusted index)
        // Should NOT contain dynamic2 (inside range 2-5)
        assertEquals(5, result!!.size)

        val resultSubtypes = result.map { it.subtype() ?: "" }.sorted()
        val expectedSubtypes = listOf(
            "static1",
            "static2",
            "dynamic1",
            "dynamic3",
            "newDynamic",
        ).sorted()

        assertEquals(expectedSubtypes, resultSubtypes)
    }

    @Test
    fun `getRefreshedNotifications with dynamic notifications without geometry_index are filtered out`() {
        val oldNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "static1",
                geometryIndex = null,
                refreshType = NOTIFICATION_REFRESH_TYPE_STATIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamicNoIndex",
                geometryIndex = null, // dynamic without geometry_index
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamic1",
                geometryIndex = 10,
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
        )

        val result = refresher.getRefreshedNotifications(
            oldNotifications = oldNotifications,
            newNotifications = null,
            startingLegGeometryIndex = 5,
            lastRefreshLegGeometryIndex = 8,
        )

        // Should contain:
        // 1. static1 (always kept)
        // 2. dynamic1 (index 10 > 8, outside range - kept)
        // Should NOT contain dynamicNoIndex (dynamic without geometry_index)
        assertEquals(2, result!!.size)

        val resultSubtypes = result.map { it.subtype() ?: "" }.sorted()
        val expectedSubtypes = listOf("static1", "dynamic1").sorted()

        assertEquals(expectedSubtypes, resultSubtypes)
    }

    @Test
    fun `getRefreshedNotifications with edge case geometry ranges`() {
        val oldNotifications = listOf(
            createNotification(
                type = "alert",
                subtype = "dynamicAtStart",
                geometryIndex = 5, // exactly at start
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamicAtEnd",
                geometryIndex = 10, // exactly at end
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamicJustBefore",
                geometryIndex = 4, // just before start
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
            createNotification(
                type = "alert",
                subtype = "dynamicJustAfter",
                geometryIndex = 11, // just after end
                refreshType = NOTIFICATION_REFRESH_TYPE_DYNAMIC,
            ),
        )

        val result = refresher.getRefreshedNotifications(
            oldNotifications = oldNotifications,
            newNotifications = null,
            startingLegGeometryIndex = 5,
            lastRefreshLegGeometryIndex = 10,
        )

        // Should keep:
        // - dynamicJustBefore (index 4 < 5)
        // - dynamicJustAfter (index 11 > 10)
        // Should filter out:
        // - dynamicAtStart (index 5 >= 5 and <= 10)
        // - dynamicAtEnd (index 10 >= 5 and <= 10)
        assertEquals(2, result!!.size)

        val resultSubtypes = result.map { it.subtype() ?: "" }.sorted()
        val expectedSubtypes = listOf("dynamicJustBefore", "dynamicJustAfter").sorted()

        assertEquals(expectedSubtypes, resultSubtypes)
    }

    // Helper function to create real Notification objects using builder
    private fun createNotification(
        type: String,
        subtype: String,
        geometryIndex: Int?,
        refreshType: String,
        geometryIndexStart: Int? = null,
        geometryIndexEnd: Int? = null,
    ): Notification {
        return Notification.builder()
            .type(type)
            .subtype(subtype)
            .apply {
                geometryIndex?.let { geometryIndex(it) }
                geometryIndexStart?.let { geometryIndexStart(it) }
                geometryIndexEnd?.let { geometryIndexEnd(it) }
            }
            .refreshType(refreshType)
            .build()
    }
}
