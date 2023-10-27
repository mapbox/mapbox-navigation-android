package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
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
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(79, 78, 77, 76, 75)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("a", "b", "c", "d", "e")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
                        .congestionNumeric(listOf(1, 2, 3, 4, 5))
                        .congestion(listOf("c1", "c2", "c3", "c4", "c5"))
                        .distance(listOf(1.2, 3.4, 5.6, 7.8, 9.0))
                        .duration(listOf(11.2, 33.4, 55.6, 77.8, 99.0))
                        .speed(listOf(41.0, 42.5, 43.1, 44.6, 45.9))
                        .freeflowSpeed(listOf(1, 2, 3, 4, 5))
                        .currentSpeed(listOf(2, 3, 4, 5, 6))
                        .maxspeed(List(5) { MaxSpeed.builder().speed(it * 10).unit("mph").build() })
                        .build(),
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(70, 69, 68, 67, 66)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("f", "g", "h", "i", "j")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
                        .congestionNumeric(listOf(6, 7, 8, 9, 10))
                        .congestion(listOf("c6", "c7", "c8", "c9", "c10"))
                        .distance(listOf(2.1, 4.3, 6.5, 8.7, 0.9))
                        .duration(listOf(22.1, 44.3, 66.5, 88.7, 110.9))
                        .speed(listOf(51.0, 52.5, 53.1, 54.6, 55.9))
                        .freeflowSpeed(listOf(5, 4, 3, 2, 1))
                        .currentSpeed(listOf(6, 5, 4, 3, 2))
                        .maxspeed(
                            List(5) {
                                MaxSpeed.builder().speed(it * 10 + 1).unit("kmh").build()
                            }
                        )
                        .build(),
                    0,
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(70, 69, 68, 67, 66)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("f", "g", "h", "i", "j")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
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
                        .freeflowSpeed(listOf(5, 4, 3, 2, 1))
                        .currentSpeed(listOf(6, 5, 4, 3, 2))
                        .build(),
                    "Everything is filled, index = 0. New annotation properties are used."
                ),
                arrayOf(
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(79, 78, 77, 76, 75)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("a", "b", "c", "d", "e")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
                        .congestionNumeric(listOf(1, 2, 3, 4, 5))
                        .congestion(listOf("c1", "c2", "c3", "c4", "c5"))
                        .distance(listOf(1.2, 3.4, 5.6, 7.8, 9.0))
                        .duration(listOf(11.2, 33.4, 55.6, 77.8, 99.0))
                        .speed(listOf(41.0, 42.5, 43.1, 44.6, 45.9))
                        .freeflowSpeed(listOf(1, 2, 3, 4, 5))
                        .currentSpeed(listOf(2, 3, 4, 5, 6))
                        .maxspeed(
                            List(5) {
                                MaxSpeed.builder().speed(it * 10).unit("mph").build()
                            }
                        )
                        .build(),
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(67, 66)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("i", "j")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
                        .congestionNumeric(listOf(9, 10))
                        .congestion(listOf("c9", "c10"))
                        .distance(listOf(8.7, 0.9))
                        .duration(listOf(88.7, 110.9))
                        .speed(listOf(54.6, 55.9))
                        .freeflowSpeed(listOf(11, 22))
                        .currentSpeed(listOf(33, 44))
                        .maxspeed(
                            List(2) {
                                MaxSpeed.builder().speed(it * 10 + 1).unit("kmh").build()
                            }
                        )
                        .build(),
                    3,
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(79, 78, 77, 67, 66)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("a", "b", "c", "i", "j")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
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
                        .freeflowSpeed(listOf(1, 2, 3, 11, 22))
                        .currentSpeed(listOf(2, 3, 4, 33, 44))
                        .build(),
                    "Everything is filled, index = 3. " +
                        "Old annotations properties are used before current index."
                ),
                arrayOf(
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(79, 78, 77, 76, 75)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("a", "b", "c", "d", "e")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
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
                        .unrecognizedJsonProperties(emptyMap())
                        .congestionNumeric(emptyList())
                        .congestion(emptyList())
                        .distance(emptyList())
                        .duration(emptyList())
                        .speed(emptyList())
                        .maxspeed(emptyList())
                        .freeflowSpeed(emptyList())
                        .currentSpeed(emptyList())
                        .build(),
                    5,
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(79, 78, 77, 76, 75)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("a", "b", "c", "d", "e")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
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
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(79, 78, 77, 76, 75)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("a", "b", "c", "d", "e")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
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
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(79, 78, 77, 76, 75)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("a", "b", "c", "d", "e")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
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
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to JsonPrimitive("aaa"),
                                "custom_annotation" to JsonPrimitive("bbb"),
                            )
                        )
                        .build(),
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(79, 78, 77, 76, 75)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("a", "b", "c", "d", "e")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
                        .build(),
                    0,
                    LegAnnotation.builder().build(),
                    "Non list old annotation unrecognized properties are ignored."
                ),
                arrayOf(
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(79, 78, 77, 76, 75)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("a", "b", "c", "d", "e")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
                        .build(),
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to JsonPrimitive("aaa"),
                                "custom_annotation" to JsonPrimitive("bbb"),
                            )
                        )
                        .build(),
                    5,
                    LegAnnotation.builder().build(),
                    "Non list new annotation unrecognized properties are ignored."
                ),
                arrayOf(
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(10, 11, 12, 13, 14)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("z", "y", "x", "w", "v")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
                        .build(),
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge2" to listOf(79, 78, 77, 76, 75)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation2" to listOf("a", "b", "c", "d", "e")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
                        .build(),
                    3,
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(10, 11, 12, 13, 14)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("z", "y", "x", "w", "v")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
                        .build(),
                    "Unrecognized properties keys don't match: old values are used."
                ),
                arrayOf(
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(10, 11, 12, 13, 14)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("z", "y", "x", "w", "v")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
                        .build(),
                    null,
                    3,
                    LegAnnotation.builder()
                        .unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to listOf(10, 11, 12, 13, 14)
                                    .toJsonArray(::JsonPrimitive),
                                "custom_annotation" to listOf("z", "y", "x", "w", "v")
                                    .toJsonArray(::JsonPrimitive),
                            )
                        )
                        .build(),
                    "Unrecognized properties migrate from old annotation if new annotation is null"
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

private fun <T> List<T>.toJsonArray(toJsonElement: (T) -> JsonElement): JsonArray =
    JsonArray().also { array -> forEach { array.add(toJsonElement(it)) } }
