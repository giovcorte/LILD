package com.lightimageloaderdownloader.lild.cache.memorycache

import android.graphics.Bitmap
import java.util.*

class ImageMemoryCache {

    private val cache = Collections.synchronizedMap(
        LinkedHashMap<String?, Bitmap?>(10, 1.5f, true)
    )

    private var size: Long = 0
    private val limit: Long = Runtime.getRuntime().maxMemory() / 8

    @Synchronized
    operator fun get(key: String): Bitmap? {
        return try {
            if (!cache.containsKey(key)) {
                null
            } else cache[key]
        } catch (e: NullPointerException) {
            null
        }
    }

    @Synchronized
    fun contains(key: String): Boolean {
        return cache.containsKey(key)
    }

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        if (cache.containsKey(key)) {
            size -= getSizeInBytes(cache[key])
        }
        cache[key] = bitmap
        size += getSizeInBytes(bitmap)
        checkSize()
    }

    private fun checkSize() {
        if (size > limit) {
            // Least recently accessed item will be the first one iterated
            val iterator: MutableIterator<Map.Entry<String?, Bitmap?>> = cache.entries.iterator()
            while (iterator.hasNext()) {
                val (_, value) = iterator.next()
                size -= getSizeInBytes(value)
                iterator.remove()
                if (size <= limit) {
                    break
                }
            }
        }
    }

    fun clear() {
        cache.clear()
        size = 0
    }

    private fun getSizeInBytes(bitmap: Bitmap?): Long {
        return if (bitmap == null) {
            0
        } else bitmap.rowBytes.toLong() * bitmap.height
    }

}