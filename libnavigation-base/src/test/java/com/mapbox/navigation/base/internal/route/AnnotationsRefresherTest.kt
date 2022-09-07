package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.MaxSpeed
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AnnotationsRefresherTest(
    private val oldAnnotation: LegAnnotation?,
    private val newAnnotation: LegAnnotation?,
    private val legGeometryIndex: Int,
    private val expectedMergedAnnotation: LegAnnotation?,
    private val description: String,
) {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{4}")
        fun data(): Collection<Array<Any?>> {
            val defaultAnnotation = LegAnnotation.builder().build()
            val annotationWithCongestionNumericOnlyEmpty = defaultAnnotation.toBuilder()
                .congestionNumeric(listOf())
                .build()
            val annotationWithCongestionNumericOnlyFilled = defaultAnnotation.toBuilder()
                .congestionNumeric(listOf(1, 2, 3, 4, 5))
                .build()
            return listOf(
                arrayOf(
                    null,
                    null,
                    0,
                    null,
                    "Everything is null. Null old annotation turns merged one to null."
                ),
                arrayOf(
                    defaultAnnotation,
                    null,
                    0,
                    defaultAnnotation,
                    "Default + null. Null old annotation property turns merged property to null."
                ),
                arrayOf(
                    defaultAnnotation,
                    defaultAnnotation,
                    0,
                    defaultAnnotation,
                    "2 default annotations, index = 0. " +
                        "Null old annotation property turns merged property to null."
                ),
                arrayOf(
                    defaultAnnotation,
                    defaultAnnotation,
                    5,
                    defaultAnnotation,
                    "2 default annotations, index = 5. " +
                        "Null old annotation property turns merged property to null."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyEmpty,
                    defaultAnnotation,
                    0,
                    annotationWithCongestionNumericOnlyEmpty,
                    "Empty congestion_numeric + default, index = 0. " +
                        "congestion_numeric is backfilled."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyEmpty,
                    defaultAnnotation,
                    5,
                    defaultAnnotation,
                    "Empty congestion_numeric + default, index = 5. " +
                        "Index out of bounds results in null annotation property"
                ),
                arrayOf(
                    defaultAnnotation,
                    annotationWithCongestionNumericOnlyEmpty,
                    0,
                    defaultAnnotation,
                    "Default + empty congestion_numeric, index = 0. " +
                        "Null old annotation property turns merged property to null."
                ),
                arrayOf(
                    defaultAnnotation,
                    annotationWithCongestionNumericOnlyEmpty,
                    5,
                    defaultAnnotation,
                    "Default + empty congestion_numeric, index = 5. " +
                        "Mismatched sizes result in null merged annotation property."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyEmpty,
                    annotationWithCongestionNumericOnlyEmpty,
                    0,
                    annotationWithCongestionNumericOnlyEmpty,
                    "Empty congestion_numeric x2, index = 0. New annotation property is used.",
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    defaultAnnotation,
                    0,
                    annotationWithCongestionNumericOnlyFilled,
                    "Filled congestion_numeric + default, index = 0. " +
                        "congestion_numeric is backfilled."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    defaultAnnotation,
                    5,
                    annotationWithCongestionNumericOnlyFilled,
                    "Filled congestion_numeric + default, index = 5. " +
                        "Nothing changes since index is the last."
                ),
                arrayOf(
                    defaultAnnotation,
                    LegAnnotation.builder().congestionNumeric(listOf(3, 4, 5)).build(),
                    2,
                    defaultAnnotation,
                    "Default + filled congestion_numeric, index = 2." +
                        "Index is too big: return null annotation."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyEmpty,
                    LegAnnotation.builder().congestionNumeric(listOf(3, 4, 5)).build(),
                    2,
                    defaultAnnotation,
                    "Empty congestion_numeric + filled congestion_numeric, index = 2. " +
                        "Index is too big: return null annotation."
                ),
                arrayOf(
                    LegAnnotation.builder().congestionNumeric(listOf(1)).build(),
                    LegAnnotation.builder().congestionNumeric(listOf(3, 4, 5)).build(),
                    2,
                    defaultAnnotation,
                    "Partially filled congestion_numeric + filled congestion_numeric, index = 2. " +
                        "Index is too big: return null annotation."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    LegAnnotation.builder().congestionNumeric(listOf(6, 7, 8, 9, 10)).build(),
                    0,
                    LegAnnotation.builder().congestionNumeric(listOf(6, 7, 8, 9, 10)).build(),
                    "Filled congestion_numeric x2, index = 0. New annotation property is used."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    LegAnnotation.builder().congestionNumeric(listOf(8, 9, 10)).build(),
                    2,
                    LegAnnotation.builder().congestionNumeric(listOf(1, 2, 8, 9, 10)).build(),
                    "Filled congestion_numeric x2, index = 2. Annotations before current " +
                        "geometry index should be updated to old value."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    LegAnnotation.builder().congestionNumeric(listOf(8, 9, 10, 11)).build(),
                    2,
                    LegAnnotation.builder().congestionNumeric(listOf(1, 2, 8, 9, 10)).build(),
                    "Filled congestion_numeric + too long new annotation, index = 2. " +
                        "Excessive new annotations should be ignored."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    LegAnnotation.builder().congestionNumeric(listOf(8, 9)).build(),
                    2,
                    LegAnnotation.builder().congestionNumeric(listOf(1, 2, 8, 9, 5)).build(),
                    "Filled congestion_numeric + too short new annotation, index = 2. " +
                        "Last items are filled with old values."
                ),
                arrayOf(
                    LegAnnotation.builder().congestion(List(5) { "unknown" }).build(),
                    LegAnnotation.builder().congestion(listOf("low", "severe")).build(),
                    2,
                    LegAnnotation
                        .builder()
                        .congestion(listOf("unknown", "unknown", "low", "severe", "unknown"))
                        .build(),
                    "Congestion filled with default values + " +
                        "update in the middle updates only middle."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    annotationWithCongestionNumericOnlyEmpty,
                    5,
                    annotationWithCongestionNumericOnlyFilled,
                    "Filled congestion_numeric + empty congestion numeric, index = 5. " +
                        "Old annotation property is used before current index."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    annotationWithCongestionNumericOnlyEmpty,
                    6,
                    defaultAnnotation,
                    "Filled congestion_numeric + empty congestion numeric, index = 6. " +
                        "Index out of bounds results in null annotation."
                ),
                arrayOf(
                    LegAnnotation.builder()
                        .congestionNumeric(listOf(1, 2, 3, 4, 5))
                        .congestion(listOf("c1", "c2", "c3", "c4", "c5"))
                        .distance(listOf(1.2, 3.4, 5.6, 7.8, 9.0))
                        .duration(listOf(11.2, 33.4, 55.6, 77.8, 99.0))
                        .speed(listOf(41.0, 42.5, 43.1, 44.6, 45.9))
                        .maxspeed(List(5) { MaxSpeed.builder().speed(it * 10).unit("mph").build() })
                        .build(),
                    LegAnnotation.builder()
                        .congestionNumeric(listOf(6, 7, 8, 9, 10))
                        .congestion(listOf("c6", "c7", "c8", "c9", "c10"))
                        .distance(listOf(2.1, 4.3, 6.5, 8.7, 0.9))
                        .duration(listOf(22.1, 44.3, 66.5, 88.7, 110.9))
                        .speed(listOf(51.0, 52.5, 53.1, 54.6, 55.9))
                        .maxspeed(
                            List(5) {
                                MaxSpeed.builder().speed(it * 10 + 1).unit("kmh").build()
                            }
                        )
                        .build(),
                    0,
                    LegAnnotation.builder()
                        .congestionNumeric(listOf(6, 7, 8, 9, 10))
                        .congestion(listOf("c6", "c7", "c8", "c9", "c10"))
                        .distance(listOf(2.1, 4.3, 6.5, 8.7, 0.9))
                        .duration(listOf(22.1, 44.3, 66.5, 88.7, 110.9))
                        .speed(listOf(51.0, 52.5, 53.1, 54.6, 55.9))
                        .maxspeed(
                            List(5) {
                                MaxSpeed.builder().speed(it * 10 + 1).unit("kmh").build()
                            }
                        )
                        .build(),
                    "Everything is filled, index = 0. New annotation properties are used."
                ),
                arrayOf(
                    LegAnnotation.builder()
                        .congestionNumeric(listOf(1, 2, 3, 4, 5))
                        .congestion(listOf("c1", "c2", "c3", "c4", "c5"))
                        .distance(listOf(1.2, 3.4, 5.6, 7.8, 9.0))
                        .duration(listOf(11.2, 33.4, 55.6, 77.8, 99.0))
                        .speed(listOf(41.0, 42.5, 43.1, 44.6, 45.9))
                        .maxspeed(
                            List(5) {
                                MaxSpeed.builder().speed(it * 10).unit("mph").build()
                            }
                        )
                        .build(),
                    LegAnnotation.builder()
                        .congestionNumeric(listOf(9, 10))
                        .congestion(listOf("c9", "c10"))
                        .distance(listOf(8.7, 0.9))
                        .duration(listOf(88.7, 110.9))
                        .speed(listOf(54.6, 55.9))
                        .maxspeed(
                            List(2) {
                                MaxSpeed.builder().speed(it * 10 + 1).unit("kmh").build()
                            }
                        )
                        .build(),
                    3,
                    LegAnnotation.builder()
                        .congestionNumeric(listOf(1, 2, 3, 9, 10))
                        .congestion(listOf("c1", "c2", "c3", "c9", "c10"))
                        .distance(listOf(1.2, 3.4, 5.6, 8.7, 0.9))
                        .duration(listOf(11.2, 33.4, 55.6, 88.7, 110.9))
                        .speed(listOf(41.0, 42.5, 43.1, 54.6, 55.9))
                        .maxspeed(
                            List(3) {
                                MaxSpeed.builder().speed(it * 10).unit("mph").build()
                            } +
                                List(2) {
                                    MaxSpeed.builder().speed(it * 10 + 1).unit("kmh").build()
                                }
                        )
                        .build(),
                    "Everything is filled, index = 3. " +
                        "Old annotations properties are used before current index."
                ),
                arrayOf(
                    LegAnnotation.builder()
                        .congestionNumeric(listOf(1, 2, 3, 4, 5))
                        .congestion(listOf("c1", "c2", "c3", "c4", "c5"))
                        .distance(listOf(1.2, 3.4, 5.6, 7.8, 9.0))
                        .duration(listOf(11.2, 33.4, 55.6, 77.8, 99.0))
                        .speed(listOf(41.0, 42.5, 43.1, 44.6, 45.9))
                        .maxspeed(
                            List(5) {
                                MaxSpeed.builder().speed(it * 10).unit("mph").build()
                            }
                        )
                        .build(),
                    LegAnnotation.builder()
                        .congestionNumeric(emptyList())
                        .congestion(emptyList())
                        .distance(emptyList())
                        .duration(emptyList())
                        .speed(emptyList())
                        .maxspeed(emptyList())
                        .build(),
                    5,
                    LegAnnotation.builder()
                        .congestionNumeric(listOf(1, 2, 3, 4, 5))
                        .congestion(listOf("c1", "c2", "c3", "c4", "c5"))
                        .distance(listOf(1.2, 3.4, 5.6, 7.8, 9.0))
                        .duration(listOf(11.2, 33.4, 55.6, 77.8, 99.0))
                        .speed(listOf(41.0, 42.5, 43.1, 44.6, 45.9))
                        .maxspeed(
                            List(5) {
                                MaxSpeed.builder().speed(it * 10).unit("mph").build()
                            }
                        )
                        .build(),
                    "Everything is filled, index = 5. " +
                        "Old annotation properties are used before current index."
                ),
                arrayOf(
                    LegAnnotation.builder()
                        .congestionNumeric(listOf(1, 2, 3, 4, 5))
                        .congestion(listOf("c1", "c2", "c3", "c4", "c5"))
                        .distance(listOf(1.2, 3.4, 5.6, 7.8, 9.0))
                        .duration(listOf(11.2, 33.4, 55.6, 77.8, 99.0))
                        .speed(listOf(41.0, 42.5, 43.1, 44.6, 45.9))
                        .maxspeed(
                            List(5) {
                                MaxSpeed.builder().speed(it * 10).unit("mph").build()
                            }
                        )
                        .build(),
                    defaultAnnotation,
                    5,
                    LegAnnotation.builder()
                        .congestionNumeric(listOf(1, 2, 3, 4, 5))
                        .congestion(listOf("c1", "c2", "c3", "c4", "c5"))
                        .distance(listOf(1.2, 3.4, 5.6, 7.8, 9.0))
                        .duration(listOf(11.2, 33.4, 55.6, 77.8, 99.0))
                        .speed(listOf(41.0, 42.5, 43.1, 44.6, 45.9))
                        .maxspeed(
                            List(5) {
                                MaxSpeed.builder().speed(it * 10).unit("mph").build()
                            }
                        )
                        .build(),
                    "Everything is filled + default, index = 5. " +
                        "Old annotation properties are used before current index."
                ),
                arrayOf(
                    LegAnnotation.fromJson("{ \"my_key1\": \"my_value1\" }"),
                    LegAnnotation.fromJson("{ \"my_key2\": \"my_value2\" }"),
                    3,
                    LegAnnotation.fromJson("{ \"my_key2\": \"my_value2\" }"),
                    "Unrecognized properties migrate from new annotation."
                ),
                arrayOf(
                    LegAnnotation.fromJson("{ \"my_key1\": \"my_value1\" }"),
                    LegAnnotation.fromJson(
                        "{ \"my_key2\": \"my_value2\", " +
                            "\"my_key3\": \"my_value3\" }"
                    ),
                    3,
                    LegAnnotation.fromJson(
                        "{ \"my_key2\": \"my_value2\", " +
                            "\"my_key3\": \"my_value3\" }"
                    ),
                    "Old annotation has less unrecognized properties. " +
                        "Unrecognized properties migrate from new annotation."
                ),
                arrayOf(
                    LegAnnotation.builder().build(),
                    LegAnnotation.fromJson(
                        "{ \"my_key2\": \"my_value2\", " +
                            "\"my_key3\": \"my_value3\" }"
                    ),
                    3,
                    LegAnnotation.fromJson(
                        "{ \"my_key2\": \"my_value2\", " +
                            "\"my_key3\": \"my_value3\" }"
                    ),
                    "Old annotation has no unrecognized properties. " +
                        "Unrecognized properties migrate from new annotation."
                ),
                arrayOf(
                    LegAnnotation.fromJson(
                        "{ \"my_key2\": \"my_value2\", " +
                            "\"my_key3\": \"my_value3\" }"
                    ),
                    LegAnnotation.fromJson("{ \"my_key1\": \"my_value1\" }"),
                    3,
                    LegAnnotation.fromJson("{ \"my_key1\": \"my_value1\" }"),
                    "Old annotation has more unrecognized properties. " +
                        "Unrecognized properties migrate from new annotation."
                ),
            )
        }
    }

    @Test
    fun getRefreshedAnnotations() {
        val actual = AnnotationsRefresher.getRefreshedAnnotations(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex
        )
        assertEquals(expectedMergedAnnotation, actual)
    }
}
