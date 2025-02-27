package com.mapbox.navigation.instrumentation_tests.utils.tiles

import android.content.Context
import android.util.Log
import com.mapbox.navigation.instrumentation_tests.utils.ZipUtils
import org.junit.Assume.assumeTrue
import java.io.File

enum class OfflineRegion(val id: String) {
    Berlin("berlin"),
}

/**
 * Unpacks offline tiles for the given region and returns the version of the unpacked tileset.
 */
fun Context.unpackOfflineTiles(region: OfflineRegion): String {
    val tilesetResName = "tileset_${region.id}"
    val tilesetResId = resources.getIdentifier(tilesetResName, "raw", packageName)

    val tilesVersion = if (tilesetResId == 0) {
        Log.e("unpackTileset", "Can't find raw resource with name `$tilesetResName`")
        null
    } else {
        val tilesetStream = resources.openRawResource(tilesetResId)
        val mapboxDir = File(filesDir, ".mapbox").apply {
            deleteRecursively()
            mkdir()
        }
        ZipUtils.unzip(tilesetStream, mapboxDir)

        @Suppress("SpellCheckingInspection")
        val tilesDir = File(mapboxDir, "tile_store/navigation/dmapbox%2fdriving-traffic")
        tilesDir.listFiles()?.firstOrNull()?.name
    }

    assumeTrue(
        "Wasn't able to prepare offline routing tiles",
        tilesVersion != null,
    )

    return tilesVersion!!.drop(1)
}
