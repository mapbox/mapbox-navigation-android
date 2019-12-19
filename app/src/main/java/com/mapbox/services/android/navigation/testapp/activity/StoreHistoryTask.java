package com.mapbox.services.android.navigation.testapp.activity;

import android.os.AsyncTask;
import android.os.Environment;

import com.mapbox.navigation.base.logger.model.Message;
import com.mapbox.navigation.logger.MapboxLogger;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

class StoreHistoryTask extends AsyncTask<Void, Void, Void> {
  private static final String EMPTY_HISTORY = "{}";
  private static final String DRIVES_FOLDER = "/drives";
  private final MapboxNavigation navigation;
  private final String filename;

  StoreHistoryTask(MapboxNavigation navigation, String filename) {
    this.navigation = navigation;
    this.filename = filename;
  }

  @Override
  protected Void doInBackground(Void... paramsUnused) {
    if (isExternalStorageWritable()) {
      String history = navigation.retrieveHistory();
      if (!history.contentEquals(EMPTY_HISTORY)) {
        File pathToExternalStorage = Environment.getExternalStorageDirectory();
        File appDirectory = new File(pathToExternalStorage.getAbsolutePath() + DRIVES_FOLDER);
        appDirectory.mkdirs();
        File saveFilePath = new File(appDirectory, filename);
        write(history, saveFilePath);
      }
    }
    return null;
  }

  private boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)) {
      return true;
    }
    return false;
  }

  private void write(String history, File saveFilePath) {
    try {
      FileOutputStream fos = new FileOutputStream(saveFilePath);
      OutputStreamWriter outDataWriter = new OutputStreamWriter(fos);
      outDataWriter.write(history);
      outDataWriter.close();
      fos.flush();
      fos.close();
    } catch (Exception exception) {
      MapboxLogger.INSTANCE.e(new Message(exception.getLocalizedMessage()), exception);
    }
  }
}
