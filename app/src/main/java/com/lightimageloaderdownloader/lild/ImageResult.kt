package com.lightimageloaderdownloader.lild

import android.graphics.Bitmap

sealed class ImageResult<out T> {
    data class Success(val value: Bitmap, val fromCache: Boolean = false): ImageResult<Bitmap>()
    data class Error(val value: Bitmap? = null) : ImageResult<Nothing>()
}