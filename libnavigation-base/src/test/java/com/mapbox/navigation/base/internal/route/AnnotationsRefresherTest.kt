package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.MaxSpeed
import org.junit.Assert.assertEquals
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
                    "Everything is null"
                ),
                arrayOf(
                    defaultAnnotation,
                    null,
                    0,
                    null,
                    "New annotation is null"
                ),
                arrayOf(
                    defaultAnnotation,
                    defaultAnnotation,
                    0,
                    defaultAnnotation,
                    "2 default annotations, index = 0"
                ),
                arrayOf(
                    defaultAnnotation,
                    defaultAnnotation,
                    5,
                    defaultAnnotation,
                    "2 default annotations, index = 5"
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyEmpty,
                    defaultAnnotation,
                    0,
                    defaultAnnotation,
                    "Empty congestion_numeric + default, index = 0"
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyEmpty,
                    defaultAnnotation,
                    0,
                    defaultAnnotation,
                    "Empty congestion_numeric + default, index = 5"
                ),
                arrayOf(
                    defaultAnnotation,
                    annotationWithCongestionNumericOnlyEmpty,
                    0,
                    annotationWithCongestionNumericOnlyEmpty,
                    "Default + empty congestion_numeric, index = 0"
                ),
                arrayOf(
                    defaultAnnotation,
                    annotationWithCongestionNumericOnlyEmpty,
                    5,
                    LegAnnotation.builder().congestionNumeric(listOf(0, 0, 0, 0, 0)).build(),
                    "Default + empty congestion_numeric, index = 5. Annotations before current " +
                        "geometry index should be updated to default value."
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyEmpty,
                    annotationWithCongestionNumericOnlyEmpty,
                    0,
                    annotationWithCongestionNumericOnlyEmpty,
                    "Empty congestion_numeric x2, index = 0",
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    defaultAnnotation,
                    0,
                    defaultAnnotation,
                    "Filled congestion_numeric + default, index = 0"
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    defaultAnnotation,
                    5,
                    defaultAnnotation,
                    "Filled congestion_numeric + default, index = 5"
                ),
                arrayOf(
                    defaultAnnotation,
                    LegAnnotation.builder().congestionNumeric(listOf(3, 4, 5)).build(),
                    2,
                    LegAnnotation.builder().congestionNumeric(listOf(0, 0, 3, 4, 5)).build(),
                    "Default + filled congestion_numeric, index = 2"
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyEmpty,
                    LegAnnotation.builder().congestionNumeric(listOf(3, 4, 5)).build(),
                    2,
                    LegAnnotation.builder().congestionNumeric(listOf(0, 0, 3, 4, 5)).build(),
                    "Empty congestion_numeric + filled congestion_numeric, index = 2"
                ),
                arrayOf(
                    LegAnnotation.builder().congestionNumeric(listOf(1)).build(),
                    LegAnnotation.builder().congestionNumeric(listOf(3, 4, 5)).build(),
                    2,
                    LegAnnotation.builder().congestionNumeric(listOf(1, 0, 3, 4, 5)).build(),
                    "Partially filled congestion_numeric + filled congestion_numeric, index = 2"
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    LegAnnotation.builder().congestionNumeric(listOf(6, 7, 8, 9, 10)).build(),
                    0,
                    LegAnnotation.builder().congestionNumeric(listOf(6, 7, 8, 9, 10)).build(),
                    "Filled congestion_numeric x2, index = 0"
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    LegAnnotation.builder().congestionNumeric(listOf(8, 9, 10)).build(),
                    2,
                    LegAnnotation.builder().congestionNumeric(listOf(1, 2, 8, 9, 10)).build(),
                    "Filled congestion_numeric x2, index = 2"
                ),
                arrayOf(
                    annotationWithCongestionNumericOnlyFilled,
                    annotationWithCongestionNumericOnlyEmpty,
                    5,
                    annotationWithCongestionNumericOnlyFilled,
                    "Filled congestion_numeric + empty congestion numeric, index = 5"
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
                    "Everything is filled, index = 0"
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
                    "Everything is filled, index = 3"
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
                    "Everything is filled, index = 5"
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
                    defaultAnnotation,
                    "Everything is filled + default, index = 5"
                ),
                arrayOf(
                    LegAnnotation.fromJson("{ \"my_key1\": \"my_value1\" }"),
                    LegAnnotation.fromJson("{ \"my_key2\": \"my_value2\" }"),
                    3,
                    LegAnnotation.fromJson("{ \"my_key2\": \"my_value2\" }"),
                    "Unrecognized properties migrate from new annotation"
                )
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
