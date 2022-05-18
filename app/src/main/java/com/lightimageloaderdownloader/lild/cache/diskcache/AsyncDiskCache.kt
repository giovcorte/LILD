package com.lightimageloaderdownloader.lild.cache.diskcache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

abstract class AsyncDiskCache<T>(
    private val diskCache: DiskCache,
) {
    private val mutex = Mutex()

    suspend fun put(key: String, value: T) : Boolean {
        return mutex.withLock {
            diskCache.edit(formatKey(key))?.let { editor ->
                writeValueToFile(value, editor.file())
                editor.commit()
                true
            } ?: run {
                false
            }
        }
    }

    suspend fun get(key: String) : T? {
        return mutex.withLock {
            diskCache.get(formatKey(key))?.let { snapshot ->
                val result : T? = decodeValueFromFile(snapshot.file())
                snapshot.close()
                result
            } ?: run {
                null
            }
        }
    }

    fun contains(key: String) : Boolean {
        return diskCache.get(formatKey(key)) != null
    }

    suspend fun clear() {
        mutex.withLock {
            diskCache.evictAll()
        }
    }

    abstract fun writeValueToFile(value: T, file: File)

    abstract fun decodeValueFromFile(file: File) : T?

    private fun formatKey(str: String?): String {
        val formatted = str!!.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
        return formatted.substring(0, if (formatted.length >= 120) 110 else formatted.length)
    }
}