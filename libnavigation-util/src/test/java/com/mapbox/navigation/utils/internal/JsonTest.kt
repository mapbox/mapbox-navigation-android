package com.mapbox.navigation.utils.internal

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JsonTest {

    @Test
    fun getExistingJsonObject() {
        val json = JSONObject(JSON_WITH_OBJECT)
        json.getOrPutJsonObject(OBJECT_NAME)
        assertEquals(json.toString(), JSONObject(JSON_WITH_OBJECT).toString())
    }

    @Test
    fun getNonExistingJsonObject() {
        val json = JSONObject(EMPTY_JSON_OBJECT)
        json.getOrPutJsonObject(OBJECT_NAME)
        assertEquals(json.toString(), JSONObject(JSON_WITH_OBJECT).toString())
    }

    private companion object {
        const val EMPTY_JSON_OBJECT = "{}"

        const val JSON_WITH_OBJECT = """
            {
                "foo": {
                }
            }
        """

        const val OBJECT_NAME = "foo"
    }
}
