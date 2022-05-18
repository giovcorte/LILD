package com.lightimageloaderdownloader.lild.fetcher

import android.graphics.Bitmap
import com.lightimageloaderdownloader.lild.ImageResult
import com.lightimageloaderdownloader.lild.Request
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class URLFetcher: ImageFetcher() {

    override fun fetch(request: Request): ImageResult<Bitmap> {
        try {
            // InputStream from url
            val imageUrl = URL(request.asString())
            val conn = imageUrl.openConnection() as HttpURLConnection
            val inputStream = conn.inputStream

            // Creating byteArrayOutputStream to decode bitmap and cache it

            // Creating byteArrayOutputStream to decode bitmap and cache it
            val outputStreamUrl = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len: Int
            while (inputStream.read(buffer).also { len = it } > -1) {
                outputStreamUrl.write(buffer, 0, len)
            }
            outputStreamUrl.flush()
            inputStream.close()
            conn.disconnect()

            // Decode byte[] to bitmap, but not from the cached file. Doing so permit us to get the bitmap also if memory is full

            // Decode byte[] to bitmap, but not from the cached file. Doing so permit us to get the bitmap also if memory is full
            val bitmap = decodeByteArray(outputStreamUrl.toByteArray(), request.requiredSize())
            outputStreamUrl.close()

            return ImageResult.Success(bitmap!!)
        } catch (e: Exception) {
            return ImageResult.Error()
        }
    }

}