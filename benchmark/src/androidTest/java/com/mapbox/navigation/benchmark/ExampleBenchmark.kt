package com.mapbox.navigation.benchmark

import android.os.Looper
import android.util.Log
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun do_nothing() { // 1.8 ns
        benchmarkRule.measureRepeated {
        }
    }
    @Test
    fun compare_different_loopers() { // 17.9 ns
        benchmarkRule.measureRepeated {
            Looper.myLooper() == Looper.getMainLooper()
        }
    }

    @Test
    fun compare_same_loopers() { // 26.6 ns
        benchmarkRule.measureRepeated {
            Looper.getMainLooper() == Looper.getMainLooper()
        }
    }

    @Test
    fun compare_lists() { // 149  ns
        val list1 = MutableList(10) { it }
        val list2 = MutableList(10) { it }
        benchmarkRule.measureRepeated {
             list1 == list2
        }
    }


}