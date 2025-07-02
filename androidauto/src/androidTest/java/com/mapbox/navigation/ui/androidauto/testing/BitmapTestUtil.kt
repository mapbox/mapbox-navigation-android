package com.mapbox.navigation.ui.androidauto.testing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import org.junit.Assert.fail
import org.junit.rules.TestName
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.IntBuffer
import kotlin.math.abs
import kotlin.math.max

/**
 * The instrumentation tests generate bitmap images.
 *
 * 1. When you're creating images that need to be tested, use the [writeBitmapFile]
 * and then see your bitmaps in the Device File Explorer.
 * Android Studio > View > Tool Windows > Device File Explorer
 * Find your bitmaps in storage > self > Download > mapbox_test
 *
 *   Download files, example:
 *     View the results on the device
 *       adb shell "cd sdcard/Download/mapbox_test && ls"
 *     Pull the results onto your desktop
 *       adb pull sdcard/Download/mapbox_test my-local-folder
 *
 * 2. When you're ready push bitmaps and keep code verified by bitmap images. Copy the
 * sample images into an [expectedAssetsDirectoryName] and then verify your tests
 * with the [assertBitmapsSimilar] function.
 *
 * @param expectedAssetsDirectoryName directory in the assets folder that contains
 *    expected bitmap images. Each bitmap file is named after the unit test name.
 * @param samplesDirectoryName directory to store each test's bitmap for each run.
 */
