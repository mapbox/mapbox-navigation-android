package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.QueriedRenderedFeature
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClosestRouteUtilsTest {

    @Test
    fun getIndexOfFirstFeature_emptyFeatures() {
        val routeFeatures = listOf<FeatureCollection>(
            mockk {
                every { features() } returns listOf(
                    mockk {
                        every { id() } returns null
                    },
                )
            },
        )

        val result = ClosestRouteUtils.getIndexOfFirstFeature(emptyList(), routeFeatures)

        assertTrue(result.isError)
    }

    @Test
    fun getIndexOfFirstFeature_emptyRouteFeatures() {
        val features = listOf<QueriedRenderedFeature>(
            mockk {
                every { queriedFeature } returns mockk {
                    every { feature } returns mockk {
                        every { id() } returns "id#0"
                    }
                }
            },
        )

        val result = ClosestRouteUtils.getIndexOfFirstFeature(features, emptyList())

        assertTrue(result.isError)
    }

    @Test
    fun getIndexOfFirstFeature_everythingIsEmpty() {
        val result = ClosestRouteUtils.getIndexOfFirstFeature(emptyList(), emptyList())

        assertTrue(result.isError)
    }

    @Test
    fun getIndexOfFirstFeature_noMatchFound() {
        val features = listOf<QueriedRenderedFeature>(
            mockk {
                every { queriedFeature } returns mockk {
                    every { feature } returns mockk {
                        every { id() } returns "id#0"
                    }
                }
            },
            mockk {
                every { queriedFeature } returns mockk {
                    every { feature } returns mockk {
                        every { id() } returns "id#1"
                    }
                }
            },
        )
        val routeFeatures = listOf<FeatureCollection>(
            mockk {
                every { features() } returns listOf(
                    mockk {
                        every { id() } returns "id#2"
                    },
                    mockk {
                        every { id() } returns "id#3"
                    },
                )
            },
            mockk {
                every { features() } returns listOf(
                    mockk {
                        every { id() } returns "id#4"
                    },
                    mockk {
                        every { id() } returns "id#5"
                    },
                )
            },
        )

        val result = ClosestRouteUtils.getIndexOfFirstFeature(features, routeFeatures)

        assertTrue(result.isError)
    }

    @Test
    fun getIndexOfFirstFeature_matchFoundButNotFirstFeature() {
        val features = listOf<QueriedRenderedFeature>(
            mockk {
                every { queriedFeature } returns mockk {
                    every { feature } returns mockk {
                        every { id() } returns "id#0"
                    }
                }
            },
            mockk {
                every { queriedFeature } returns mockk {
                    every { feature } returns mockk {
                        every { id() } returns "id#2"
                    }
                }
            },
        )
        val routeFeatures = listOf<FeatureCollection>(
            mockk {
                every { features() } returns listOf(
                    mockk {
                        every { id() } returns "id#2"
                    },
                    mockk {
                        every { id() } returns "id#3"
                    },
                )
            },
            mockk {
                every { features() } returns listOf(
                    mockk {
                        every { id() } returns "id#4"
                    },
                    mockk {
                        every { id() } returns "id#5"
                    },
                )
            },
        )

        val result = ClosestRouteUtils.getIndexOfFirstFeature(features, routeFeatures)

        assertTrue(result.isError)
    }

    @Test
    fun getIndexOfFirstFeature_matchFoundButNotFirstFeatureInRouteFeature() {
        val features = listOf<QueriedRenderedFeature>(
            mockk {
                every { queriedFeature } returns mockk {
                    every { feature } returns mockk {
                        every { id() } returns "id#0"
                    }
                }
            },
            mockk {
                every { queriedFeature } returns mockk {
                    every { feature } returns mockk {
                        every { id() } returns "id#1"
                    }
                }
            },
        )
        val routeFeatures = listOf<FeatureCollection>(
            mockk {
                every { features() } returns listOf(
                    mockk {
                        every { id() } returns "id#2"
                    },
                    mockk {
                        every { id() } returns "id#0"
                    },
                )
            },
            mockk {
                every { features() } returns listOf(
                    mockk {
                        every { id() } returns "id#4"
                    },
                    mockk {
                        every { id() } returns "id#0"
                    },
                )
            },
        )

        val result = ClosestRouteUtils.getIndexOfFirstFeature(features, routeFeatures)

        assertTrue(result.isError)
    }

    @Test
    fun getIndexOfFirstFeature_matchFoundFirstRouteFeature() {
        val features = listOf<QueriedRenderedFeature>(
            mockk {
                every { queriedFeature } returns mockk {
                    every { feature } returns mockk {
                        every { id() } returns "id#0"
                    }
                }
            },
            mockk {
                every { queriedFeature } returns mockk {
                    every { feature } returns mockk {
                        every { id() } returns "id#1"
                    }
                }
            },
        )
        val routeFeatures = listOf<FeatureCollection>(
            mockk {
                every { features() } returns listOf(
                    mockk {
                        every { id() } returns "id#0"
                    },
                    mockk {
                        every { id() } returns "id#3"
                    },
                )
            },
            mockk {
                every { features() } returns listOf(
                    mockk {
                        every { id() } returns "id#4"
                    },
                    mockk {
                        every { id() } returns "id#5"
                    },
                )
            },
        )

        val result = ClosestRouteUtils.getIndexOfFirstFeature(features, routeFeatures)

        assertEquals(0, result.value)
    }

    @Test
    fun getIndexOfFirstFeature_matchFoundSecondRouteFeature() {
        val features = listOf<QueriedRenderedFeature>(
            mockk {
                every { queriedFeature } returns mockk {
                    every { feature } returns mockk {
                        every { id() } returns "id#0"
                    }
                }
            },
            mockk {
                every { queriedFeature } returns mockk {
                    every { feature } returns mockk {
                        every { id() } returns "id#1"
                    }
                }
            },
        )
        val routeFeatures = listOf<FeatureCollection>(
            mockk {
                every { features() } returns listOf(
                    mockk {
                        every { id() } returns "id#2"
                    },
                    mockk {
                        every { id() } returns "id#3"
                    },
                )
            },
            mockk {
                every { features() } returns listOf(
                    mockk {
                        every { id() } returns "id#0"
                    },
                    mockk {
                        every { id() } returns "id#5"
                    },
                )
            },
        )

        val result = ClosestRouteUtils.getIndexOfFirstFeature(features, routeFeatures)

        assertEquals(1, result.value)
    }
}
