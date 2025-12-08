package com.mapbox.api.directions.v5.models.utils

import com.google.flatbuffers.FlexBuffers
import com.google.flatbuffers.Table
import com.google.flatbuffers.offset
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.utils.internal.logE
import java.nio.ByteBuffer

internal interface BaseFBWrapper {

    val unrecognized: ByteBuffer?

    val unrecognizedPropertiesLength: Int

    val unrecognizeFlexBufferMap: FlexBuffers.Map?
        get() {
            if (unrecognizedPropertiesLength <= 0) {
                // workaround for https://github.com/google/flatbuffers/issues/8691
                return null
            }
            unrecognized ?: return null
            val flexBuffers = FlexBuffers.getRoot(unrecognized)
            return if (!flexBuffers.isMap) {
                logE(FB_LOG_CATEGORY) {
                    "Unrecognized properties are not a map, type = ${flexBuffers.type}"
                }
                null
            } else {
                flexBuffers.asMap()
            }
        }

    fun unrecognizedMap(): Map<String, Any?>? {
        return unrecognizeFlexBufferMap?.run {
            traverseFlexMap(this)
        }
    }

    fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return unrecognizeFlexBufferMap?.run {
            traverseFlexMapToJsonElements(this)
                ?.filter { it.value != null }
                ?.mapValues {
                    SerializableJsonElement(it.value)
                }
        }
    }

    fun efficientEquals(a: Table, b: Table): Boolean {
        return a.byteBuffer === b.byteBuffer && a.offset == b.offset
    }

    fun efficientHashCode(table: Table): Int {
        return System.identityHashCode(table.byteBuffer) * 31 + table.offset.hashCode()
    }

    private fun mapReference(value: FlexBuffers.Reference): Any? {
        return when {
            // Important: don't change order,
            // Some checks are too broad, i.e. map is also a vector, float is also an int, etc.
            value.isMap -> traverseFlexMap(value.asMap())
            value.isVector || value.isTypedVector -> {
                val vector = value.asVector()
                FlatbuffersListWrapper.get(vector) {
                    mapReference(vector.get(it))
                }
            }

            value.isBoolean -> value.asBoolean()
            value.isInt -> value.asInt()
            value.isUInt -> value.asUInt()
            value.isString -> value.asString()
            value.isFloat -> value.asFloat()
            value.isBlob -> value.asBlob().bytes
            value.isNull -> null
            value.isKey -> {
                logE(FB_LOG_CATEGORY) {
                    "Unrecognized properties value is a key"
                }
                null
            }

            else -> {
                logE(FB_LOG_CATEGORY) {
                    "Unknown unrecognized properties value type: ${value.type}"
                }
                null
            }
        }
    }

    private fun traverseFlexMap(flexMap: FlexBuffers.Map?): Map<String, Any?>? {
        flexMap ?: return null
        return FlatbuffersMapWrapper.get(flexMap) {
            object : Map.Entry<String, Any?> {
                override val key: String = flexMap.keys().get(it).toString()
                override val value: Any? = mapReference(flexMap.get(it))
            }
        }
    }

    private fun mapReferenceToJsonElement(value: FlexBuffers.Reference): JsonElement? {
        return when {
            // Important: don't change order,
            // Some checks are too broad, i.e. map is also a vector, float is also an int, etc.
            value.isMap -> traverseFlexMapToJsonElements(value.asMap())?.run {
                val jsonObject = JsonObject()
                forEach { (key, value) ->
                    jsonObject.add(key, value)
                }
                jsonObject
            }

            value.isVector || value.isTypedVector -> {
                val array = JsonArray()
                val v = value.asVector()
                (0 until v.size()).forEach {
                    array.add(mapReferenceToJsonElement(v.get(it)))
                }
                return array
            }

            value.isBoolean -> JsonPrimitive(value.asBoolean())
            value.isInt -> JsonPrimitive(value.asInt())
            value.isUInt -> JsonPrimitive(value.asUInt())
            value.isString -> JsonPrimitive(value.asString())
            value.isFloat -> JsonPrimitive(value.asFloat())
            value.isBlob -> {
                val array = JsonArray()
                val v = value.asBlob()
                (0 until v.size()).forEach {
                    array.add(JsonPrimitive(v.get(it)))
                }
                return array
            }

            value.isNull -> null
            value.isKey -> {
                logE(FB_LOG_CATEGORY) {
                    "Unrecognized properties value is a key"
                }
                null
            }

            else -> {
                logE(FB_LOG_CATEGORY) {
                    "Unknown unrecognized properties value type: ${value.type}"
                }
                null
            }
        }
    }

    private fun traverseFlexMapToJsonElements(
        flexMap: FlexBuffers.Map?,
    ): Map<String, JsonElement?>? {
        flexMap ?: return null
        return FlatbuffersMapWrapper.get(flexMap) {
            object : Map.Entry<String, JsonElement?> {
                override val key: String = flexMap.keys().get(it).toString()
                override val value: JsonElement? = mapReferenceToJsonElement(flexMap.get(it))
            }
        }
    }
}
