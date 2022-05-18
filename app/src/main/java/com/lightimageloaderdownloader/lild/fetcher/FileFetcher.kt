package com.lightimageloaderdownloader.lild.fetcher

import android.graphics.Bitmap
import com.lightimageloaderdownloader.lild.ImageResult
import com.lightimageloaderdownloader.lild.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

class FileFetcher: ImageFetcher() {

    override fun fetch(request: Request): ImageResult<Bitmap> {
        try {
            val inputStream = FileInputStream(File(request.asString()))
            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len: Int
            while (inputStream.read(buffer).also { len = it } > -1) {
                byteArrayOutputStream.write(buffer, 0, len)
            }
            byteArrayOutputStream.flush()
            inputStream.close()
            val bitmap = decodeByteArray(
                byteArrayOutputStream.toByteArray(),
                request.requiredSize()
            )
            byteArrayOutputStream.close()

            return ImageResult.Success(bitmap!!)
        } catch (e: Exception) {
            return ImageResult.Error()
        }
    }

}