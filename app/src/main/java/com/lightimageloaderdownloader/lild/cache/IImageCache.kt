package com.lightimageloaderdownloader.lild.cache

import android.graphics.Bitmap
import com.lightimageloaderdownloader.lild.ImageRequest
import java.io.File

interface IImageCache {

    fun get(imageRequest: ImageRequest<*>) : Bitmap?

    fun put(imageRequest: ImageRequest<*>, bitmap: Bitmap) : Boolean

    fun clear()

    fun dumps(imageRequest: ImageRequest<*>, file: File) : Boolean

    fun contains(imageRequest: ImageRequest<*>) : Boolean

    enum class CachingStrategy {
        ALL, MEMORY, DISK, NONE, PREDEFINED
    }

}