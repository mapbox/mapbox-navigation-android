package com.mapbox.navigation.core.replay.route

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayRouteMapperTest {

    private val replayRouteMapper = ReplayRouteMapper()

    @Test
    fun `should have polyline 6 option for geometry replay`() {
        val directionRoute: DirectionsRoute = mockk {
            every { routeOptions() } returns mockk {
                every { geometries() } returns DirectionsCriteria.GEOMETRY_POLYLINE6
                every { geometry() } returns ""
            }
        }

        val replayEvent = replayRouteMapper.mapDirectionsRouteGeometry(directionRoute)

        assertTrue(replayEvent.isEmpty())
    }

    @Test
    fun `should map android location`() {
        val location: Location = mockk {
            every { provider } returns "test provider"
            every { latitude } returns -122.392624
            every { longitude } returns 37.764107
            every { hasAccuracy() } returns true
            every { accuracy } returns 11.0f
            every { hasBearing() } returns true
            every { bearing } returns 12.0f
            every { hasSpeed() } returns true
            every { speed } returns 2.0f
            every { hasAltitude() } returns true
            every { altitude } returns 25.0
        }

        val replayEvent = ReplayRouteMapper.mapToUpdateLocation(0.1, location)

        val locationUpdate = replayEvent as ReplayEventUpdateLocation
        assertEquals(0.1, locationUpdate.eventTimestamp, 0.001)
        assertEquals(-122.392624, locationUpdate.location.lat, 0.000001)
        assertEquals(37.764107, locationUpdate.location.lon, 0.000001)
        assertEquals(11.0, locationUpdate.location.accuracyHorizontal)
        assertEquals(12.0, locationUpdate.location.bearing)
        assertEquals(2.0, locationUpdate.location.speed)
        assertEquals(25.0, locationUpdate.location.altitude)
    }

    @Test
    fun `should map android location with optional`() {
        val location: Location = mockk {
            every { provider } returns "test provider"
            every { latitude } returns -122.392624
            every { longitude } returns 37.764107
            every { hasAccuracy() } returns false
            every { hasBearing() } returns false
            every { hasSpeed() } returns false
            every { hasAltitude() } returns false
        }

        val replayEvent = ReplayRouteMapper.mapToUpdateLocation(0.1, location)

        val locationUpdate = replayEvent as ReplayEventUpdateLocation
        assertEquals(0.1, locationUpdate.eventTimestamp, 0.001)
        assertEquals(-122.392624, locationUpdate.location.lat, 0.000001)
        assertEquals(37.764107, locationUpdate.location.lon, 0.000001)
        assertNull(locationUpdate.location.accuracyHorizontal)
        assertNull(locationUpdate.location.bearing)
        assertNull(locationUpdate.location.speed)
        assertNull(locationUpdate.location.altitude)
    }

    @Test
    fun `mapRouteLegAnnotation should give actionable error message when map route leg annotations fails`() {
        val routeLegWithoutDistanceAnnotation: RouteLeg = RouteLeg.fromJson("""{"distance":248.3,"duration":98.7,"summary":"S Street, 32nd Street","steps":[{"distance":95.0,"duration":52.9,"geometry":"_kuphAd}vtfF|`@vMW|A~I|CfBl@","name":"","mode":"driving","maneuver":{"location":[-121.466851,38.563008],"bearing_before":0.0,"bearing_after":199.0,"instruction":"Head south","type":"depart","modifier":"right"},"voiceInstructions":[{"distanceAlongGeometry":95.0,"announcement":"Head south, then turn right onto S Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eHead south, then turn right onto S Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"},{"distanceAlongGeometry":26.9,"announcement":"Turn right onto S Street, then turn left onto 32nd Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eTurn right onto S Street, then turn left onto \u003csay-as interpret-as\u003d\"address\"\u003e32nd\u003c/say-as\u003e Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":95.0,"primary":{"text":"S Street","components":[{"text":"S Street","type":"text","abbr":"S St","abbr_priority":0}],"type":"turn","modifier":"right"}},{"distanceAlongGeometry":26.9,"primary":{"text":"S Street","components":[{"text":"S Street","type":"text","abbr":"S St","abbr_priority":0}],"type":"turn","modifier":"right"},"sub":{"text":"32nd Street","components":[{"text":"32nd Street","type":"text","abbr":"32nd St","abbr_priority":0}],"type":"turn","modifier":"left"}}],"driving_side":"right","weight":185.7,"intersections":[{"location":[-121.466851,38.563008],"bearings":[199],"entry":[true],"out":0},{"location":[-121.467087,38.562465],"bearings":[15,105,240],"entry":[false,true,true],"in":0,"out":2}]},{"distance":57.4,"duration":12.3,"geometry":"q{sphAfuwtfFmI~e@","name":"S Street","mode":"driving","maneuver":{"location":[-121.467236,38.562249],"bearing_before":198.0,"bearing_after":288.0,"instruction":"Turn right onto S Street","type":"end of road","modifier":"right"},"voiceInstructions":[{"distanceAlongGeometry":57.4,"announcement":"Turn left onto 32nd Street, then you will arrive at your destination","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eTurn left onto \u003csay-as interpret-as\u003d\"address\"\u003e32nd\u003c/say-as\u003e Street, then you will arrive at your destination\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":57.4,"primary":{"text":"32nd Street","components":[{"text":"32nd Street","type":"text","abbr":"32nd St","abbr_priority":0}],"type":"turn","modifier":"left"}}],"driving_side":"right","weight":44.7,"intersections":[{"location":[-121.467236,38.562249],"bearings":[15,105,285],"entry":[false,true,true],"in":0,"out":2}]},{"distance":96.0,"duration":33.5,"geometry":"_ftphAf|xtfFrUxHtTlHvE|A","name":"32nd Street","mode":"driving","maneuver":{"location":[-121.46786,38.562416],"bearing_before":288.0,"bearing_after":198.0,"instruction":"Turn left onto 32nd Street","type":"turn","modifier":"left"},"voiceInstructions":[{"distanceAlongGeometry":14.3,"announcement":"You have arrived at your destination, on the left","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eYou have arrived at your destination, on the left\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":96.0,"primary":{"text":"You will arrive","components":[{"text":"You will arrive","type":"text"}],"type":"arrive","modifier":"left"}},{"distanceAlongGeometry":14.3,"primary":{"text":"You have arrived","components":[{"text":"You have arrived","type":"text"}],"type":"arrive","modifier":"left"}}],"driving_side":"right","weight":33.599999999999994,"intersections":[{"location":[-121.46786,38.562416],"bearings":[15,105,195,285],"entry":[true,false,true,true],"in":1,"out":2},{"location":[-121.468168,38.561707],"bearings":[15,105,195,285],"entry":[false,true,true,true],"in":0,"out":2}]},{"distance":0.0,"duration":0.0,"geometry":"}rrphAlrytfF","name":"32nd Street","mode":"driving","maneuver":{"location":[-121.468215,38.561599],"bearing_before":199.0,"bearing_after":0.0,"instruction":"You have arrived at your destination, on the left","type":"arrive","modifier":"left"},"voiceInstructions":[],"bannerInstructions":[],"driving_side":"right","weight":0.0,"intersections":[{"location":[-121.468215,38.561599],"bearings":[19],"entry":[true],"in":0}]}],"annotation":{"speed":[2.2,2.3,2.2,0.6,8.3,1.7,6.1,9.8],"congestion":["unknown","unknown","unknown","unknown","low","unknown","unknown","low"]}}""")

        val failureMessage = try {
            replayRouteMapper.mapRouteLegAnnotation(routeLegWithoutDistanceAnnotation)
            ""
        } catch (e: Throwable) {
            e.message
        }

        assertEquals("mapRouteLegAnnotation only works when there are speed and distance profiles",
            "Directions request should include annotations DirectionsCriteria.ANNOTATION_SPEED and DirectionsCriteria.ANNOTATION_DISTANCE",
            failureMessage)
    }
}
