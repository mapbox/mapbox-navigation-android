package com.mapbox.services.android.navigation.v5.location.replay

import android.location.Location
import android.os.AsyncTask

import org.xml.sax.SAXException

import java.io.IOException
import java.io.InputStream
import java.text.ParseException

import javax.xml.parsers.ParserConfigurationException

internal class ParseGpxTask(private val parser: GpxParser, private val listener: Listener) : AsyncTask<InputStream, Void, List<Location>>() {

    override fun doInBackground(vararg inputStreams: InputStream): List<Location>? {
        val inputStream = inputStreams[FIRST_INPUT_STREAM]
        try {
            return parseGpxStream(inputStream)
        } catch (exception: IOException) {
            listener.onParseError(exception)
            return null
        }

    }

    override fun onPostExecute(locationList: List<Location>?) {
        if (locationList != null && !locationList.isEmpty()) {
            listener.onParseComplete(locationList)
        } else {
            listener.onParseError(RuntimeException("An error occurred parsing the GPX Xml."))
        }
    }

    @Throws(IOException::class)
    private fun parseGpxStream(inputStream: InputStream): List<Location>? {
        try {
            return parser.parseGpx(inputStream)
        } catch (exception: ParserConfigurationException) {
            exception.printStackTrace()
            listener.onParseError(exception)
            return null
        } catch (exception: ParseException) {
            exception.printStackTrace()
            listener.onParseError(exception)
            return null
        } catch (exception: SAXException) {
            exception.printStackTrace()
            listener.onParseError(exception)
            return null
        } catch (exception: IOException) {
            exception.printStackTrace()
            listener.onParseError(exception)
            return null
        } finally {
            inputStream.close()
        }
    }

    interface Listener {

        fun onParseComplete(gpxLocationList: List<Location>)

        fun onParseError(exception: Exception)
    }

    companion object {

        private val FIRST_INPUT_STREAM = 0
    }
}
