package com.lightimageloaderdownloader.lild.cache.diskcache

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AsyncImageDiskCache(diskCache: DiskCache): AsyncDiskCache<Bitmap>(diskCache) {

    override fun writeValueToFile(value: Bitmap, file: File) {
        value.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
    }

    override fun decodeValueFromFile(file: File): Bitmap? {
        return BitmapFactory.decodeStream(FileInputStream(file))
    }

}