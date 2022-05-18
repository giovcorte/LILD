package com.lightimageloaderdownloader.lild.fetcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lightimageloaderdownloader.lild.Request
import com.lightimageloaderdownloader.lild.ImageResult
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

abstract class ImageFetcher: Fetcher {

    abstract override fun fetch(request: Request): ImageResult<Bitmap>

    @Throws(IOException::class)
    open fun decodeByteArray(bytes: ByteArray?, requiredSize: Int): Bitmap? {
        // Decode image size
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val stream1: InputStream = ByteArrayInputStream(bytes)
        BitmapFactory.decodeStream(stream1, null, options)
        stream1.close()
        // Scale image in order to reduce memory consumption
        var widthTmp = options.outWidth
        var heightTmp = options.outHeight
        var scale = 1
        while (widthTmp / 2 >= requiredSize && heightTmp / 2 >= requiredSize) {
            widthTmp /= 2
            heightTmp /= 2
            scale *= 2
        }
        // Decode with current scale values
        val options1 = BitmapFactory.Options()
        options1.inSampleSize = scale
        val stream2: InputStream = ByteArrayInputStream(bytes)
        val bitmap = BitmapFactory.decodeStream(stream2, null, options1)
        stream2.close()
        return bitmap
    }

}