package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import java.io.File;


/**
 * Takes in a string for a path to a TAR file containing routing tiles, and unpacks them to the
 * specified destination path. The path to the TAR file containing routing tiles and the path to
 * the directory in which to unpack the tiles are included in the params passed to this AsyncTask.
 */
public class UnpackerTask extends AsyncTask<String, Integer, File> {

  @Override
  protected File doInBackground(String... strings) {
    MapboxOfflineNavigator offlineNavigator = new MapboxOfflineNavigator();
    offlineNavigator.unpackTiles(strings[0], strings[1]);
    return new File(strings[0]);
  }

  @Override
  protected void onPostExecute(File file) {
    file.delete();
  }
}
