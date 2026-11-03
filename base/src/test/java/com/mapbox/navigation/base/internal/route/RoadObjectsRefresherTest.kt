package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.Incident
import org.junit.Assert.assertEquals
import org.junit.Test

class RoadObjectsRefresherTest {

    private val incidentsRefresher = IncidentsRefresher

    @Test
    fun nullOldObjectsNullNewObjects() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            null,
            null,
            0,
            10,
        )
        assertEquals(emptyList<Incident>(), result)
    }

    @Test
    fun noOldObjectsNoNewObjects() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            emptyList(),
            emptyList(),
            0,
            10,
        )
        assertEquals(emptyList<Incident>(), result)
    }

    @Test
    fun indicesAreAdjustedZero() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            emptyList(),
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(25)
                    .geometryIndexEnd(null)
                    .build(),
            ),
            0,
            40,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(25)
                    .geometryIndexEnd(null)
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun indicesAreAdjustedNonZero() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            emptyList(),
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(25)
                    .geometryIndexEnd(null)
                    .build(),
            ),
            10,
            37,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(15)
                    .geometryIndexEnd(18)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun objectsOutsideTheRangeAreKeptWhereverTheyAreListed() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                incident("overlaps-start", 10, 50),
                incident("before", 20, 25),
                incident("after", 70, 80),
                incident("inside", 40, 45),
                incident("overlaps-end", 55, 65),
                incident("after-2", 61, 62),
            ),
            listOf(incident("new", 5, 8)),
            30,
            60,
        )

        assertEquals(
            listOf("before", "new", "after", "after-2").zip(
                listOf(20 to 25, 35 to 38, 70 to 80, 61 to 62),
            ).map { (id, range) -> incident(id, range.first, range.second) },
            result,
        )
    }

    @Test
    fun rangeBoundariesAreStrict() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                incident("ends-before-start", 20, 29),
                incident("ends-at-start", 20, 30),
                incident("starts-at-last", 60, 70),
                incident("starts-after-last", 61, 70),
            ),
            emptyList(),
            30,
            60,
        )

        assertEquals(listOf("ends-before-start", "starts-after-last"), result.map { it.id() })
    }

    @Test
    fun objectsWithOneGeometryIndexAreTestedByIt() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                incident("overlaps", 10, 50),
                incident("ends-before", null, 20),
                incident("starts-before", 25, null),
                incident("starts-inside", 40, null),
                incident("ends-after", null, 61),
                incident("starts-after", 70, null),
            ),
            emptyList(),
            30,
            60,
        )

        assertEquals(
            listOf("ends-before", "starts-before", "ends-after", "starts-after"),
            result.map { it.id() },
        )
    }

    @Test
    fun objectWithoutGeometryIndexBetweenRangeObjectsIsDropped() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                incident("overlaps-start", 10, 50),
                incident("unplaced", null, null),
                incident("overlaps-end", 55, 65),
            ),
            emptyList(),
            30,
            60,
        )

        assertEquals(emptyList<Incident>(), result)
    }

    @Test
    fun onlyObjectWithoutGeometryIndexIsKeptAsBeforeAndAfter() {
        val unplaced = incident("unplaced", null, null)

        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(unplaced),
            emptyList(),
            30,
            60,
        )

        assertEquals(listOf(unplaced, unplaced), result)
    }

    @Test
    fun closuresOutsideTheRangeAreKeptWhereverTheyAreListed() {
        fun closure(start: Int, end: Int) = Closure.builder()
            .geometryIndexStart(start)
            .geometryIndexEnd(end)
            .build()

        val result = ClosuresRefresher.getRefreshedRoadObjects(
            listOf(closure(10, 50), closure(20, 25), closure(55, 65), closure(61, 62)),
            listOf(closure(0, 2)),
            30,
            60,
        )

        assertEquals(listOf(closure(20, 25), closure(30, 32), closure(61, 62)), result)
    }

    private fun incident(id: String, start: Int?, end: Int?) = Incident.builder()
        .id(id)
        .geometryIndexStart(start)
        .geometryIndexEnd(end)
        .build()

    @Test
    fun nullOldRoadObjectsHasNewRoadObjects() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            null,
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
            ),
            0,
            40,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun hadRoadObjectsNoRoadObjectsRefreshedEverything() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            null,
            0,
            130,
        )

        assertEquals(
            emptyList<Incident>(),
            result,
        )
    }

    @Test
    fun hadRoadObjectsNoRoadObjectsRefreshedStart() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            null,
            0,
            30,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun hadRoadObjectsNoRoadObjectsRefreshedMiddle() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            null,
            30,
            80,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun hadRoadObjectsNoRoadObjectsRefreshedEnd() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            null,
            80,
            130,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun hadRoadObjectsHasNewRoadObjectsRefreshedEverything() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(90)
                    .geometryIndexEnd(110)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(115)
                    .geometryIndexEnd(117)
                    .build(),
            ),
            0,
            130,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(90)
                    .geometryIndexEnd(110)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(115)
                    .geometryIndexEnd(117)
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun hadRoadObjectsHasNewRoadObjectsRefreshedStart() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(6)
                    .geometryIndexEnd(8)
                    .build(),
            ),
            0,
            30,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(6)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun hadRoadObjectsHasNewRoadObjectsRefreshedMiddle() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            listOf(
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(24)
                    .geometryIndexEnd(30)
                    .build(),
            ),
            30,
            80,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(54)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun hadRoadObjectsHasNewRoadObjectsRefreshedEnd() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id9")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(90)
                    .build(),
                Incident.builder()
                    .id("id10")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(120)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            listOf(
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(41)
                    .geometryIndexEnd(50)
                    .build(),
            ),
            80,
            130,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(5)
                    .geometryIndexEnd(8)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(11)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id4")
                    .geometryIndexStart(35)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id5")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id6")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id7")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(75)
                    .build(),
                Incident.builder()
                    .id("id8")
                    .geometryIndexStart(null)
                    .geometryIndexEnd(null)
                    .build(),
                Incident.builder()
                    .id("id11")
                    .geometryIndexStart(121)
                    .geometryIndexEnd(130)
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun oldRoadObjectStartIntersectsWithRefreshInterval() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                Incident.builder()
                    .id("id1")
                    .geometryIndexStart(25)
                    .geometryIndexEnd(35)
                    .build(),
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
            ),
            listOf(
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(24)
                    .geometryIndexEnd(30)
                    .build(),
            ),
            30,
            80,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(54)
                    .geometryIndexEnd(60)
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun oldRoadObjectEndIntersectsWithRefreshInterval() {
        val result = incidentsRefresher.getRefreshedRoadObjects(
            listOf(
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(55)
                    .geometryIndexEnd(60)
                    .build(),
                Incident.builder()
                    .id("id3")
                    .geometryIndexStart(75)
                    .geometryIndexEnd(85)
                    .build(),
            ),
            listOf(
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(24)
                    .geometryIndexEnd(30)
                    .build(),
            ),
            30,
            80,
        )

        assertEquals(
            listOf(
                Incident.builder()
                    .id("id2")
                    .geometryIndexStart(54)
                    .geometryIndexEnd(60)
                    .build(),
            ),
            result,
        )
    }
}
