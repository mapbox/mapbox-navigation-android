package com.mapbox.navigation.instrumentation_tests.utils.tiles

import com.mapbox.geojson.FeatureCollection

object OfflineRegions {
    val Berlin = OfflineRegion(
        id = "berlin-test-tiles",
        geometry = BERLIN_GEOMETRY
    )
}

private val BERLIN_GEOMETRY = FeatureCollection.fromJson(
    "{\n" +
        "  \"type\": \"FeatureCollection\",\n" +
        "  \"features\": [\n" +
        "    {\n" +
        "      \"type\": \"Feature\",\n" +
        "      \"properties\": {},\n" +
        "      \"geometry\": {\n" +
        "        \"coordinates\": [\n" +
        "          [\n" +
        "            [\n" +
        "              13.03807042990934,\n" +
        "              52.70072965030741\n" +
        "            ],\n" +
        "            [\n" +
        "              13.03807042990934,\n" +
        "              52.32726294794662\n" +
        "            ],\n" +
        "            [\n" +
        "              13.818542568562549,\n" +
        "              52.32726294794662\n" +
        "            ],\n" +
        "            [\n" +
        "              13.818542568562549,\n" +
        "              52.70072965030741\n" +
        "            ],\n" +
        "            [\n" +
        "              13.03807042990934,\n" +
        "              52.70072965030741\n" +
        "            ]\n" +
        "          ]\n" +
        "        ],\n" +
        "        \"type\": \"Polygon\"\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}"
).features()!!.first().geometry()!!
