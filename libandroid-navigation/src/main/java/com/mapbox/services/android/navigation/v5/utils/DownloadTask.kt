package com.mapbox.services.android.navigation.v5.utils

import android.os.AsyncTask
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import okhttp3.ResponseBody

/**
 * This class is an [AsyncTask] that downloads a file from a [ResponseBody].
 * Creates a DownloadTask which will download the input stream into the given
 * destinationDirectory with the specified file name and file extension. If a listener is passed
 *
 * @param destDirectory path to the directory where file should be downloaded
 * @param fileName name to name the file
 * @param extension file extension of the resulting file
 * @param downloadListener listener to be updated on completion of the task
 */
class DownloadTask
@JvmOverloads
constructor(
    private val destDirectory: String,
    private val fileName: String = "",
    private val extension: String,
    private val downloadListener: DownloadListener
) : AsyncTask<ResponseBody, Void, File>() {

    companion object {
        private const val END_OF_FILE_DENOTER = -1
        private var uniqueId = 0
    }

    override fun doInBackground(vararg responseBodies: ResponseBody): File? =
        saveAsFile(responseBodies.firstOrNull())

    /**
     * Saves the file returned in the response body as a file in the cache directory
     *
     * @param responseBody containing file
     * @return resulting file, or null if there were any IO exceptions
     */
    private fun saveAsFile(responseBody: ResponseBody?): File? {
        if (responseBody == null) {
            return null
        }

        try {
            val filePath = StringBuilder().append(destDirectory)
                .append(File.separator)
                .append(fileName)
                .append(retrieveUniqueId())
                .append(".")
                .append(extension)
                .toString()

            val file = File(filePath)
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
                inputStream = responseBody.byteStream()
                outputStream = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var numOfBufferedBytes = 0

                while (numOfBufferedBytes != END_OF_FILE_DENOTER) {
                    numOfBufferedBytes = inputStream.read(buffer)
                    outputStream.write(buffer, 0, numOfBufferedBytes)
                }

                outputStream.flush()
                return file
            } catch (exception: IOException) {
                return null
            } catch (exception: Exception) {
                return null
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (exception: IOException) {
            return null
        }
    }

    private fun retrieveUniqueId(): String =
        if (uniqueId++ > 0) uniqueId.toString() else ""

    override fun onPostExecute(instructionFile: File?) {
        if (instructionFile == null) {
            downloadListener.onErrorDownloading()
        } else {
            downloadListener.onFinishedDownloading(instructionFile)
        }
    }

    /**
     * Interface which allows a Listener to be updated upon the completion of a [DownloadTask].
     */
    interface DownloadListener {
        fun onFinishedDownloading(file: File)

        fun onErrorDownloading()
    }
}
