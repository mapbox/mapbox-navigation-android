package com.mapbox.navigation.ui.shield

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.core.module.CommonSingletonModuleProvider
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.shield.model.MapboxRouteShieldOptions
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import com.mapbox.navigation.ui.shield.model.RouteShieldToDownload
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.*


@ExperimentalCoroutinesApi
class RoadShieldContentManagerTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val accessToken = "pk.1234"
    private val manager = RoadShieldContentManager(MapboxRouteShieldOptions.Builder().build())

    private val testBanners = listOf<BannerInstructions>(
        mockk {
            every { primary() } returns mockk {
                every { components() } returns listOf(
                    mockk {
                        every { type() } returns BannerComponents.ICON
                        every { text() } returns "I 880"
                        every { imageBaseUrl() } returns "https://shield.mapbox.com/880"
                    }
                )
            }
            every { secondary() } returns null
            every { sub() } returns null
        },
        mockk {
            every { primary() } returns mockk {
                every { components() } returns listOf(
                    mockk {
                        every { type() } returns BannerComponents.ICON
                        every { text() } returns "I 680"
                        every { imageBaseUrl() } returns "https://shield.mapbox.com/680"
                    },
                    mockk {
                        every { type() } returns BannerComponents.ICON
                        every { text() } returns "I 280"
                        every { imageBaseUrl() } returns "https://shield.mapbox.com/280"
                    }
                )
            }
            every { secondary() } returns null
            every { sub() } returns null
        },
        mockk {
            every { primary() } returns mockk {
                every { components() } returns listOf(
                    mockk {
                        every { type() } returns BannerComponents.TEXT
                        every { text() } returns "Lincoln Av"
                    }
                )
            }
            every { secondary() } returns mockk {
                every { components() } returns listOf(
                    mockk {
                        every { type() } returns BannerComponents.ICON
                        every { text() } returns "I 480"
                        every { imageBaseUrl() } returns "https://shield.mapbox.com/480"
                    }
                )
            }
            every { sub() } returns null
        },
        mockk {
            every { primary() } returns mockk {
                every { components() } returns listOf(
                    mockk {
                        every { type() } returns BannerComponents.TEXT
                        every { text() } returns "Bascom Av"
                    }
                )
            }
            every { secondary() } returns null
            every { sub() } returns mockk {
                every { components() } returns listOf(
                    mockk {
                        every { type() } returns BannerComponents.ICON
                        every { text() } returns "I 180"
                        every { imageBaseUrl() } returns "https://shield.mapbox.com/180"
                    }
                )
            }
        }
    )

    @Before
    fun setup() {
        mockkObject(RoadShieldDownloader)
    }

    @After
    fun tearDown() {
        unmockkObject(RoadShieldDownloader)
    }

    @Test
    fun `empty lists returned if indices empty`() = coroutineRule.runBlockingTest {
        val expected = RouteShieldResult(
            shields = emptyList(),
            errors = emptyList(),
        )

        var actual: RouteShieldResult? = null
        pauseDispatcher {
            actual = manager.getShields(
                accessToken = accessToken,
                fallbackToLegacy = true,
                fallbackToGeneric = true,
                emptyList()
            )
        }

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `eager - empty lists returned if indices empty`() = coroutineRule.runBlockingTest {
        val expected = RouteShieldResult(
            shields = emptyList(),
            errors = emptyList(),
        )

        val actual = manager.getShields(
            accessToken = accessToken,
            fallbackToLegacy = true,
            fallbackToGeneric = true,
            emptyList()
        )

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `all shields for single maneuver are returned`() = coroutineRule.runBlockingTest {
        val testBanner = testBanners[0]
        val routeShieldToDownload = mutableListOf<RouteShieldToDownload>()
        val primaryUrl = testBanner.primary().components()!!.findLegacyUrl()!!
        routeShieldToDownload.add(
            RouteShieldToDownload(
                "I 880", primaryUrl, null, null, null
            )
        )
        val primaryByteArray = byteArrayOf(1)
        coEvery {
            RoadShieldDownloader.downloadImage(primaryUrl.plus(".svg"))
        } returns ExpectedFactory.createValue(primaryByteArray)

        /*val secondaryUrl = testBanner.secondary()!!.components()!!.findLegacyUrl()!!
        val secondaryByteArray = byteArrayOf(2)
        coEvery {
            RoadShieldDownloader.downloadImage(secondaryUrl)
        } returns ExpectedFactory.createValue(secondaryByteArray)

        val subUrl = testBanner.sub()!!.components()!!.findLegacyUrl()!!
        val subByteArray = byteArrayOf(3)
        coEvery {
            RoadShieldDownloader.downloadImage(subUrl)
        } returns ExpectedFactory.createValue(subByteArray)

        val expected = RouteShieldResult(
            shields = listOf(
                RouteShield.MapboxLegacyShield(
                    shield = primaryByteArray,
                    errorMessage = "",
                    url = primaryUrl
                )
            ),
            errors = emptyList(),
        )

        var actual: RouteShieldResult? = null
        pauseDispatcher {
            actual = manager.getShields(
                accessToken = accessToken,
                fallbackToLegacy = true,
                fallbackToGeneric = true,
                routeShieldToDownload
            )
        }

        Assert.assertEquals(expected, actual)*/
    }

    /*@Test
    fun `eager - all shields for single maneuver are returned`() = coroutineRule.runBlockingTest {
        val testManeuver = testManeuvers[3]
        val primaryUrl = testManeuver.primary.componentList.findShieldUrl()!!
        val primaryByteArray = byteArrayOf(1)
        coEvery {
            RoadShieldDownloader.downloadImage(primaryUrl)
        } returns ExpectedFactory.createValue(primaryByteArray)

        val secondaryUrl = testManeuver.secondary!!.componentList.findShieldUrl()!!
        val secondaryByteArray = byteArrayOf(2)
        coEvery {
            RoadShieldDownloader.downloadImage(secondaryUrl)
        } returns ExpectedFactory.createValue(secondaryByteArray)

        val subUrl = testManeuver.sub!!.componentList.findShieldUrl()!!
        val subByteArray = byteArrayOf(3)
        coEvery {
            RoadShieldDownloader.downloadImage(subUrl)
        } returns ExpectedFactory.createValue(subByteArray)

        val expected = RoadShieldResult(
            shields = hashMapOf(
                "primary_3" to RoadShield(primaryUrl, primaryByteArray),
                "secondary_3" to RoadShield(secondaryUrl, secondaryByteArray),
                "sub_3" to RoadShield(subUrl, subByteArray)
            ),
            errors = emptyMap(),
        )

        val actual = manager.getShields(listOf(testManeuvers[3]))

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `null url returns an empty shield mapping`() = coroutineRule.runBlockingTest {
        val expected = RoadShieldResult(
            shields = hashMapOf(
                "primary_4" to null,
                "sub_4" to null
            ),
            errors = emptyMap(),
        )

        var actual: RoadShieldResult? = null
        pauseDispatcher {
            actual = manager.getShields(listOf(testManeuvers[4]))
        }

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `eager - null url returns an empty shield mapping`() = coroutineRule.runBlockingTest {
        val expected = RoadShieldResult(
            shields = hashMapOf(
                "primary_4" to null,
                "sub_4" to null
            ),
            errors = emptyMap(),
        )

        val actual = manager.getShields(listOf(testManeuvers[4]))

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `download failure reports error`() = coroutineRule.runBlockingTest {
        val testManeuver = testManeuvers[0]
        val primaryUrl = testManeuver.primary.componentList.findShieldUrl()!!
        coEvery {
            RoadShieldDownloader.downloadImage(primaryUrl)
        } returns ExpectedFactory.createError("some_error")

        val expected = RoadShieldResult(
            shields = hashMapOf(),
            errors = hashMapOf(
                "primary_0" to RoadShieldError(primaryUrl, "some_error")
            ),
        )

        var actual: RoadShieldResult? = null
        pauseDispatcher {
            actual = manager.getShields(listOf(testManeuvers[0]))
        }

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `eager - download failure reports error`() = coroutineRule.runBlockingTest {
        val testManeuver = testManeuvers[0]
        val primaryUrl = testManeuver.primary.componentList.findShieldUrl()!!
        coEvery {
            RoadShieldDownloader.downloadImage(primaryUrl)
        } returns ExpectedFactory.createError("some_error")

        val expected = RoadShieldResult(
            shields = hashMapOf(),
            errors = hashMapOf(
                "primary_0" to RoadShieldError(primaryUrl, "some_error")
            ),
        )

        val actual = manager.getShields(listOf(testManeuvers[0]))

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `download failure can successfully be re-tried`() = coroutineRule.runBlockingTest {
        val testManeuver = testManeuvers[0]
        val primaryUrl = testManeuver.primary.componentList.findShieldUrl()!!
        coEvery {
            RoadShieldDownloader.downloadImage(primaryUrl)
        } returns ExpectedFactory.createError("some_error")
        pauseDispatcher {
            manager.getShields(listOf(testManeuvers[0]))
        }
        val primaryByteArray = byteArrayOf(1)
        coEvery {
            RoadShieldDownloader.downloadImage(primaryUrl)
        } returns ExpectedFactory.createValue(primaryByteArray)

        val expected = RoadShieldResult(
            shields = hashMapOf(
                "primary_0" to RoadShield(primaryUrl, primaryByteArray)
            ),
            errors = hashMapOf(),
        )

        var actual: RoadShieldResult? = null
        pauseDispatcher {
            actual = manager.getShields(listOf(testManeuvers[0]))
        }

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `eager - download failure can successfully be re-tried`() = coroutineRule.runBlockingTest {
        val testManeuver = testManeuvers[0]
        val primaryUrl = testManeuver.primary.componentList.findShieldUrl()!!
        coEvery {
            RoadShieldDownloader.downloadImage(primaryUrl)
        } returns ExpectedFactory.createError("some_error")
        manager.getShields(listOf(testManeuvers[0]))
        val primaryByteArray = byteArrayOf(1)
        coEvery {
            RoadShieldDownloader.downloadImage(primaryUrl)
        } returns ExpectedFactory.createValue(primaryByteArray)

        val expected = RoadShieldResult(
            shields = hashMapOf(
                "primary_0" to RoadShield(primaryUrl, primaryByteArray)
            ),
            errors = hashMapOf(),
        )

        val actual = manager.getShields(listOf(testManeuvers[0]))

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `when banner successfully prepared before, do not re-download shields`() =
        coroutineRule.runBlockingTest {
            val testManeuver = testManeuvers[0]
            val primaryUrl = testManeuver.primary.componentList.findShieldUrl()!!
            val primaryByteArray = byteArrayOf(1)
            coEvery {
                RoadShieldDownloader.downloadImage(primaryUrl)
            } returns ExpectedFactory.createValue(primaryByteArray)

            pauseDispatcher {
                launch {
                    manager.getShields(listOf(testManeuvers[0]))
                }

                launch {
                    manager.getShields(listOf(testManeuvers[0]))
                }
            }

            coVerify(exactly = 1) { RoadShieldDownloader.downloadImage(primaryUrl) }
        }

    @Test
    fun `eager - when banner successfully prepared before, do not re-download shields`() =
        coroutineRule.runBlockingTest {
            val testManeuver = testManeuvers[0]
            val primaryUrl = testManeuver.primary.componentList.findShieldUrl()!!
            val primaryByteArray = byteArrayOf(1)
            coEvery {
                RoadShieldDownloader.downloadImage(primaryUrl)
            } returns ExpectedFactory.createValue(primaryByteArray)

            manager.getShields(listOf(testManeuvers[0]))
            manager.getShields(listOf(testManeuvers[0]))

            coVerify(exactly = 1) { RoadShieldDownloader.downloadImage(primaryUrl) }
        }

    @Test
    fun `when shield is available, do not re-download`() = coroutineRule.runBlockingTest {
        val testManeuver = testManeuvers[0]
        val primaryUrl = testManeuver.primary.componentList.findShieldUrl()!!
        val primaryByteArray = byteArrayOf(1)
        coEvery {
            RoadShieldDownloader.downloadImage(primaryUrl)
        } returns ExpectedFactory.createValue(primaryByteArray)

        // both primary banners have the same URL here
        pauseDispatcher {
            launch {
                manager.getShields(listOf(testManeuvers[0]))
            }
            launch {
                manager.getShields(listOf(testManeuvers[5]))
            }
        }

        coVerify(exactly = 1) { RoadShieldDownloader.downloadImage(primaryUrl) }
    }

    @Test
    fun `eager - when shield is available, do not re-download`() = coroutineRule.runBlockingTest {
        val testManeuver = testManeuvers[0]
        val primaryUrl = testManeuver.primary.componentList.findShieldUrl()!!
        val primaryByteArray = byteArrayOf(1)
        coEvery {
            RoadShieldDownloader.downloadImage(primaryUrl)
        } returns ExpectedFactory.createValue(primaryByteArray)

        // both primary banners have the same URL here
        manager.getShields(listOf(testManeuvers[0]))
        manager.getShields(listOf(testManeuvers[5]))

        coVerify(exactly = 1) { RoadShieldDownloader.downloadImage(primaryUrl) }
    }

    @Test
    fun `when same maneuver is requested twice at the same time, download shields only once`() =
        coroutineRule.runBlockingTest {
            val testManeuver1 = testManeuvers[0]
            val primaryUrl = testManeuver1.primary.componentList.findShieldUrl()!!
            val primaryByteArray = byteArrayOf(1)
            coEvery {
                RoadShieldDownloader.downloadImage(primaryUrl)
            } returns ExpectedFactory.createValue(primaryByteArray)

            val testManeuver2 = testManeuvers[2]
            val secondaryUrl = testManeuver2.secondary!!.componentList.findShieldUrl()!!
            val secondaryByteArray = byteArrayOf(2)
            coEvery {
                RoadShieldDownloader.downloadImage(secondaryUrl)
            } returns ExpectedFactory.createValue(secondaryByteArray)

            val expected1 = RoadShieldResult(
                shields = hashMapOf(
                    "primary_0" to RoadShield(primaryUrl, primaryByteArray)
                ),
                errors = hashMapOf(),
            )
            val expected2 = RoadShieldResult(
                shields = hashMapOf(
                    "primary_0" to RoadShield(primaryUrl, primaryByteArray),
                    "primary_2" to null,
                    "secondary_2" to RoadShield(secondaryUrl, secondaryByteArray),
                ),
                errors = hashMapOf(),
            )

            var actual1: RoadShieldResult? = null
            var actual2: RoadShieldResult? = null
            pauseDispatcher {
                launch {
                    pauseDispatcher {
                        actual1 = manager.getShields(listOf(testManeuvers[0]))
                    }
                }
                launch {
                    pauseDispatcher {
                        actual2 = manager.getShields(listOf(testManeuvers[0], testManeuvers[2]))
                    }
                }
            }

            Assert.assertEquals(expected1, actual1)
            Assert.assertEquals(expected2, actual2)
            coVerify(exactly = 1) { RoadShieldDownloader.downloadImage(primaryUrl) }
            coVerify(exactly = 1) { RoadShieldDownloader.downloadImage(secondaryUrl) }
        }

    @Test
    fun `shields for requested banners are downloaded in parallel`() =
        coroutineRule.runBlockingTest {
            val testManeuver1 = testManeuvers[0]
            val primaryUrl = testManeuver1.primary.componentList.findShieldUrl()!!
            val primaryByteArray = byteArrayOf(1)
            coEvery {
                RoadShieldDownloader.downloadImage(primaryUrl)
            } coAnswers {
                delay(1000L)
                ExpectedFactory.createValue(primaryByteArray)
            }

            val testManeuver2 = testManeuvers[2]
            val secondaryUrl = testManeuver2.secondary!!.componentList.findShieldUrl()!!
            val secondaryByteArray = byteArrayOf(2)
            coEvery {
                RoadShieldDownloader.downloadImage(secondaryUrl)
            } coAnswers {
                delay(1000L)
                ExpectedFactory.createValue(secondaryByteArray)
            }

            val expected = RoadShieldResult(
                shields = hashMapOf(
                    "primary_0" to RoadShield(primaryUrl, primaryByteArray),
                    "primary_2" to null,
                    "secondary_2" to RoadShield(secondaryUrl, secondaryByteArray),
                ),
                errors = hashMapOf(),
            )

            var actual: RoadShieldResult? = null

            pauseDispatcher {
                launch {
                    actual = manager.getShields(listOf(testManeuvers[0], testManeuvers[2]))
                }
                // run coroutines until they hit the download delays
                runCurrent()
                // advance by less then a sum of both download delays above which is equal to 2s
                advanceTimeBy(1500)

                Assert.assertEquals(
                    "shields didn't manage to be downloaded in time which means that " +
                        "downloads didn't run in parallel",
                    expected,
                    actual
                )
                coVerify(exactly = 1) { RoadShieldDownloader.downloadImage(primaryUrl) }
                coVerify(exactly = 1) { RoadShieldDownloader.downloadImage(secondaryUrl) }
            }
        }

    @Test
    fun `shields that didn't manage to get downloaded before cancellation report correctly`() =
        coroutineRule.runBlockingTest {
            val testManeuver1 = testManeuvers[0]
            val primaryUrl = testManeuver1.primary.componentList.findShieldUrl()!!
            val primaryByteArray = byteArrayOf(1)
            coEvery {
                RoadShieldDownloader.downloadImage(primaryUrl)
            } coAnswers {
                delay(2000L)
                ExpectedFactory.createValue(primaryByteArray)
            }

            val testManeuver2 = testManeuvers[2]
            val secondaryUrl = testManeuver2.secondary!!.componentList.findShieldUrl()!!
            val secondaryByteArray = byteArrayOf(2)
            coEvery {
                RoadShieldDownloader.downloadImage(secondaryUrl)
            } coAnswers {
                delay(1000L)
                ExpectedFactory.createValue(secondaryByteArray)
            }

            val expected = RoadShieldResult(
                shields = hashMapOf(
                    "primary_2" to null,
                    "secondary_2" to RoadShield(secondaryUrl, secondaryByteArray),
                ),
                errors = hashMapOf(
                    "primary_0" to RoadShieldError(primaryUrl, CANCELED_MESSAGE)
                ),
            )

            var actual: RoadShieldResult? = null

            pauseDispatcher {
                val job = launch {
                    actual = manager.getShields(listOf(testManeuvers[0], testManeuvers[2]))
                }
                // run coroutines until they hit the download delays
                runCurrent()
                // advance by enough to only download one shield
                advanceTimeBy(1500)

                manager.cancelAll()
                job.cancel()
                job.join()
            }

            Assert.assertEquals(expected, actual)
        }

    @Test
    fun `canceled callback is released`() =
        coroutineRule.runBlockingTest {
            val testManeuver1 = testManeuvers[0]
            val primaryUrl = testManeuver1.primary.componentList.findShieldUrl()!!
            val primaryByteArray = byteArrayOf(1)
            coEvery {
                RoadShieldDownloader.downloadImage(primaryUrl)
            } coAnswers {
                delay(2000L)
                ExpectedFactory.createValue(primaryByteArray)
            }

            val testManeuver2 = testManeuvers[2]
            val secondaryUrl = testManeuver2.secondary!!.componentList.findShieldUrl()!!
            val secondaryByteArray = byteArrayOf(2)
            coEvery {
                RoadShieldDownloader.downloadImage(secondaryUrl)
            } coAnswers {
                delay(1000L)
                ExpectedFactory.createValue(secondaryByteArray)
            }

            val expected = RoadShieldResult(
                shields = hashMapOf(
                    "primary_2" to null,
                    "secondary_2" to RoadShield(secondaryUrl, secondaryByteArray),
                ),
                errors = hashMapOf(
                    "primary_0" to RoadShieldError(primaryUrl, CANCELED_MESSAGE)
                ),
            )

            var actual: RoadShieldResult? = null

            pauseDispatcher {
                val job = launch {
                    actual = manager.getShields(listOf(testManeuvers[0], testManeuvers[2]))
                }
                // run coroutines until they hit the download delays
                runCurrent()
                // advance by enough to only download one shield
                advanceTimeBy(1500)

                manager.cancelAll()
                job.cancel()
                job.join()
                Assert.assertEquals(expected, actual)

                launch {
                    // this coroutine would throw an exception when finished if there was
                    // a lingering callback reference in the content manager
                    actual = manager.getShields(listOf(testManeuvers[0], testManeuvers[2]))
                }
            }
        }

    @Test
    fun `canceled shield download can be successfully re-tried`() =
        coroutineRule.runBlockingTest {
            val testManeuver1 = testManeuvers[0]
            val primaryUrl = testManeuver1.primary.componentList.findShieldUrl()!!
            val primaryByteArray = byteArrayOf(1)
            coEvery {
                RoadShieldDownloader.downloadImage(primaryUrl)
            } coAnswers {
                delay(2000L)
                ExpectedFactory.createValue(primaryByteArray)
            }

            val testManeuver2 = testManeuvers[2]
            val secondaryUrl = testManeuver2.secondary!!.componentList.findShieldUrl()!!
            val secondaryByteArray = byteArrayOf(2)
            coEvery {
                RoadShieldDownloader.downloadImage(secondaryUrl)
            } coAnswers {
                delay(1000L)
                ExpectedFactory.createValue(secondaryByteArray)
            }

            val expected = RoadShieldResult(
                shields = hashMapOf(
                    "primary_0" to RoadShield(primaryUrl, primaryByteArray),
                    "primary_2" to null,
                    "secondary_2" to RoadShield(secondaryUrl, secondaryByteArray),
                ),
                errors = hashMapOf()
            )

            var actual: RoadShieldResult? = null
            pauseDispatcher {
                val job = launch {
                    manager.getShields(listOf(testManeuvers[0], testManeuvers[2]))
                }
                // run coroutines until they hit the download delays
                runCurrent()
                // advance by enough to only download one shield
                advanceTimeBy(1500)

                manager.cancelAll()
                job.cancel()
                job.join()
            }

            pauseDispatcher {
                actual = manager.getShields(listOf(testManeuvers[0], testManeuvers[2]))
            }
            Assert.assertEquals(expected, actual)
        }*/

    private fun List<BannerComponents>.findLegacyUrl(): String? {
        val shieldComponent = this.find { it.type() == BannerComponents.ICON }
        return shieldComponent?.imageBaseUrl()
    }
}
