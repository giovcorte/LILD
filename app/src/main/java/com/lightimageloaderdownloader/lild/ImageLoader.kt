package com.lightimageloaderdownloader.lild

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.lightimageloaderdownloader.lild.cache.IImageCache
import com.lightimageloaderdownloader.lild.cache.ImageCache
import com.lightimageloaderdownloader.lild.compose.DrawablePainter
import com.lightimageloaderdownloader.lild.compose.State
import com.lightimageloaderdownloader.lild.fetcher.Fetcher
import kotlinx.coroutines.*
import java.lang.IllegalArgumentException

@Suppress("unused")
class ImageLoader(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO),
    private val imageCache: IImageCache = ImageCache(context, DEFAULT_DISK_CACHE_SIZE, DEFAULT_APP_VERSION)) {

    init {
        INSTANCE = this
    }

    suspend fun load(imageRequest: ImageRequest<*>) : State {
        val result = coroutineScope.async {
            val fetcher = Fetcher.Factory.from(imageRequest.data, context)
            fetcher?.let { imageFetcher ->
                when(val result = imageFetcher.fetch(imageRequest)) {
                    is Result.BitmapData -> State.Success(BitmapPainter(result.value.asImageBitmap()))
                    is Result.DrawableData -> State.Success(DrawablePainter(result.value))
                    is Result.Error -> State.Error()
                }
            } ?: run { throw IllegalArgumentException("No fetcher for this data") }

        }
        return result.await()
    }

    fun abort() {
        coroutineScope.coroutineContext.cancelChildren()
    }

    fun clear() {
        imageCache.clear()
    }

    fun cache(): IImageCache {
        return imageCache
    }

    fun logging(enabled: Boolean) {
        LOGGING_ENABLED = enabled
    }

    private fun log(message: String) {
        if (LOGGING_ENABLED) {
            Log.d(IMAGE_LOADER_TAG, message)
        }
    }

    companion object {
        const val DEFAULT_DISK_CACHE_SIZE: Long = 1024 * 1024 * 200
        const val DEFAULT_APP_VERSION: Int = 1
        const val IMAGE_LOADER_TAG = "LightImageLoaderDownloader"

        var LOGGING_ENABLED = false
        lateinit var INSTANCE: ImageLoader

        fun get(): ImageLoader {
            return INSTANCE
        }
    }

}