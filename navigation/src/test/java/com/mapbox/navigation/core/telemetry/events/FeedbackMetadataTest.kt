package com.mapbox.navigation.core.telemetry.events

import com.google.gson.Gson
import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.withPrefabTestPoint
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.Step
import com.mapbox.navigator.UserFeedbackMetadata
import nl.jqno.equalsverifier.EqualsVerifier
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class FeedbackMetadataTest {

    @Test
    fun testEmptyToJson() {
        assertEqualsJson(EMPTY_METADATA_JSON, EMPTY_FEEDBACK_METADATA.toJson(Gson()))
    }

    @Test
    fun testEmptyFromJson() {
        assertEquals(
            EMPTY_FEEDBACK_METADATA.toString(),
            FeedbackMetadata.fromJson(EMPTY_METADATA_JSON).toString(),
        )
    }

    @Test
    fun tesFilledToJson() {
        assertEqualsJson(FILLED_METADATA_JSON, FILLED_FEEDBACK_METADATA.toJson(Gson()))
    }

    @Test
    fun testFilledFromJson() {
        assertEquals(
            FILLED_FEEDBACK_METADATA.toString(),
            FeedbackMetadata.fromJson(FILLED_METADATA_JSON).toString(),
        )
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(FeedbackMetadata::class.java)
            .withPrefabTestPoint()
            .verify()

        ToStringVerifier.forClass(FeedbackMetadata::class.java)
            .verify()
    }

    private companion object {

        const val EMPTY_METADATA_JSON = """
            {"userFeedbackMetadata":{"locationsBefore":[],"locationsAfter":[],"feedbackId":""}}
        """

        val EMPTY_USER_FEEDBACK_METADATA = UserFeedbackMetadata("", emptyList(), emptyList(), null)

        val EMPTY_FEEDBACK_METADATA = FeedbackMetadata.create(EMPTY_USER_FEEDBACK_METADATA)

        val locationAfter = createFixLocation(
            coordinate = Point.fromLngLat(-122.41952214092478, 37.76333484609922),
            monotonicTimestampNanoseconds = 17103294555948L,
            time = createDate {
                set(2024, Calendar.JULY, 10, 11, 35, 15)
            },
            speed = 10.045827F,
            bearing = -3.009055F,
            altitude = null,
            accuracyHorizontal = 3.0F,
            provider = "ReplayRoute",
            bearingAccuracy = null,
            speedAccuracy = null,
            verticalAccuracy = null,
            extras = hashMapOf(),
            isMock = false,
        )

        val locationBefore = createFixLocation(
            coordinate = Point.fromLngLat(-122.4192, 37.7627),
            monotonicTimestampNanoseconds = 17103294558924L,
            time = createDate {
                set(2024, Calendar.JULY, 10, 11, 30, 55)
            },
            speed = null,
            bearing = null,
            altitude = null,
            accuracyHorizontal = null,
            provider = "ReplayRoute",
            bearingAccuracy = null,
            speedAccuracy = null,
            verticalAccuracy = null,
            extras = hashMapOf(),
            isMock = false,
        )

        private val step = createStep(
            distance = 72.10580490891357,
            distanceRemaining = 12.033105583437191,
            duration = 0.8569852460291705,
            durationRemaining = 2.896368297729623,
            upcomingName = "",
            upcomingType = "",
            upcomingModifier = "",
            upcomingInstruction = "",
            previousName = "",
            previousType = "turn",
            previousModifier = "",
            previousInstruction = "right",
        )

        val FILLED_USER_FEEDBACK_METADATA = UserFeedbackMetadata(
            "some_id",
            listOf(locationBefore),
            listOf(locationAfter),
            step,
        )

        val FILLED_FEEDBACK_METADATA = FeedbackMetadata.create(FILLED_USER_FEEDBACK_METADATA)

        const val FILLED_METADATA_JSON = """
            {
               "userFeedbackMetadata":{
                  "feedbackId":"some_id",
                  "locationsAfter":[
                     {
                        "bearing":-3.009055,
                        "coordinate":{
                           "coordinates":[
                              -122.41952214092478,
                              37.76333484609922
                           ],
                           "type":"Point"
                        },
                        "extras":{
                           
                        },
                        "monotonicTimestampNanoseconds":17103294555948,
                        "isMock":false,
                        "provider":"ReplayRoute",
                        "speed":10.045827,
                        "accuracyHorizontal":3.0,
                        "time":"Jul 10, 2024, 11:35:15 AM"
                     }
                  ],
                  "locationsBefore":[
                     {
                        "coordinate":{
                           "coordinates":[
                              -122.4192,
                              37.7627
                           ],
                           "type":"Point"
                        },
                        "extras":{
                           
                        },
                        "monotonicTimestampNanoseconds":17103294558924,
                        "isMock":false,
                        "provider":"ReplayRoute",
                        "time":"Jul 10, 2024, 11:30:55 AM"
                     }
                  ],
                  "step":{
                     "distance":72.10580490891357,
                     "distanceRemaining":12.033105583437191,
                     "duration":0.8569852460291705,
                     "durationRemaining":2.896368297729623,
                     "previousInstruction":"right",
                     "previousModifier":"",
                     "previousName":"",
                     "previousType":"turn",
                     "upcomingInstruction":"",
                     "upcomingModifier":"",
                     "upcomingName":"",
                     "upcomingType":""
                  }
               }
            }
        """

        fun assertEqualsJson(expected: String, actual: String) {
            assertEquals(JSONObject(expected).toString(), JSONObject(actual).toString())
        }

        private fun createFixLocation(
            coordinate: Point,
            monotonicTimestampNanoseconds: Long,
            time: Date,
            speed: Float?,
            bearing: Float?,
            altitude: Float?,
            accuracyHorizontal: Float?,
            provider: String?,
            bearingAccuracy: Float?,
            speedAccuracy: Float?,
            verticalAccuracy: Float?,
            extras: HashMap<String, Value>,
            isMock: Boolean,
        ): FixLocation {
            return FixLocation(
                coordinate,
                monotonicTimestampNanoseconds,
                time,
                speed,
                bearing,
                altitude,
                accuracyHorizontal,
                provider,
                bearingAccuracy,
                speedAccuracy,
                verticalAccuracy,
                extras,
                isMock,
            )
        }

        private fun createStep(
            distance: Double,
            distanceRemaining: Double,
            duration: Double,
            durationRemaining: Double,
            upcomingName: String,
            upcomingType: String,
            upcomingModifier: String,
            upcomingInstruction: String,
            previousName: String,
            previousType: String,
            previousModifier: String,
            previousInstruction: String,
        ): Step {
            return Step(
                distance,
                distanceRemaining,
                duration,
                durationRemaining,
                upcomingName,
                upcomingType,
                upcomingModifier,
                upcomingInstruction,
                previousName,
                previousType,
                previousModifier,
                previousInstruction,
            )
        }

        private fun createDate(calendarSet: Calendar.() -> Unit): Date {
            val calendar = Calendar.getInstance()
            calendarSet(calendar)
            return calendar.time
        }
    }
}
