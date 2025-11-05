package com.mapbox.navigation.testing.utils.offline

import android.content.Context
import android.util.Log
import com.mapbox.common.TileDataDomain
import com.mapbox.navigation.testing.utils.ZipUtils
import org.junit.Assume.assumeTrue
import java.io.File


@Suppress("SpellCheckingInspection")
private const val SD_TILE_STORE_FILE_PATH = "navigation/dmapbox%2fdriving-traffic"

@Suppress("SpellCheckingInspection")
private const val HD_TILE_STORE_FILE_PATH = "navigationhd/dmapbox"

enum class Tileset(
    val assetName: String,
    val domains: List<TileDataDomain>,
    val persistentConfigResourceName: String?
) {
    Berlin("tileset_berlin", listOf(TileDataDomain.NAVIGATION), null),
    NearMunich("tileset_near_munich", listOf(TileDataDomain.NAVIGATION), null),
    HelsinkiHdSd(
        "tileset_helsinki_2025-11-05",
        listOf(TileDataDomain.NAVIGATION, TileDataDomain.NAVIGATION_HD),
        "persistent_config_helsinki"
    ),
}

/**
 * Unpacks offline tiles for the given region and returns the version of the unpacked tileset.
 */
fun Context.unpackTiles(tileset: Tileset): Map<TileDataDomain, String> {
    val domainToVersions = assets.open("${tileset.assetName}.zip").use { tilesetStream ->
        val mapboxDir = File(filesDir, ".mapbox").apply {
            deleteRecursively()
            mkdir()
        }
        ZipUtils.unzip(tilesetStream, mapboxDir)

        tileset.domains.associateWith {
            val tileStoreFilePath = when (it) {
                TileDataDomain.NAVIGATION -> SD_TILE_STORE_FILE_PATH
                TileDataDomain.NAVIGATION_HD -> HD_TILE_STORE_FILE_PATH
                else -> throw IllegalStateException("Unsupported domain: $it")
            }
            val tilesDir = File(mapboxDir, "tile_store/$tileStoreFilePath")
            val version = tilesDir.listFiles()
                // Versions directories names look like "vv2025_10_18-08_47_01". We drop 1 character to get the version without the "v" prefix: this is the accepted version format in NavigationOptions.
                .map { it.name.drop(1) }
                // For HD tiles we have a directory with an empty version: "v". Filter it out.
                .firstOrNull { it.isNotEmpty() }
            assumeTrue(
                "Wasn't able to prepare offline routing tiles",
                version != null,
            )
            version!!
        }
    }
    if (tileset.persistentConfigResourceName != null) {
        loadPersistentConfig(tileset.persistentConfigResourceName)
    }
    return domainToVersions
}

private fun Context.loadPersistentConfig(resourceName: String) {
    val configResId = resources.getIdentifier(resourceName, "raw", packageName)

    if (configResId == 0) {
        Log.e("loadPersistentConfig", "Can't find raw resource with name `$resourceName`")
        throw IllegalStateException("Can't find raw resource with name `$resourceName`")
    } else {
        val configContents =
            resources.openRawResource(configResId).bufferedReader().use { it.readText() }
        val outputDir = File(filesDir, "mbx_nav/tiles/navigation")
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw IllegalStateException("Could not create directory $outputDir")
        }
        File(outputDir, "config.json").writeText(configContents)
    }
}

private fun printFile(file: File) {
    if (file.isDirectory) {
        file.listFiles().forEach {
            printFile(it)
        }
    }
}
