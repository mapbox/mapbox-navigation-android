package com.mapbox.services.android.navigation.v5.utils;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;

/**
 * This class is an {@link AsyncTask} that downloads a file from a {@link ResponseBody}.
 */
public class DownloadTask extends AsyncTask<ResponseBody, Void, File> {

  private static final int END_OF_FILE_DENOTER = -1;
  private static int uniqueId = 0;
  private final String destDirectory;
  private final DownloadListener downloadListener;
  private final String extension;
  private final String fileName;

  /**
   * Creates a DownloadTask with a blank file name, so each subsequent file will just be named
   * numerically.
   *
   * @param destDirectory path to the directory where file should be downloaded
   * @param extension file extension of the resulting file
   * @param downloadListener listener to be updated on completion of the task
   */
  public DownloadTask(String destDirectory, String extension, DownloadListener downloadListener) {
    this(destDirectory, "", extension, downloadListener);
  }

  /**
   * Creates a DownloadTask which will download the input stream into the given
   * destinationDirectory with the specified file name and file extension. If a listener is passed
   *
   * @param destDirectory path to the directory where file should be downloaded
   * @param fileName name to name the file
   * @param extension file extension of the resulting file
   * @param downloadListener listener to be updated on completion of the task
   */
  public DownloadTask(String destDirectory, String fileName, String extension,
                      DownloadListener downloadListener) {
    this.destDirectory = destDirectory;
    this.fileName = fileName;
    this.extension = extension;
    this.downloadListener = downloadListener;
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
    if (responseBody == null) {
      return null;
    }

    try {
      File file = new File(destDirectory + File.separator + fileName + retrieveUniqueId() + "." + extension);
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
        return null;

      } catch (Exception exception) {
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

  private String retrieveUniqueId() {
    return uniqueId++ > 0 ? "" + uniqueId : "";
  }

  @Override
  protected void onPostExecute(File instructionFile) {
    if (downloadListener == null) {
      return;
    }

    if (instructionFile == null) {
      downloadListener.onErrorDownloading();
    } else {
      downloadListener.onFinishedDownloading(instructionFile);
    }
  }

  /**
   * Interface which allows a Listener to be updated upon the completion of a {@link DownloadTask}.
   */
  public interface DownloadListener {
    void onFinishedDownloading(@NonNull File file);

    void onErrorDownloading();
  }
}
