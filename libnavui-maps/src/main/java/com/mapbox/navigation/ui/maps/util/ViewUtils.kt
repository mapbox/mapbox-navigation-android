package com.mapbox.navigation.ui.maps.util

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import com.mapbox.maps.MapView

/**
 * Provides utilities to capture screenshots of view trees, that may contain one or more [MapView]s.
 */
object ViewUtils {

    /**
     * Captures a view tree that contains a given [View] and returns the obtained [Bitmap].
     *
     * @param callback invoked when a screenshot of the view tree is ready
     */
    @JvmStatic
    @UiThread
    fun View.capture(callback: OnViewScreenshotReady) {
        val root = rootView
        val maps = collectMaps(root)
        collectSnapshots(maps) { snapshots ->
            val foregrounds = ArrayList<Drawable?>(snapshots.size)
            for ((map, snapshot) in snapshots) {
                foregrounds.add(map.foreground)
                map.foreground = BitmapDrawable(map.resources, snapshot)
            }
            root.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(root.drawingCache)
            root.isDrawingCacheEnabled = false
            snapshots.forEachIndexed { index, (map, _) ->
                map.foreground = foregrounds[index]
            }
            callback.onViewCaptureReady(bitmap)
        }
    }

    // finds all map views in a view tree using BFS approach
    private fun collectMaps(root: View): List<MapView> {
        val queue = ArrayDeque<View>()
        val maps = arrayListOf<MapView>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            when (val head = queue.removeFirst()) {
                is MapView -> {
                    maps.add(head)
                }
                is ViewGroup -> {
                    repeat(head.childCount) { index ->
                        queue.add(head.getChildAt(index))
                    }
                }
            }
        }
        return maps
    }

    // requests snapshots of all map views and waits for every callback
    private inline fun collectSnapshots(
        maps: List<MapView>,
        crossinline onDone: (List<Pair<MapView, Bitmap>>) -> Unit,
    ) {
        var snapshotsLeft = maps.size
        val snapshots = arrayListOf<Pair<MapView, Bitmap>>()
        val mainHandler = Handler(Looper.getMainLooper())
        for (map in maps) {
            map.snapshot { snapshot ->
                mainHandler.post {
                    if (snapshot != null) {
                        snapshots.add(map to snapshot)
                    }
                    if (--snapshotsLeft == 0) {
                        onDone(snapshots)
                    }
                }
            }
        }
    }
}
