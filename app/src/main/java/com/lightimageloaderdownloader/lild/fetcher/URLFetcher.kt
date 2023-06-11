package com.lightimageloaderdownloader.lild.fetcher

import com.lightimageloaderdownloader.lild.Result
import com.lightimageloaderdownloader.lild.ImageRequest
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class URLFetcher: ImageFetcher() {

    override fun fetch(imageRequest: ImageRequest<*>): Result {
        try {
            val imageUrl = URL(imageRequest.asString)
            val connection = imageUrl.openConnection() as HttpURLConnection
            val inputStream = connection.inputStream

            val outputStreamUrl = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > -1) {
                outputStreamUrl.write(buffer, 0, length)
            }
            outputStreamUrl.flush()
            inputStream.close()
            connection.disconnect()

            val bitmap = decodeByteArray(outputStreamUrl.toByteArray(), imageRequest.requiredSize)
            outputStreamUrl.close()

            return Result.BitmapData(bitmap!!)
        } catch (e: Exception) {
            return Result.Error
        }
    }

}