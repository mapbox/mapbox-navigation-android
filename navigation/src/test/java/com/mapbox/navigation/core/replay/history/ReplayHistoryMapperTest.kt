package com.mapbox.navigation.core.replay.history

import com.mapbox.navigation.core.history.model.HistoryEventGetStatus
import com.mapbox.navigation.core.history.model.HistoryEventPushHistoryRecord
import com.mapbox.navigation.core.history.model.HistoryEventSetRoute
import com.mapbox.navigation.core.history.model.HistoryEventUpdateLocation
import com.mapbox.navigation.testing.BuilderTest
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.reflect.KClass

class ReplayHistoryMapperTest : BuilderTest<ReplayHistoryMapper, ReplayHistoryMapper.Builder>() {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    override fun getImplementationClass(): KClass<ReplayHistoryMapper> =
        ReplayHistoryMapper::class

    override fun getFilledUpBuilder() = ReplayHistoryMapper.Builder()
        .locationMapper {
            ReplayEventUpdateLocation(
                it.eventTimestamp,
                ReplayEventLocation(
                    lat = it.location.latitude,
                    lon = it.location.longitude,
                    provider = "test provider",
                    time = it.eventTimestamp,
                    altitude = null,
                    accuracyHorizontal = null,
                    bearing = null,
                    speed = null,
                ),
            )
        }
        .setRouteMapper {
            ReplaySetNavigationRoute.Builder(it.eventTimestamp).build()
        }
        .statusMapper {
            ReplayEventGetStatus(it.eventTimestamp)
        }
        .pushEventMappers(
            listOf(
                ReplayHistoryEventMapper {
                    object : ReplayEventBase {
                        override val eventTimestamp: Double = 1357.0246
                    }
                },
            ),
        )

    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }

    @Test
    fun `should map HistoryEventUpdateLocation`() {
        val event: HistoryEventUpdateLocation = mockk {
            every { eventTimestamp } returns 1580744198.879556
            every { location } returns mockk(relaxed = true)
        }

        val replayHistoryMapper = ReplayHistoryMapper.Builder().build()
        val replayLocationEvent = replayHistoryMapper.mapToReplayEvent(event)!!

        assertTrue(replayLocationEvent is ReplayEventUpdateLocation)
        assertEquals(1580744198.879556, replayLocationEvent.eventTimestamp, 0.0001)
    }

    @Test
    fun `should map HistoryEventSetRoute`() {
        val event: HistoryEventSetRoute = mockk {
            every { eventTimestamp } returns 1580744198.879556
            every { navigationRoute } returns mockk(relaxed = true)
        }

        val replayHistoryMapper = ReplayHistoryMapper.Builder().build()
        val replayEvent = replayHistoryMapper.mapToReplayEvent(event)!!

        assertTrue(replayEvent is ReplaySetNavigationRoute)
        assertEquals(1580744198.879556, replayEvent.eventTimestamp, 0.0001)
    }

    @Test
    fun `should map HistoryEventGetStatus`() {
        val event: HistoryEventGetStatus = mockk {
            every { eventTimestamp } returns 1580744198.879556
        }

        val replayHistoryMapper = ReplayHistoryMapper.Builder().build()
        val replayEvent = replayHistoryMapper.mapToReplayEvent(event)!!

        assertTrue(replayEvent is ReplayEventGetStatus)
        assertEquals(1580744198.879556, replayEvent.eventTimestamp, 0.0001)
    }

    @Test
    fun `should be able to disable HistoryEventUpdateLocation`() {
        val event: HistoryEventUpdateLocation = mockk {
            every { eventTimestamp } returns 1580744198.879556
        }

        val replayHistoryMapper = ReplayHistoryMapper.Builder()
            .locationMapper(null)
            .build()
        val replayEvent = replayHistoryMapper.mapToReplayEvent(event)

        assertNull(replayEvent)
    }

    @Test
    fun `should be able to disable HistoryEventSetRoute`() {
        val event: HistoryEventSetRoute = mockk {
            every { eventTimestamp } returns 1580744198.879556
        }

        val replayHistoryMapper = ReplayHistoryMapper.Builder()
            .setRouteMapper(null)
            .build()
        val replayEvent = replayHistoryMapper.mapToReplayEvent(event)

        assertNull(replayEvent)
    }

    @Test
    fun `should be able to disable HistoryEventGetStatus`() {
        val event: HistoryEventGetStatus = mockk {
            every { eventTimestamp } returns 1580744198.879556
        }

        val replayHistoryMapper = ReplayHistoryMapper.Builder()
            .statusMapper(null)
            .build()
        val replayEvent = replayHistoryMapper.mapToReplayEvent(event)

        assertNull(replayEvent)
    }

    @Test
    fun `should map HistoryEventPushHistoryRecord to null by default`() {
        val event: HistoryEventPushHistoryRecord = mockk {
            every { eventTimestamp } returns 1580744198.879556
        }

        val replayHistoryMapper = ReplayHistoryMapper.Builder().build()
        val replayEvent = replayHistoryMapper.mapToReplayEvent(event)

        assertNull(replayEvent)
    }

    @Test
    fun `should allow custom HistoryEventPushHistoryRecord mapper`() {
        val event: HistoryEventPushHistoryRecord = mockk {
            every { eventTimestamp } returns 1580744198.879556
            every { type } returns "feedback_initiated"
            every { properties } returns mapOf(
                "user_id" to "13495",
                "feedback" to "Amazing alternative route suggestion",
                "rating" to "5",
            ).toString()
        }

        class FeedbackReplayEventBase(
            override val eventTimestamp: Double,
            val userId: Long,
            val feedback: String,
            val rating: Int,
        ) : ReplayEventBase

        val replayHistoryMapper = ReplayHistoryMapper.Builder()
            .pushEventMappers(
                listOf(
                    ReplayHistoryEventMapper { pushHistory ->
                        val propertiesMap = pushHistory.properties
                            .removeSurrounding("{", "}")
                            .split(", ")
                            .associate {
                                val (left, right) = it.split("=")
                                left to right.trim()
                            }

                        FeedbackReplayEventBase(
                            eventTimestamp = pushHistory.eventTimestamp,
                            userId = propertiesMap["user_id"]!!.toLong(),
                            feedback = propertiesMap["feedback"]!!,
                            rating = propertiesMap["rating"]!!.toInt(),
                        )
                    },
                ),
            )
            .build()

        val replayEvent = replayHistoryMapper.mapToReplayEvent(event)
            as FeedbackReplayEventBase

        assertEquals(1580744198.879556, replayEvent.eventTimestamp, 0.0001)
        assertEquals(13495L, replayEvent.userId)
        assertEquals("Amazing alternative route suggestion", replayEvent.feedback)
        assertEquals(5, replayEvent.rating)
    }
}
