package com.lightimageloaderdownloader.lild.cache

import android.content.Context
import android.graphics.Bitmap
import com.lightimageloaderdownloader.lild.ImageRequest
import com.lightimageloaderdownloader.lild.cache.diskcache.ImageDiskCache
import com.lightimageloaderdownloader.lild.cache.diskcache.DiskCache
import com.lightimageloaderdownloader.lild.cache.memorycache.ImageMemoryCache
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageCache(
    context: Context,
    diskSize: Long,
    appVersion: Int
) : IImageCache {

    private val diskImageCache = ImageDiskCache(DiskCache(File(context.cacheDir.path + File.separator + "imagescache"), diskSize, appVersion))
    private val memoryImageCache = ImageMemoryCache()

    override fun get(imageRequest: ImageRequest<*>): Bitmap? {
        val key = imageRequest.cachingKey
        return if (memoryImageCache.contains(key)) memoryImageCache[key] else diskImageCache.get(key)
    }

    override fun put(imageRequest: ImageRequest<*>, bitmap: Bitmap) : Boolean {
        val key = imageRequest.cachingKey
        var result = true
        when (imageRequest.cachingStrategy) {
            IImageCache.CachingStrategy.ALL -> {
                memoryImageCache.put(key, bitmap)
                result = diskImageCache.put(key, bitmap)
            }
            IImageCache.CachingStrategy.DISK -> if (diskImageCache.get(key) == null) {
                result = diskImageCache.put(key, bitmap)
            }
            IImageCache.CachingStrategy.MEMORY -> if (memoryImageCache[key] == null) {
                memoryImageCache.put(key, bitmap)
            }
            else -> { }
        }
        return result
    }

    override fun contains(imageRequest: ImageRequest<*>): Boolean {
        val key = imageRequest.cachingKey
        return memoryImageCache.contains(key) || diskImageCache.contains(key)
    }

    override fun clear() {
        memoryImageCache.clear()
        diskImageCache.clear()
    }

    override fun dumps(imageRequest: ImageRequest<*>, file: File): Boolean {
        return try {
            val bitmap = get(imageRequest)
            writeBitmap(bitmap, file)
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun writeBitmap(bitmap: Bitmap?, imageFile: File) : Boolean {
        return try {
            val outputStream = FileOutputStream(imageFile)
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            true
        } catch (_: Exception) {
            false
        }
    }

}