package com.mapbox.services.android.navigation.v5.navigation

import io.mockk.every
import io.mockk.mockk
import java.util.HashMap
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class TarResponseErrorMapTest {

    @Test
    fun buildErrorMessage_402messageIsCreated() {
        val errorCodes = HashMap<Int, String>()
        val response = mockk<Response<ResponseBody>>()
        every { response.code() } returns 402
        val errorMap = TarResponseErrorMap(errorCodes)

        val errorMessage = errorMap.buildErrorMessageWith(response)

        assertTrue(errorMessage.contains("Please contact us at support@mapbox.com"))
    }

    @Test
    fun buildErrorMessage_messageIsCreatedForCodeNotFound() {
        val errorCodes = HashMap<Int, String>()
        val response = mockk<Response<ResponseBody>>()
        every { response.code() } returns 100
        every { response.message() } returns "Some error message"
        val errorMap = TarResponseErrorMap(errorCodes)

        val errorMessage = errorMap.buildErrorMessageWith(response)

        assertEquals("Error code 100: Some error message", errorMessage)
    }
}
