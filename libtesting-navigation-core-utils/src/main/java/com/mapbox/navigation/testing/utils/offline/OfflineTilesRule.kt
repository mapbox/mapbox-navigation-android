package com.mapbox.navigation.testing.utils.offline

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.common.TileDataDomain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.system.measureTimeMillis

private const val LOG_TAG = "OfflineTilesRule"

/**
 * Marks a test that needs [tileset] unpacked into the default tile store. Read by
 * [OfflineTilesRule].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class WithOfflineTiles(val tileset: Tileset)

/**
 * Unpacks the tileset that a test requests with [WithOfflineTiles] before the test starts.
 *
 * Unpacking is blocking and takes seconds on a slow device. Doing it in a rule keeps it off the
 * thread the test body runs on and out of the test's own timeout. Tests without the annotation
 * get nothing unpacked.
 */
class OfflineTilesRule : TestRule {

    private var unpacked: Pair<Tileset, Map<TileDataDomain, String>>? = null

    /**
     * Returns the version of [tileset] unpacked for the current test.
     */
    fun tilesVersion(
        tileset: Tileset,
        domain: TileDataDomain = TileDataDomain.NAVIGATION,
    ): String {
        val (unpackedTileset, versions) = checkNotNull(unpacked?.takeIf { it.first == tileset }) {
            "annotate the test with @${WithOfflineTiles::class.simpleName}($tileset)"
        }
        return checkNotNull(versions[domain]) { "$unpackedTileset has no $domain tiles" }
    }

    override fun apply(base: Statement, description: Description): Statement {
        val tileset = description.getAnnotation(WithOfflineTiles::class.java)?.tileset
            ?: return base
        return object : Statement() {
            override fun evaluate() {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                Log.d(LOG_TAG, "unpacking $tileset for ${description.methodName}")
                val tookMs = measureTimeMillis {
                    unpacked = tileset to context.unpackTiles(tileset)
                }
                Log.d(LOG_TAG, "unpacked $tileset in $tookMs ms")
                try {
                    base.evaluate()
                } finally {
                    unpacked = null
                }
            }
        }
    }
}
