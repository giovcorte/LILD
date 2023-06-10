package com.lightimageloaderdownloader.lild.cache.diskcache

import java.io.File

abstract class GenericDiskCache<T>(
    private val diskCache: DiskCache,
) {
    fun put(key: String, value: T) : Boolean {
        return diskCache.edit(formatKey(key))?.let { editor ->
            writeValueToFile(value, editor.file())
            editor.commit()
            true
        } ?: run {
            false
        }
    }

    fun get(key: String) : T? {
        return diskCache.get(formatKey(key))?.let { snapshot ->
            val result : T? = decodeValueFromFile(snapshot.file())
            snapshot.close()
            result
        } ?: run {
            null
        }
    }

    fun contains(key: String) : Boolean {
        return diskCache.get(formatKey(key)) != null
    }

    fun clear() {
        diskCache.evictAll()
    }

    abstract fun writeValueToFile(value: T, file: File)

    abstract fun decodeValueFromFile(file: File) : T?

    private fun formatKey(str: String?): String {
        val formatted = str!!.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
        return formatted.substring(0, if (formatted.length >= 120) 110 else formatted.length)
    }
}