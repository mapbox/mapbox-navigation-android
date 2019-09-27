package com.mapbox.services.android.navigation.v5.location.replay

import android.location.Location

import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException

import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.TimeZone

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

internal class GpxParser {

    private val dateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN)

    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class, ParseException::class)
    fun parseGpx(inputStream: InputStream): List<Location>? {

        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        val documentBuilder = documentBuilderFactory.newDocumentBuilder()
        val document = documentBuilder.parse(inputStream)
        val elementRoot = document.documentElement

        val trackPointNodes = elementRoot.getElementsByTagName(TAG_TRACK_POINT)
        return if (trackPointNodes == null || trackPointNodes.length == 0) {
            null // Gpx trace did not contain correct tagging
        } else createGpxLocationList(trackPointNodes)
    }

    @Throws(ParseException::class)
    private fun createGpxLocationList(trackPointNodes: NodeList): List<Location> {
        val gpxLocations = ArrayList<Location>()

        for (i in 0 until trackPointNodes.length) {
            val node = trackPointNodes.item(i)
            val attributes = node.attributes

            val latitude = createCoordinate(attributes, ATTR_LATITUDE)
            val longitude = createCoordinate(attributes, ATTR_LONGITUDE)
            val time = createTime(node)

            gpxLocations.add(buildGpxLocation(latitude, longitude, time))
        }
        return gpxLocations
    }

    private fun createCoordinate(attributes: NamedNodeMap, attributeName: String): Double {
        val coordinateTextContent = attributes.getNamedItem(attributeName).textContent
        return java.lang.Double.parseDouble(coordinateTextContent)
    }

    @Throws(ParseException::class)
    private fun createTime(trackPoint: Node): Long {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val trackPointChildren = trackPoint.childNodes
        for (i in 0 until trackPointChildren.length) {
            val node = trackPointChildren.item(i)
            if (node.nodeName.contains(TAG_TIME)) {
                val date = dateFormat.parse(node.textContent)
                return date.time
            }
        }
        return 0L
    }

    private fun buildGpxLocation(latitude: Double?, longitude: Double?, time: Long?): Location {
        val gpxLocation = Location(GPX_LOCATION_NAME)
        gpxLocation.time = time!!
        gpxLocation.latitude = latitude!!
        gpxLocation.longitude = longitude!!
        return gpxLocation
    }

    companion object {

        private val TAG_TRACK_POINT = "trkpt"
        private val TAG_TIME = "time"
        private val ATTR_LATITUDE = "lat"
        private val ATTR_LONGITUDE = "lon"
        private val GPX_LOCATION_NAME = "GPX Generated Location"
        private val DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    }
}
