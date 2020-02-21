package com.mapbox.navigation.core.location.replay

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

internal class TimestampAdapter : TypeAdapter<Date>() {

    companion object {
        private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        private val DATE_FORMAT = SimpleDateFormat(DATE_FORMAT_PATTERN)
        private const val UTC = "UTC"
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Date?) {
        if (value == null) {
            out.nullValue()
        } else {
            DATE_FORMAT.timeZone = TimeZone.getTimeZone(
                UTC
            )
            val date = DATE_FORMAT.format(value)
            out.value(date)
        }
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): Date? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        val dateAsString = reader.nextString()
        try {
            return DATE_FORMAT.parse(dateAsString)
        } catch (exception: ParseException) {
            exception.printStackTrace()
        }

        return null
    }
}
