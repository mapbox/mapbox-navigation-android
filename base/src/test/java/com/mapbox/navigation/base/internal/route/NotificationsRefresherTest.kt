package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonArray
import com.google.gson.JsonObject
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
        val newNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "test",
                    geometryIndex = 2,
                    refreshType = "dynamic",
                ),
            ),
        )
        val result = refresher.getRefreshedNotifications(
            oldNotifications = null,
            newNotifications = newNotifications,
            startingLegGeometryIndex = 5,
            lastRefreshLegGeometryIndex = 10,
        )

        val expected = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "test",
                    geometryIndex = 7, // 2 + 5 offset
                    refreshType = "dynamic",
                ),
            ),
        )
        assertEquals(expected.toString(), result.toString())
    }

    @Test
    fun `getRefreshedNotifications returns filtered old notifications when new is null`() {
        val oldNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "static1",
                    geometryIndex = null,
                    refreshType = "static",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic1",
                    geometryIndex = 3,
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic2",
                    geometryIndex = 8,
                    refreshType = "dynamic",
                ),
            ),
        )

        val result = refresher.getRefreshedNotifications(
            oldNotifications = oldNotifications,
            newNotifications = null,
            startingLegGeometryIndex = 2,
            lastRefreshLegGeometryIndex = 6,
        )

        // Should keep static notification and dynamic notification outside range (index 8)
        val expected = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "static1",
                    geometryIndex = null,
                    refreshType = "static",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic2",
                    geometryIndex = 8,
                    refreshType = "dynamic",
                ),
            ),
        )
        assertEquals(expected.toString(), result.toString())
    }

    @Test
    fun `getRefreshedNotifications merges static and dynamic notifications correctly`() {
        val oldNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "static1",
                    geometryIndex = null,
                    refreshType = "static",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic1",
                    geometryIndex = 3,
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic2",
                    geometryIndex = 10,
                    refreshType = "dynamic",
                ),
            ),
        )

        val newNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "newDynamic",
                    geometryIndex = 2,
                    refreshType = "dynamic",
                ),
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
        assertEquals("Expected 4 notifications", 4, result!!.size())

        // Convert to string arrays for easier comparison
        val resultStrings = (0 until result.size()).map {
            result.get(it).asJsonObject.get("subtype").asString
        }.sorted()
        val expectedSubtypes = listOf("static1", "dynamic1", "dynamic2", "newDynamic").sorted()

        assertEquals("Notification subtypes should match", expectedSubtypes, resultStrings)
    }

    @Test
    fun `getRefreshedNotifications with only static notifications and adding dynamic ones`() {
        val oldNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "static1",
                    geometryIndex = null,
                    refreshType = "static",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "static2",
                    geometryIndex = 5,
                    refreshType = "static",
                ),
            ),
        )

        val newNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "newDynamic1",
                    geometryIndex = 2,
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "newDynamic2",
                    geometryIndex = 8,
                    refreshType = "dynamic",
                ),
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
        assertEquals("Expected 4 notifications", 4, result!!.size())

        val resultSubtypes = (0 until result.size()).map {
            result.get(it).asJsonObject.get("subtype").asString
        }.sorted()
        val expectedSubtypes = listOf("static1", "static2", "newDynamic1", "newDynamic2").sorted()

        assertEquals("Notification subtypes should match", expectedSubtypes, resultSubtypes)
    }

    @Test
    fun `getRefreshedNotifications with only dynamic notifications mixed inside and outside range`() {
        val oldNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "dynamic1",
                    geometryIndex = 1, // outside range (< 5)
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic2",
                    geometryIndex = 6, // inside range (5-10)
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic3",
                    geometryIndex = 12, // outside range (> 10)
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic4",
                    geometryIndex = 8, // inside range (5-10)
                    refreshType = "dynamic",
                ),
            ),
        )

        val newNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "newDynamic1",
                    geometryIndex = 3,
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "newDynamic2",
                    geometryIndex = 7,
                    refreshType = "dynamic",
                ),
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
        assertEquals("Expected 4 notifications", 4, result!!.size())

        val resultSubtypes = (0 until result.size()).map {
            result.get(it).asJsonObject.get("subtype").asString
        }.sorted()
        val expectedSubtypes = listOf("dynamic1", "dynamic3", "newDynamic1", "newDynamic2").sorted()

        assertEquals("Notification subtypes should match", expectedSubtypes, resultSubtypes)

        // Verify geometry indices are correctly adjusted for new notifications
        val newDynamic1Result = (0 until result.size()).find {
            result.get(it).asJsonObject.get("subtype").asString == "newDynamic1"
        }?.let { result.get(it).asJsonObject.get("geometry_index").asInt }
        assertEquals("newDynamic1 should have adjusted index", 8, newDynamic1Result)

        val newDynamic2Result = (0 until result.size()).find {
            result.get(it).asJsonObject.get("subtype").asString == "newDynamic2"
        }?.let { result.get(it).asJsonObject.get("geometry_index").asInt }
        assertEquals("newDynamic2 should have adjusted index", 12, newDynamic2Result)
    }

    @Test
    fun `getRefreshedNotifications with mixed static and dynamic, complex geometry ranges`() {
        val oldNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "static1",
                    geometryIndex = null,
                    refreshType = "static",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic1",
                    geometryIndex = 0, // outside range (< 2)
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic2",
                    geometryIndex = 3, // inside range (2-5)
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "static2",
                    geometryIndex = 4, // inside range but static
                    refreshType = "static",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic3",
                    geometryIndex = 7, // outside range (> 5)
                    refreshType = "dynamic",
                ),
            ),
        )

        val newNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "newDynamic",
                    geometryIndex = 1,
                    refreshType = "dynamic",
                ),
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
        assertEquals("Expected 5 notifications", 5, result!!.size())

        val resultSubtypes = (0 until result.size()).map {
            result.get(it).asJsonObject.get("subtype").asString
        }.sorted()
        val expectedSubtypes = listOf(
            "static1",
            "static2",
            "dynamic1",
            "dynamic3",
            "newDynamic",
        ).sorted()

        assertEquals("Notification subtypes should match", expectedSubtypes, resultSubtypes)
    }

    @Test
    fun `getRefreshedNotifications with dynamic notifications without geometry_index are filtered out`() {
        val oldNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "static1",
                    geometryIndex = null,
                    refreshType = "static",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamicNoIndex",
                    geometryIndex = null, // dynamic without geometry_index
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamic1",
                    geometryIndex = 10,
                    refreshType = "dynamic",
                ),
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
        assertEquals("Expected 2 notifications", 2, result!!.size())

        val resultSubtypes = (0 until result.size()).map {
            result.get(it).asJsonObject.get("subtype").asString
        }.sorted()
        val expectedSubtypes = listOf("static1", "dynamic1").sorted()

        assertEquals("Notification subtypes should match", expectedSubtypes, resultSubtypes)
    }

    @Test
    fun `getRefreshedNotifications with edge case geometry ranges`() {
        val oldNotifications = createNotifications(
            listOf(
                NotificationData(
                    type = "alert",
                    subtype = "dynamicAtStart",
                    geometryIndex = 5, // exactly at start
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamicAtEnd",
                    geometryIndex = 10, // exactly at end
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamicJustBefore",
                    geometryIndex = 4, // just before start
                    refreshType = "dynamic",
                ),
                NotificationData(
                    type = "alert",
                    subtype = "dynamicJustAfter",
                    geometryIndex = 11, // just after end
                    refreshType = "dynamic",
                ),
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
        assertEquals("Expected 2 notifications", 2, result!!.size())

        val resultSubtypes = (0 until result.size()).map {
            result.get(it).asJsonObject.get("subtype").asString
        }.sorted()
        val expectedSubtypes = listOf("dynamicJustBefore", "dynamicJustAfter").sorted()

        assertEquals("Notification subtypes should match", expectedSubtypes, resultSubtypes)
    }

    // Helper data class for creating test notifications
    private data class NotificationData(
        val type: String,
        val subtype: String,
        val geometryIndex: Int?,
        val refreshType: String,
    )

    // Helper function to create JsonArray of notifications
    private fun createNotifications(notifications: List<NotificationData>): JsonArray {
        val jsonArray = JsonArray()
        notifications.forEach { notification ->
            val jsonObject = JsonObject().apply {
                addProperty("type", notification.type)
                addProperty("subtype", notification.subtype)
                addProperty("refresh_type", notification.refreshType)
                notification.geometryIndex?.let { addProperty("geometry_index", it) }
            }
            jsonArray.add(jsonObject)
        }
        return jsonArray
    }
}
