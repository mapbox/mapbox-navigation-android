package com.mapbox.navigation.ui.shield.internal.loader

import com.mapbox.bindgen.ExpectedFactory.createValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CachedResourceLoaderTest {

    private lateinit var loader: ResourceLoader<Int, String>
    private lateinit var sut: CachedResourceLoader<Int, String>

    @Before
    fun setUp() {
        loader = mockk {
            coEvery { load(any()) } coAnswers {
                createValue(firstArg<Int>().toString())
            }
        }
        sut = CachedResourceLoader(2, loader)
    }

    @Test
    fun `should load non-cached value`() = runBlockingTest {
        val r = sut.load(1)

        coVerify(exactly = 1) { loader.load(1) }
        assertEquals("1", r.value)
    }

    @Test
    fun `should return cached value`() = runBlockingTest {
        val results = listOf(
            sut.load(1),
            sut.load(1)
        )

        coVerify(exactly = 1) { loader.load(1) }
        assertEquals(listOf("1", "1"), results.map { it.value })
    }
}
