package com.mapbox.services.android.navigation.v5.internal.navigation

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.core.os.ConfigurationCompat
import java.io.IOException
import java.io.RandomAccessFile
import java.util.regex.Pattern
import timber.log.Timber

internal object MetadataBuilder {

    private const val RANDOM_ACCESS_FILE_NAME = "/proc/meminfo"
    private const val READ_MODE = "r"
    private val OPERATING_SYSTEM = "Android - ${Build.VERSION.RELEASE}"
    private val DEVICE = Build.DEVICE
    private val MANUFACTURER = Build.MANUFACTURER
    private val BRAND = Build.BRAND
    private val ABI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Build.SUPPORTED_ABIS[0]
    } else {
        Build.CPU_ABI
    }
    private val VERSION = Build.VERSION.SDK_INT.toString()

    private var metadata: NavigationPerformanceMetadata? = null

    fun getMetadata(context: Context): NavigationPerformanceMetadata =
        metadata ?: NavigationPerformanceMetadata.builder()
            .version(VERSION)
            .screenSize(getScreenSize(context))
            .country(getCountry(context))
            .device(DEVICE)
            .abi(ABI)
            .brand(BRAND)
            .ram(getTotalMemory(context))
            .os(OPERATING_SYSTEM)
            .gpu("")
            .manufacturer(MANUFACTURER)
            .build()
            .also { metadata = it }

    private fun getTotalMemory(context: Context): String {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager =
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        activityManager.getMemoryInfo(memoryInfo)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            (memoryInfo.totalMem / (1024 * 1024)).toString()
        } else {
            getTotalMemorySize()
        }
    }

    private fun getCountry(context: Context): String =
        ConfigurationCompat.getLocales(context.resources.configuration)[0].country

    private fun getScreenSize(context: Context): String {
        val displayMetrics = context.resources.displayMetrics
        return "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
    }

    private fun getTotalMemorySize(): String {
        val reader: RandomAccessFile
        val load: String
        return try {
            reader = RandomAccessFile(
                RANDOM_ACCESS_FILE_NAME,
                READ_MODE
            )
            load = reader.readLine()
            val pattern = Pattern.compile("(\\d+)")
            val matcher = pattern.matcher(load)
            var value = ""
            while (matcher.find()) {
                value = matcher.group(1)
            }
            reader.close()
            (value.toLong() / 1024).toString()
        } catch (ex: IOException) {
            Timber.e("Failing to access RandomAccessFile $RANDOM_ACCESS_FILE_NAME")
            ""
        }
    }
}
