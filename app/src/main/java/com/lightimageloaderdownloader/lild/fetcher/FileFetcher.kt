package com.lightimageloaderdownloader.lild.fetcher

import com.lightimageloaderdownloader.lild.Result
import com.lightimageloaderdownloader.lild.ImageRequest
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

class FileFetcher: ImageFetcher() {

    override fun fetch(imageRequest: ImageRequest<*>): Result {
        try {
            val inputStream = FileInputStream(File(imageRequest.asString))
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
                imageRequest.requiredSize
            )
            byteArrayOutputStream.close()

            return Result.BitmapData(bitmap!!)
        } catch (e: Exception) {
            return Result.Error
        }
    }

}