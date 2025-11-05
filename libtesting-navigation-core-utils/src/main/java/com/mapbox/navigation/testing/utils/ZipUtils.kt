package com.mapbox.navigation.testing.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.collections.forEach
import kotlin.collections.orEmpty

object ZipUtils {
    @Throws(IOException::class)
    fun zipDirectory(folderToZip: File, outputFile: File) {
        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                zipFile(
                    folderToZip,
                    folderToZip.getName().trimEnd('/'),
                    zos,
                )
            }
        }
    }

    @Throws(IOException::class)
    fun unzip(inputStream: InputStream, toFolder: File) {
        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val file = File(toFolder, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos -> zis.copyTo(fos) }
                }
                entry = zis.nextEntry
            }
        }
    }

    @Throws(IOException::class)
    private fun zipFile(fileToZip: File, fileName: String, zos: ZipOutputStream) {
        if (fileToZip.isHidden()) {
            return
        }

        if (fileToZip.isDirectory()) {
            zos.putNextEntry(ZipEntry("$fileName/"))
            zos.closeEntry()

            fileToZip.listFiles().orEmpty().forEach { childFile ->
                zipFile(childFile, fileName + "/" + childFile.getName(), zos)
            }
        } else {
            FileInputStream(fileToZip).use { fis ->
                zos.putNextEntry(ZipEntry(fileName))
                fis.copyTo(zos)
            }
        }
    }

}
