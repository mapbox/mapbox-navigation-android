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
    fun getFavorites_cancelsAsyncTask() = coroutineRule.runBlockingTest {
        val expectedAsyncOperationTask = mockk<AsyncOperationTask>(relaxed = true)
        val expectedItemList = listOf<FavoriteRecord>()
        val callbackSlot = slot<CompletionCallback<List<FavoriteRecord>>>()
        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { getAll(capture(callbackSlot)) } coAnswers {
                callbackSlot.captured.onComplete(expectedItemList)
                expectedAsyncOperationTask
            }
        }
        val api = FavoritesApi(mockFavoritesProvider)

        api.getFavorites()
        api.getFavorites()

        verify { expectedAsyncOperationTask.cancel() }
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
    fun addFavorite_cancelsAsyncTask() = coroutineRule.runBlockingTest {
        val favoriteRecord = mockk<FavoriteRecord>(relaxed = true)
        val expectedAsyncOperationTask = mockk<AsyncOperationTask>(relaxed = true)
        val callbackSlot = slot<CompletionCallback<Unit>>()

        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { upsert(favoriteRecord, capture(callbackSlot)) } answers {
                callbackSlot.captured.onComplete(Unit)
                expectedAsyncOperationTask
            }
        }
        val api = FavoritesApi(mockFavoritesProvider)

        api.addFavorite(favoriteRecord)
        api.addFavorite(favoriteRecord)

        verify { expectedAsyncOperationTask.cancel() }
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
    fun removeFavorite_cancelsAsyncTask() = coroutineRule.runBlockingTest {
        val expectedAsyncOperationTask = mockk<AsyncOperationTask>(relaxed = true)
        val callbackSlot = slot<CompletionCallback<Boolean>>()
        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { remove("foobar", capture(callbackSlot)) } answers {
                callbackSlot.captured.onComplete(true)
                expectedAsyncOperationTask
            }
        }
        val api = FavoritesApi(mockFavoritesProvider)

        api.removeFavorite("foobar")
        api.removeFavorite("foobar")

        verify { expectedAsyncOperationTask.cancel() }
    }

    @Test
    fun cancelRequests() = coroutineRule.runBlockingTest {
        val favoriteRecord = mockk<FavoriteRecord>(relaxed = true)
        val getAllTask = mockk<AsyncOperationTask>(relaxed = true)
        val addFavoriteTask = mockk<AsyncOperationTask>(relaxed = true)
        val removeFavoriteTask = mockk<AsyncOperationTask>(relaxed = true)
        val removeCallbackSlot = slot<CompletionCallback<Boolean>>()
        val addCallbackSlot = slot<CompletionCallback<Unit>>()
        val getAllCallbackSlot = slot<CompletionCallback<List<FavoriteRecord>>>()
        val mockFavoritesProvider = mockk<FavoritesDataProvider>(relaxed = true) {
            every { remove("foobar", capture(removeCallbackSlot)) } coAnswers {
                removeCallbackSlot.captured.onComplete(true)
                removeFavoriteTask
            }
            every { upsert(any(), capture(addCallbackSlot)) } coAnswers {
                addCallbackSlot.captured.onComplete(Unit)
                getAllTask
            }
            every { getAll(capture(getAllCallbackSlot)) } coAnswers {
                getAllCallbackSlot.captured.onComplete(listOf())
                addFavoriteTask
            }
        }
        val api = FavoritesApi(mockFavoritesProvider)
        api.removeFavorite("foobar")
        api.removeFavorite("foobar")
        api.addFavorite(favoriteRecord)
        api.addFavorite(favoriteRecord)
        api.getFavorites()
        api.getFavorites()

        api.cancel()

        verify { removeFavoriteTask.cancel() }
        verify { addFavoriteTask.cancel() }
        verify { getAllTask.cancel() }
    }
}
