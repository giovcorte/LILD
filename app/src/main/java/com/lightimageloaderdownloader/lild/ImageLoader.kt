package com.lightimageloaderdownloader.lild

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.lightimageloaderdownloader.lild.cache.IImageCache
import com.lightimageloaderdownloader.lild.cache.ImageCache
import com.lightimageloaderdownloader.lild.compose.DrawablePainter
import com.lightimageloaderdownloader.lild.compose.State
import com.lightimageloaderdownloader.lild.compose.toPainter
import com.lightimageloaderdownloader.lild.exceptions.IllegalDataException
import com.lightimageloaderdownloader.lild.fetcher.Fetcher
import kotlinx.coroutines.*

@Suppress("unused")
class ImageLoader(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO),
    private val imageCache: IImageCache = ImageCache(context, DEFAULT_DISK_CACHE_SIZE, DEFAULT_APP_VERSION)) {

    private val currentRequests : MutableMap<Int, ImageRequest<*>> = mutableMapOf()

    fun load(imageRequest: ImageRequest<*>, imageView: ImageView) {
        currentRequests[imageView.hashCode()] = imageRequest
        imageView.setImageDrawable(imageRequest.placeHolder)
        coroutineScope.launch {
            if (imageCache.contains(imageRequest)) {
                val image = imageCache.get(imageRequest)
                if (currentRequests[imageView.hashCode()] !== imageRequest) return@launch
                currentRequests.remove(imageView.hashCode())
                withContext(Dispatchers.Main.immediate) {
                    imageView.setImageBitmap(image)
                }
            } else {
                Fetcher.Factory.from(imageRequest.data, context)?.let { imageFetcher ->
                    val result: Any? = when (val result = imageFetcher.fetch(imageRequest)) {
                        is Result.BitmapData -> {
                            imageCache.put(imageRequest, result.value)
                            result.value
                        }
                        is Result.DrawableData -> result.value
                        is Result.Error -> imageRequest.errorPlaceHolder
                    }
                    if (currentRequests[imageView.hashCode()] !== imageRequest) return@launch
                    currentRequests.remove(imageView.hashCode())
                    withContext(Dispatchers.Main.immediate) {
                        when (result) {
                            is Bitmap -> imageView.setImageBitmap(result)
                            is Drawable -> imageView.setImageDrawable(result)
                            else -> imageView.setImageDrawable(imageRequest.errorPlaceHolder)
                        }
                    }
                } ?: run { throw IllegalDataException(imageRequest.data?.javaClass) }
            }
        }
    }

    internal suspend fun load(imageRequest: ImageRequest<*>) : State {
        val result = coroutineScope.async {
            imageCache.get(imageRequest)?.let {
                State.Success(BitmapPainter(it.asImageBitmap()))
            } ?: run {
                Fetcher.Factory.from(imageRequest.data, context)?.let { imageFetcher ->
                    when(val result = imageFetcher.fetch(imageRequest)) {
                        is Result.BitmapData -> {
                            imageCache.put(imageRequest, result.value)
                            State.Success(BitmapPainter(result.value.asImageBitmap()))
                        }
                        is Result.DrawableData -> State.Success(DrawablePainter(result.value))
                        is Result.Error -> State.Error(imageRequest.errorPlaceHolder?.toPainter())
                    }
                } ?: run { throw IllegalDataException(imageRequest.data?.javaClass) }
            }
        }
        return result.await()
    }

    fun abort() {
        coroutineScope.coroutineContext.cancelChildren()
    }

    fun clear() {
        coroutineScope.coroutineContext.cancelChildren()
        currentRequests.clear()
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
    }

}