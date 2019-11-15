package com.mapbox.navigation.base.util

import com.google.gson.Gson

interface JsonMapper {
    fun <T> toJson(obj: T): String

    object GsonImpl : JsonMapper {
        private val gson by lazy { Gson() }

        override fun <T> toJson(obj: T): String = gson.toJson(obj)
    }
}
