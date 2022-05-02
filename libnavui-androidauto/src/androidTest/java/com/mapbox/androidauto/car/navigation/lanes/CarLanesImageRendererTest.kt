package com.mapbox.androidauto.car.navigation.lanes

import android.Manifest
import android.graphics.Color
import androidx.test.filters.SmallTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.GrantPermissionRule
import com.mapbox.androidauto.testing.BitmapTestUtil
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@SmallTest
class CarLanesImageRendererTest {

    @Rule
    @JvmField
    var testName = TestName()

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val bitmapTestUtils = BitmapTestUtil(
        "expected_lanes_images",
        "test_lanes_images"
    )

    private val carLanesImageGenerator = CarLanesImageRenderer(
        context = bitmapTestUtils.carDisplayContext(),
        background = Color.RED
    )

    @Test
    fun one_lane_uturn() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = mockk {
                every { allLanes } returns listOf(
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "uturn"
                        every { isActive } returns true
                        every { directions } returns listOf("uturn")
                    }
                )
            }
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun two_lanes_straight_sharp_left_straight() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = mockk {
                every { allLanes } returns listOf(
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "sharp left"
                        every { isActive } returns true
                        every { directions } returns listOf("sharp left")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "sharp left"
                        every { isActive } returns false
                        every { directions } returns listOf("straight")
                    }
                )
            }
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun three_lanes_straight_left_straight_right() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = mockk {
                every { allLanes } returns listOf(
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "straight"
                        every { isActive } returns true
                        every { directions } returns listOf("straight", "left")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "straight"
                        every { isActive } returns true
                        every { directions } returns listOf("straight")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "straight"
                        every { isActive } returns false
                        every { directions } returns listOf("right")
                    }
                )
            }
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun four_lanes_right_left_straight_right() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = mockk {
                every { allLanes } returns listOf(
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "right"
                        every { isActive } returns false
                        every { directions } returns listOf("straight", "left")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "right"
                        every { isActive } returns false
                        every { directions } returns listOf("straight")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "right"
                        every { isActive } returns true
                        every { directions } returns listOf("straight", "right")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "right"
                        every { isActive } returns true
                        every { directions } returns listOf("right")
                    }
                )
            }
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun five_lanes_straight_various() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = mockk {
                every { allLanes } returns listOf(
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "straight"
                        every { isActive } returns true
                        every { directions } returns listOf("straight", "left")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "straight"
                        every { isActive } returns true
                        every { directions } returns listOf("straight", "slight left")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "straight"
                        every { isActive } returns true
                        every { directions } returns listOf("straight", "slight right")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "straight"
                        every { isActive } returns false
                        every { directions } returns listOf("right")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "straight"
                        every { isActive } returns false
                        every { directions } returns listOf("sharp right")
                    }
                )
            }
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun six_lanes_left_various() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = mockk {
                every { allLanes } returns listOf(
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns null
                        every { isActive } returns false
                        every { directions } returns listOf("uturn")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns false
                        every { directions } returns listOf("straight", "left")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns false
                        every { directions } returns listOf("straight", "slight left")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns false
                        every { directions } returns listOf("straight", "slight right")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns true
                        every { directions } returns listOf("right")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns false
                        every { directions } returns listOf("sharp right")
                    }
                )
            }
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun seven_lanes_left_various() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = mockk {
                every { allLanes } returns listOf(
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns false
                        every { directions } returns listOf("straight")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns false
                        every { directions } returns listOf("straight")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns false
                        every { directions } returns listOf("straight")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns false
                        every { directions } returns listOf("straight", "slight left")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns true
                        every { directions } returns listOf("slight right")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns true
                        every { directions } returns listOf("right")
                    },
                    mockk {
                        every { drivingSide } returns "right"
                        every { activeDirection } returns "left"
                        every { isActive } returns true
                        every { directions } returns listOf("sharp right")
                    }
                )
            }
        )

        // Fail successfully for now. Handle more images with scaling
//        writeFileSample(carLanesImage!!)
        assertNotNull(carLanesImage)
    }
}