class BitmapTestUtil(
    private val expectedAssetsDirectoryName: String,
    private val samplesDirectoryName: String,
) {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mapboxTestDirectoryName = "mapbox_test"
    private val directory: File =
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            mapboxTestDirectoryName,
        )
    private val deviceTestDirectory = File(directory, samplesDirectoryName)

    /**
     * When testing bitmaps for the car. Use a specific car display so all systems
     * are consistent.
     */
    fun carDisplayContext(): Context {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val carDisplay = displayManager.createCarDisplay()
        return context.createDisplayContext(carDisplay.display)
    }

    /**
     * Reads a bitmap for the current [testName] in the [expectedAssetsDirectoryName] directory.
     * If they are similar, nothing happens. If they are not equal an assertion is thrown.
     *
     * Similarity is used to handle fragmentation across devices. For example, aliasing
     * algorithms can differ for emulators and actual devices which creates false negatives.
     */
    fun assertBitmapsSimilar(testName: TestName, actual: Bitmap) {
        val filename = testName.methodName + ".png"
        val expectedBitmapFile = expectedAssetsDirectoryName + File.separator + filename
        val expected = try {
            context.assets.open(expectedBitmapFile).use {
                BitmapFactory.decodeStream(it)
            }
        } catch (ignored: IOException) {
            null
        }
        if (expected != null) {
            val difference = calculateDifference(expected, actual)
            // If the images are different, write them to a file so they can be uploaded for
            // debugging.
            if (difference.similarity > 0.01) {
                writeBitmapFile(testName, actual)
                writeBitmapFile(
                    "${testName.methodName}-diff",
                    difference.difference,
                )
                fail(
                    "The ${testName.methodName} image failed with similarity: " +
                        "${difference.similarity}",
                )
            }
        } else {
            writeBitmapFile(testName, actual)
        }
    }

    /**
     * Create a human viewable image of the difference between the images.
     */
    private fun calculateDifference(expected: Bitmap, actual: Bitmap): BitmapDifference {
        val expectedPixels = getSingleImagePixels(expected)
        val actualPixels = getSingleImagePixels(actual)
        val differencePixels = differencePixels(expectedPixels, actualPixels)
        val similarity = calculateSimilarity(differencePixels)
        differencePixels.enhancePixelDifferences()
        val width = max(expected.width, actual.width)
        val height = max(expected.height, actual.height)
        val difference = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        difference.copyPixelsFromBuffer(IntBuffer.wrap(differencePixels))
        return BitmapDifference(
            expected = expected,
            actual = actual,
            difference = difference,
            similarity = similarity,
        )
    }

    /**
     * Determine how similar images are.
     *  Returns 0.0 when the images are identical.
     *  Returns 0.6 when 60% of the pixels are different.
     *  Returns 1.0 when every pixel is different.
     */
    private fun calculateSimilarity(differencePixels: IntArray): Double {
        val total = differencePixels.size.toDouble()
        return differencePixels
            .filterNot { it == Color.TRANSPARENT }
            .fold(0.0) { acc: Double, pixelDiff: Int ->
                acc + calculateSimilarity(pixelDiff).toDouble() / total
            }
    }

    private fun calculateSimilarity(pixelDiff: Int): Float {
        val color = Color.valueOf(pixelDiff)
        return color.components.maxOrNull() ?: 0.0f
    }

    /**
     * Given two image arrays, return an image with a delta of the images.
     */
    private fun differencePixels(expected: IntArray, actual: IntArray): IntArray {
        val diff = IntArray(max(expected.size, actual.size))
        for (col in diff.indices) {
            val expectedPixel = expected.getOrNull(col)
            val actualPixel = actual.getOrNull(col)
            diff[col] = if (expectedPixel == null || actualPixel == null) {
                Color.WHITE
            } else {
                differencePixel(expectedPixel, actualPixel)
            }
        }
        return diff
    }

    /**
     * If the pixel has any differences, set the alpha channel to 1.0 so
     * we can easily see what the image difference is.
     */
    private fun IntArray.enhancePixelDifferences() = apply {
        forEachIndexed { index, i ->
            if (i != Color.TRANSPARENT) {
                val differenceColor = Color.valueOf(i)
                val similarity = calculateSimilarity(i)
                this[index] = Color.argb(
                    1.0f,
                    differenceColor.red(),
                    differenceColor.green(),
                    differenceColor.blue(),
                )
            }
        }
    }

    /**
     * Given two pixels, calculate the difference for each color component and create a new
     * color that we can see in an image that identifies pixel differences.
     */
    private fun differencePixel(expectedPixel: Int, actualPixel: Int): Int {
        val expectedColor = Color.valueOf(expectedPixel)
        val actualColor = Color.valueOf(actualPixel)
        return Color.argb(
            differencePercentage(expectedColor, actualColor, 0),
            differencePercentage(expectedColor, actualColor, 1),
            differencePercentage(expectedColor, actualColor, 2),
            differencePercentage(expectedColor, actualColor, 3),
        )
    }

    /**
     * The difference percentage for a color component. A full difference will result in 1,
     * no difference will be 0.0.
     *
     * For example: If we are expecting a yellow color but get a blue color. The red component in
     * yellow is missing, so the red component will be 1.0 because there is 100% difference. The
     * blue component in yellow is not different, so it will be 0.0.
     */
    private fun differencePercentage(expected: Color, actual: Color, component: Int): Float {
        if (expected.colorSpace != actual.colorSpace) return 1.0f
        val difference = abs(expected.getComponent(component) - actual.getComponent(component))
        val minValue = expected.colorSpace.getMinValue(component)
        val maxValue = expected.colorSpace.getMaxValue(component)
        return (difference - minValue) / (maxValue - minValue)
    }

    /**
     * This is called automatically by [assertBitmapsSimilar] so you can easily see the
     * test results on your device or emulator. When you're building a new test,
     * you can use this function to create the expected images.
     */
    fun writeBitmapFile(testName: TestName, lanesImageBitmap: Bitmap) {
        writeBitmapFile(testName.methodName, lanesImageBitmap)
    }

    private fun writeBitmapFile(methodName: String, lanesImageBitmap: Bitmap) {
        val outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaStoreOutputStream(samplesDirectoryName, "$methodName.png")
        } else {
            deviceTestDirectory.mkdirs()
            val testFile = File(deviceTestDirectory, "$methodName.png")
            testFile.outputStream()
        }
        outputStream.use { out ->
            lanesImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    private fun mediaStoreOutputStream(filePath: String, fileName: String): OutputStream {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/$mapboxTestDirectoryName/$filePath",
            )
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        return resolver.openOutputStream(uri!!)!!
    }

    private fun getSingleImagePixels(bitmap: Bitmap) =
        IntArray(bitmap.width * bitmap.height).also { pixels ->
            bitmap.getPixels(
                pixels,
                0,
                bitmap.width,
                0,
                0,
                bitmap.width,
                bitmap.height,
            )
        }

    private companion object {
        private const val CAR_DISPLAY_NAME = "MapboxCarTest"
        private const val CAR_DISPLAY_WIDTH_PX = 400
        private const val CAR_DISPLAY_HEIGHT_PX = 800
        private const val CAR_DISPLAY_DPI = 160
        private fun DisplayManager.createCarDisplay(): VirtualDisplay =
            createVirtualDisplay(
                CAR_DISPLAY_NAME,
                CAR_DISPLAY_WIDTH_PX,
                CAR_DISPLAY_HEIGHT_PX,
                CAR_DISPLAY_DPI,
                null,
                0,
            )
    }
}

/**
 * Used for calculating if images are similar.
 *
 * @param expected a previously saved bitmap that is expected
 * @param actual the bitmap that was rendered in the test
 * @param difference bitmap delta from expected.i - actual.i
 * @param similarity approximation value of their difference, [0.0-1.0] where 0.0 is identical.
 */
data class BitmapDifference(
    val expected: Bitmap,
    val actual: Bitmap,
    val difference: Bitmap,
    val similarity: Double,
)
