package com.lightimageloaderdownloader.lild

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

sealed class Result {
    class BitmapData(val value: Bitmap): Result()
    class DrawableData(val value: Drawable): Result()
    class Error(val value: Bitmap? = null) : Result()
}