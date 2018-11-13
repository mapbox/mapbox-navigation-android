package com.mapbox.services.android.navigation.v5.navigation.offline;

import android.os.AsyncTask;

import com.mapbox.services.android.navigation.v5.navigation.NavigationLibraryLoader;

import java.io.File;


/**
 * Takes in a string for a path to a TAR file containing routing tiles, and unpacks them to the
 * specified destination path. The path to the TAR file containing routing tiles and the path to
 * the directory in which to unpack the tiles are included in the params passed to this AsyncTask.
 */
public class UnpackerTask extends AsyncTask<String, Integer, File> {

  static {
    NavigationLibraryLoader.load();
  }

  @Override
  protected File doInBackground(String... strings) {
    MapboxOfflineRouter router = new MapboxOfflineRouter();
    router.unpackTiles(strings[0], strings[1]);
    return new File(strings[0]);
  }

  @Override
  protected void onPostExecute(File file) {
    file.delete();
  }
}
