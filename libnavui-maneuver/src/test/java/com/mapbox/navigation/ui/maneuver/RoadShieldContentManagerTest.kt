package com.mapbox.navigation.ui.maneuver

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maneuver.RoadShieldContentManager.Companion.CANCELED_MESSAGE
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.RoadShieldError
import com.mapbox.navigation.ui.maneuver.model.RoadShieldResult
import com.mapbox.navigation.ui.maneuver.model.TextComponentNode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The internal logic of [RoadShieldContentManager] requires us to have a very precise control over
 * timing of coroutines so you'll see some paused dispatchers and granular timing advancement.
 * You can read more about the technics used in
 * [this guide](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/).
 *
 * Normally the `runBlockingTest` executes all coroutines eagerly, meaning that whenever a coroutine is launched, it's immediately executed.
 * However, in a production environment that coroutine would most-likely be added back to the message queue which significantly impacts the logic.
 * That's why this class uses the `pauseDispatcher` when needed to retain that type of the flow.
 * Where appropriate, each test has 2 variants, one eager and one regular to make sure that we can handle both.
 *
 * Another important aspect is download delays introduce in some of the test to verify that we're not downloading things twice,
 * that the queues are built correctly, and that cancellation works.
 * To be able to control the elapsed time, we need to wrap invocations in another coroutine which is done in some of the tests.
 */
@ExperimentalCoroutinesApi
class RoadShieldContentManagerTest {
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val manager = RoadShieldContentManager()

    private val testManeuvers = listOf<Maneuver>(
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_0"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/primary_0"
                        }
                    }
                )
            }
            every { secondary } returns null
            every { sub } returns null
        },
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_1"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<TextComponentNode>()
                    }
                )
            }
            every { secondary } returns null
            every { sub } returns null
        },
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_2"
                every { componentList } returns listOf()
            }
            every { secondary } returns mockk {
                every { id } returns "secondary_2"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/secondary_2"
                        }
                    }
                )
            }
            every { sub } returns null
        },
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_3"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<TextComponentNode>()
                    },
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/primary_3"
                        }
                    }
                )
            }
            every { secondary } returns mockk {
                every { id } returns "secondary_3"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/secondary_3"
                        }
                    }
                )
            }
            every { sub } returns mockk {
                every { id } returns "sub_3"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/sub_3"
                        }
                    }
                )
            }
        },
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_4"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns null
                        }
                    }
                )
            }
            every { secondary } returns null
            every { sub } returns mockk {
                every { id } returns "sub_4"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns null
                        }
                    }
                )
            }
        },
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_5"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            // same as primary_0 intentionally
                            every { shieldUrl } returns "https://shield.mapbox.com/primary_0"
                        }
                    }
                )
            }
            every { secondary } returns null
            every { sub } returns null
        }
    )

    @Before
    fun setup() {
        mockkObject(RoadShieldDownloader)
    }

    @Test
    fun `empty lists returned if indices empty`() = coroutineRule.runBlockingTest {
        val expected = RoadShieldResult(
            shields = emptyMap(),
            errors = emptyMap(),
        )

        var actual: RoadShieldResult? = null
        pauseDispatcher {
            actual = manager.getShields(emptyList())
        }

        assertEquals(expected, actual)
    }

    @Test
    fun `eager - empty lists returned if indices empty`() = coroutineRule.runBlockingTest {
        val expected = RoadShieldResult(
            shields = emptyMap(),
            errors = emptyMap(),
        )

        val actual = manager.getShields(emptyList())

        assertEquals(expected, actual)
    }

    @Test
    fun `all shields for single maneuver are returned`() = coroutineRule.runBlockingTest {
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

        var actual: RoadShieldResult? = null
        pauseDispatcher {
            actual = manager.getShields(listOf(testManeuvers[3]))
        }

        assertEquals(expected, actual)
    }

    @Test
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

        assertEquals(expected, actual)
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

        assertEquals(expected, actual)
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

        assertEquals(expected, actual)
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

        assertEquals(expected, actual)
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

        assertEquals(expected, actual)
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

        assertEquals(expected, actual)
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

        assertEquals(expected, actual)
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

            assertEquals(expected1, actual1)
            assertEquals(expected2, actual2)
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

                assertEquals(
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

            assertEquals(expected, actual)
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
                assertEquals(expected, actual)

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
            assertEquals(expected, actual)
        }

    @After
    fun tearDown() {
        unmockkObject(RoadShieldDownloader)
    }
}
