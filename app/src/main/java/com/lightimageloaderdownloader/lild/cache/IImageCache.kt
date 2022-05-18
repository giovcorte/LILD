package com.lightimageloaderdownloader.lild.cache

import android.graphics.Bitmap
import com.lightimageloaderdownloader.lild.Request
import java.io.File

interface IImageCache {

    suspend fun get(request: Request) : Bitmap?

    suspend fun put(request: Request, bitmap: Bitmap) : Boolean

    suspend fun clear()

    suspend fun dumps(request: Request, file: File) : Boolean

    fun contains(request: Request) : Boolean

    enum class CachingStrategy {
        ALL, MEMORY, DISK, NONE, PREDEFINED
    }

}