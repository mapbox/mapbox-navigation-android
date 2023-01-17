package com.mapbox.navigation.ui.shield.internal.loader

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceDownloaderTest {

    @Test
    fun `load should call download method and return its value`() = runBlockingTest {
        val sut = object : Downloader<Int, String>() {
            override suspend fun download(input: Int): Expected<Error, String> =
                createValue("$input")
        }
        val r = sut.load(1234)

        assertEquals("1234", r.value)
    }

    @Test
    fun `load should call download only once`() =
        runBlockingTest {
            val stateFlow = MutableStateFlow<String?>(null)
            val downloadCalls = mutableListOf<Int>()
            val sut = object : Downloader<Int, String>() {
                override suspend fun download(input: Int): Expected<Error, String> {
                    downloadCalls.add(input)
                    val v = stateFlow.filter { it == "$input" }.filterNotNull().first()
                    return createValue(v)
                }
            }

            val loaded = mutableListOf<String?>()
            val j = launch {
                launch { loaded.add(sut.load(1).value) }
                launch { loaded.add(sut.load(1).value) }
            }
            stateFlow.value = "1"
            j.join()

            assertEquals(1, downloadCalls.size)
        }

    @Test
    fun `load should notify all callbacks when the value has been downloaded`() =
        runBlockingTest {
            // Simulate scenario where resource (1) has been requested multiple times and takes longer
            // to load than resource (2).
            //
            // |-------------(1)------(1)-----(2)-----------------[2]-[1]------->
            // resource 1:      {-------------- loading 1 -----------}
            // resource 2:                       {-- loading 2 --}
            //
            val stateFlow = MutableStateFlow<String?>(null)
            val sut = object : Downloader<Int, String>() {
                override suspend fun download(input: Int): Expected<Error, String> {
                    val v = stateFlow.filter { it == "$input" }.filterNotNull().first()
                    return createValue(v)
                }
            }

            val loaded = mutableListOf<String?>()
            val j = launch {
                launch { loaded.add(sut.load(1).value) }
                launch { loaded.add(sut.load(1).value) }
                launch { loaded.add(sut.load(2).value) }
            }
            stateFlow.value = "2" // simulate faster load time of the resource (2)
            stateFlow.value = "1"
            j.join()

            assertEquals(listOf("2", "1", "1"), loaded)
        }
}
