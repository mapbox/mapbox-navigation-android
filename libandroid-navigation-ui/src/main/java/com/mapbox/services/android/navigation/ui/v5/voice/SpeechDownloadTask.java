package com.mapbox.services.android.navigation.ui.v5.voice;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;

class SpeechDownloadTask extends AsyncTask<ResponseBody, Void, File> {

  private static final String MP3_POSTFIX = ".mp3";
  private static final int END_OF_FILE_DENOTER = -1;
  private static int instructionNamingInt = 1;
  private final String cacheDirectory;
  private final TaskListener taskListener;

  SpeechDownloadTask(String cacheDirectory, TaskListener taskListener) {
    this.cacheDirectory = cacheDirectory;
    this.taskListener = taskListener;
  }

  @Override
  protected File doInBackground(ResponseBody... responseBodies) {
    return saveAsFile(responseBodies[0]);
  }

  /**
   * Saves the file returned in the response body as a file in the cache directory
   *
   * @param responseBody containing file
   * @return resulting file, or null if there were any IO exceptions
   */
  private File saveAsFile(ResponseBody responseBody) {
    try {
      File file = new File(cacheDirectory + File.separator + instructionNamingInt++ + MP3_POSTFIX);
      InputStream inputStream = null;
      OutputStream outputStream = null;

      try {
        inputStream = responseBody.byteStream();
        outputStream = new FileOutputStream(file);
        byte[] buffer = new byte[4096];
        int numOfBufferedBytes;

        while ((numOfBufferedBytes = inputStream.read(buffer)) != END_OF_FILE_DENOTER) {
          outputStream.write(buffer, 0, numOfBufferedBytes);
        }

        outputStream.flush();
        return file;

      } catch (IOException exception) {
        taskListener.onErrorDownloading();
        return null;

      } finally {
        if (inputStream != null) {
          inputStream.close();
        }

        if (outputStream != null) {
          outputStream.close();
        }
      }

    } catch (IOException exception) {
      return null;
    }
  }

  @Override
  protected void onPostExecute(File instructionFile) {
    if (instructionFile == null) {
      taskListener.onErrorDownloading();
    } else {
      taskListener.onFinishedDownloading(instructionFile);
    }
  }

  public interface TaskListener {
    void onFinishedDownloading(@NonNull File file);

    void onErrorDownloading();
  }
}
