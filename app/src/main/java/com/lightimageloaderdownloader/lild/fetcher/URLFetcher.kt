package com.lightimageloaderdownloader.lild.fetcher

import com.lightimageloaderdownloader.lild.Result
import com.lightimageloaderdownloader.lild.ImageRequest
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class URLFetcher: ImageFetcher() {

    override fun fetch(imageRequest: ImageRequest<*>): Result {
        try {
            // InputStream from url
            val imageUrl = URL(imageRequest.asString)
            val connection = imageUrl.openConnection() as HttpURLConnection
            val inputStream = connection.inputStream

            // Creating byteArrayOutputStream to decode bitmap and cache it

            // Creating byteArrayOutputStream to decode bitmap and cache it
            val outputStreamUrl = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > -1) {
                outputStreamUrl.write(buffer, 0, length)
            }
            outputStreamUrl.flush()
            inputStream.close()
            connection.disconnect()

            // Decode byte[] to bitmap, but not from the cached file. Doing so permit us to get the bitmap also if memory is full

            // Decode byte[] to bitmap, but not from the cached file. Doing so permit us to get the bitmap also if memory is full
            val bitmap = decodeByteArray(outputStreamUrl.toByteArray(), imageRequest.requiredSize)
            outputStreamUrl.close()

            return Result.BitmapData(bitmap!!)
        } catch (e: Exception) {
            return Result.Error()
        }
    }

}