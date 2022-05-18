package com.lightimageloaderdownloader.lild.fetcher

import android.graphics.Bitmap
import com.lightimageloaderdownloader.lild.ImageResult
import com.lightimageloaderdownloader.lild.Request

interface Fetcher {

    fun fetch(request: Request): ImageResult<Bitmap>

}