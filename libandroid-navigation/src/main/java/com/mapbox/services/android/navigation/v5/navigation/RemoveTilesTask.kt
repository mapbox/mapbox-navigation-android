package com.mapbox.services.android.navigation.v5.navigation

import android.os.AsyncTask
import com.mapbox.geojson.Point
import com.mapbox.navigator.Navigator

internal class RemoveTilesTask(
    private val navigator: Navigator,
    private val tilePath: String,
    private val southwest: Point,
    private val northeast: Point,
    private val callback: OnOfflineTilesRemovedCallback
) : AsyncTask<Void, Void, Long>() {

    override fun doInBackground(vararg paramsUnused: Void): Long = 1
        // navigator.removeTiles(tilePath, southwest, northeast)

    public override fun onPostExecute(numberOfTiles: Long) = callback.onRemoved(numberOfTiles)
}
