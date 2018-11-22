package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.navigator.Navigator;

import java.io.File;

import timber.log.Timber;


/**
 * Takes in a string for a path to a TAR file containing routing tiles, and unpacks them to the
 * specified destination path. The path to the TAR file containing routing tiles and the path to
 * the directory in which to unpack the tiles are included in the params passed to this AsyncTask.
 * The first string should be the path to the TAR file, and the second string should be the path
 * to the destination directory for the resulting tiles.
 */
class UnpackerTask extends AsyncTask<String, Integer, File> {
  private final Navigator navigator;

  UnpackerTask(Navigator navigator) {
    this.navigator = navigator;
  }

  @Override
  protected File doInBackground(String... strings) {
    long i = navigator.unpackTiles(strings[0], strings[1]);
    Timber.e("Unpacker NUMBER " + i);

    return new File(strings[0]);
  }

  @Override
  protected void onPostExecute(File file) {
    file.delete();
  }
}
