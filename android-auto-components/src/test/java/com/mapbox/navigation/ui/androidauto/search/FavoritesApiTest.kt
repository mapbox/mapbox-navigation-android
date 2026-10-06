package com.mapbox.navigation.ui.androidauto.search

import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.internal.search.FavoritesApi
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.common.CompletionCallback
import com.mapbox.search.record.FavoriteRecord
import com.mapbox.search.record.FavoritesDataProvider
import com.mapbox.search.result.NewSearchResultType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class FavoritesApiTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun getPlaces() = coroutineRule.runBlockingTest {
        val expectedItemList = listOf(
            FavoriteRecord(
                id = "id",
                name = "name",
                descriptionText = "description",
                address = null,
                routablePoints = null,
                categories = null,
                makiIcon = null,
                coordinate = Point.fromLngLat(-33.0, -44.0),
                metadata = null,
                NewSearchResultType.POI,
            ),
        )
        val callbackSlot = slot<CompletionCallback<List<FavoriteRecord>>>()
        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { getAll(capture(callbackSlot)) } answers {
                callbackSlot.captured.onComplete(expectedItemList)
                mockk()
            }
        }

        val result = FavoritesApi(mockFavoritesProvider)
            .getPlaces().value

        assertEquals("id", result!!.first().id)
        assertEquals("name", result.first().name)
        assertEquals("description", result.first().description)
        assertEquals(Point.fromLngLat(-33.0, -44.0), result.first().coordinate)
    }

    @Test
    fun getFavorites() = coroutineRule.runBlockingTest {
        val expectedItemList = listOf<FavoriteRecord>()
        val callbackSlot = slot<CompletionCallback<List<FavoriteRecord>>>()
        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { getAll(capture(callbackSlot)) } answers {
                callbackSlot.captured.onComplete(expectedItemList)
                mockk()
            }
        }

        val result = FavoritesApi(mockFavoritesProvider)
            .getFavorites().value!!

        assertEquals(expectedItemList, result)
    }

    @Test
    fun getFavorites_onError() = coroutineRule.runBlockingTest {
        val callbackSlot = slot<CompletionCallback<List<FavoriteRecord>>>()
        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { getAll(capture(callbackSlot)) } answers {
                callbackSlot.captured.onError(RuntimeException("Exception Message"))
                mockk()
            }
        }

        val result = FavoritesApi(mockFavoritesProvider)
            .getFavorites().error

        assertEquals("Exception Message", result!!.errorMessage)
    }

    @Test
    fun addFavorite() = coroutineRule.runBlockingTest {
        val expected = mockk<FavoriteRecord>(relaxed = true)
        val callbackSlot = slot<CompletionCallback<Unit>>()
        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { upsert(expected, capture(callbackSlot)) } answers {
                callbackSlot.captured.onComplete(Unit)
                mockk()
            }
        }

        val result = FavoritesApi(mockFavoritesProvider)
            .addFavorite(expected)

        assertEquals(expected, result.value)
    }

    @Test
    fun removeFavorite() = coroutineRule.runBlockingTest {
        val callbackSlot = slot<CompletionCallback<Boolean>>()
        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { remove("foobar", capture(callbackSlot)) } answers {
                callbackSlot.captured.onComplete(true)
                mockk()
            }
        }

        val result = FavoritesApi(mockFavoritesProvider)
            .removeFavorite("foobar")

        assertTrue(result.value!!)
    }

    @Test
    fun removeFavorite_onError() = coroutineRule.runBlockingTest {
        val callbackSlot = slot<CompletionCallback<Boolean>>()
        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { remove("foobar", capture(callbackSlot)) } answers {
                callbackSlot.captured.onError(RuntimeException())
                mockk()
            }
        }

        val result = FavoritesApi(mockFavoritesProvider).removeFavorite("foobar").error

        assertEquals("Error removing favorite.", result!!.errorMessage)
    }

    @Test
    fun `cancelling the caller cancels its task`() = coroutineRule.runBlockingTest {
        val getAllTask = mockk<AsyncOperationTask>(relaxed = true)
        val upsertTask = mockk<AsyncOperationTask>(relaxed = true)
        val removeTask = mockk<AsyncOperationTask>(relaxed = true)
        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { getAll(any()) } returns getAllTask
            every { upsert(any(), any()) } returns upsertTask
            every { remove(any(), any()) } returns removeTask
        }
        val api = FavoritesApi(mockFavoritesProvider)

        val getJob = launch { api.getFavorites() }
        val addJob = launch { api.addFavorite(mockk(relaxed = true)) }
        val removeJob = launch { api.removeFavorite("foobar") }
        getJob.cancel()
        addJob.cancel()
        removeJob.cancel()

        verify(exactly = 1) { getAllTask.cancel() }
        verify(exactly = 1) { upsertTask.cancel() }
        verify(exactly = 1) { removeTask.cancel() }
    }

    @Test
    fun `concurrent callers each get their own result`() = coroutineRule.runBlockingTest {
        val callbacks = mutableListOf<CompletionCallback<List<FavoriteRecord>>>()
        val tasks = mutableListOf<AsyncOperationTask>()
        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { getAll(capture(callbacks)) } answers {
                mockk<AsyncOperationTask>(relaxed = true).also { tasks.add(it) }
            }
        }
        val api = FavoritesApi(mockFavoritesProvider)

        val first = async { api.getFavorites() }
        val second = async { api.getFavorites() }
        callbacks[1].onComplete(listOf())
        callbacks[0].onError(RuntimeException("first failed"))

        assertEquals("first failed", first.await().error!!.errorMessage)
        assertEquals(listOf<FavoriteRecord>(), second.await().value)
        tasks.forEach { verify(exactly = 0) { it.cancel() } }
    }

    @Test
    fun `deprecated cancel cancels every pending caller and its task`() =
        coroutineRule.runBlockingTest {
            val getAllTask = mockk<AsyncOperationTask>(relaxed = true)
            val removeTask = mockk<AsyncOperationTask>(relaxed = true)
            val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
                every { getAll(any()) } returns getAllTask
                every { remove(any(), any()) } returns removeTask
            }
            val api = FavoritesApi(mockFavoritesProvider)
            val getJob = launch { api.getFavorites() }
            val removeJob = launch { api.removeFavorite("foobar") }

            @Suppress("DEPRECATION")
            api.cancel()

            assertTrue(getJob.isCancelled)
            assertTrue(removeJob.isCancelled)
            verify(exactly = 1) { getAllTask.cancel() }
            verify(exactly = 1) { removeTask.cancel() }
        }

    @Test
    fun `a provider that throws fails the caller and leaves later calls working`() =
        coroutineRule.runBlockingTest {
            val callbackSlot = slot<CompletionCallback<List<FavoriteRecord>>>()
            var throwOnCall = true
            val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
                every { getAll(capture(callbackSlot)) } answers {
                    if (throwOnCall) throw IllegalStateException("not initialized")
                    callbackSlot.captured.onComplete(listOf())
                    mockk()
                }
            }
            val api = FavoritesApi(mockFavoritesProvider)

            val failure = runCatching { api.getFavorites() }.exceptionOrNull()
            throwOnCall = false
            @Suppress("DEPRECATION")
            api.cancel()
            val result = api.getFavorites()

            assertTrue(failure is IllegalStateException)
            assertEquals(listOf<FavoriteRecord>(), result.value)
        }
}
