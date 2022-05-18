package com.lightimageloaderdownloader.lild

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import com.lightimageloaderdownloader.lild.cache.IImageCache
import com.lightimageloaderdownloader.lild.cache.ImageCache
import com.lightimageloaderdownloader.lild.fetcher.Fetcher
import com.lightimageloaderdownloader.lild.fetcher.FileFetcher
import com.lightimageloaderdownloader.lild.fetcher.ResourceFetcher
import com.lightimageloaderdownloader.lild.fetcher.URLFetcher
import com.lightimageloaderdownloader.lild.target.CacheTargetWrapper
import com.lightimageloaderdownloader.lild.target.DownloadTargetWrapper
import com.lightimageloaderdownloader.lild.target.ImageTargetWrapper
import kotlinx.coroutines.*
import java.io.File

@Suppress("unused")
class ImageLoader {
    private val coroutineScope: CoroutineScope

    private val imageCache: IImageCache

    private val urlFetcher: URLFetcher
    private val fileFetcher: FileFetcher
    private val resourceFetcher: ResourceFetcher

    private val viewsSourcesMap = LinkedHashMap<Int, String>()

    constructor(
        application: Application,
        coroutineScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)
    ) {
        this.coroutineScope = coroutineScope
        this.imageCache = ImageCache(application)
        this.urlFetcher = URLFetcher()
        this.fileFetcher=  FileFetcher()
        this.resourceFetcher = ResourceFetcher(application)
    }

    constructor(
        application: Application,
        coroutineScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO),
        imageCache: IImageCache
    ) {
        this.coroutineScope = coroutineScope
        this.imageCache = imageCache
        this.urlFetcher = URLFetcher()
        this.fileFetcher=  FileFetcher()
        this.resourceFetcher = ResourceFetcher(application)
    }

    init {
        INSTANCE = this
    }

    class RequestBuilder(private val loader: ImageLoader) {
        private lateinit var request: Request
        private lateinit var fetcher: Fetcher
        private var cache: IImageCache.CachingStrategy = IImageCache.CachingStrategy.PREDEFINED
        private var tag: String? = null

        fun load(request: Request, fetcher: Fetcher) : TargetBuilder {
            apply {
                this.request = request
                this.fetcher = fetcher
                return TargetBuilder(loader, request, fetcher)
            }
        }

        fun load(url: String) : TargetBuilder {
            apply {
                if (cache == IImageCache.CachingStrategy.PREDEFINED) {
                    this.cache = IImageCache.CachingStrategy.ALL
                }
                this.request = RequestWrapper(url, tag, cache)
                this.fetcher = loader.urlFetcher
                return TargetBuilder(loader, request, fetcher)
            }
        }

        fun load(file: File) : TargetBuilder {
            apply {
                if (cache == IImageCache.CachingStrategy.PREDEFINED) {
                    this.cache = IImageCache.CachingStrategy.MEMORY
                }
                this.request = RequestWrapper(file.absolutePath, tag, cache)
                this.fetcher = loader.fileFetcher
                return TargetBuilder(loader, request, fetcher)
            }
        }

        fun load(res: Int) : TargetBuilder {
            apply {
                if (cache == IImageCache.CachingStrategy.PREDEFINED) {
                    this.cache = IImageCache.CachingStrategy.NONE
                }
                this.request = RequestWrapper(res.toString(), tag, cache)
                this.fetcher = loader.resourceFetcher
                return TargetBuilder(loader, request, fetcher)
            }
        }

        fun cache(cache: IImageCache.CachingStrategy) = apply {
            this.cache = cache
        }

        fun tag(tag: String?) = apply {
            this.tag = tag
        }
    }

    class TargetBuilder(
        private val loader: ImageLoader,
        private val request: Request,
        private val fetcher: Fetcher,
    ) {
        private lateinit var target: Target

        fun into(target: Target) = apply {
            this.target = target
        }

        fun intoView(view: ImageView?) = apply {
            this.target = ImageTargetWrapper(view)
        }

        fun intoView(view: ImageView?, placeHolder: Drawable) = apply {
            this.target = ImageTargetWrapper(view, placeHolder)
        }

        fun intoCache(callback: (success: Boolean) -> Unit = ::defaultCallback) = apply {
            this.request
            this.target = CacheTargetWrapper(request, loader.imageCache, loader.coroutineScope, callback)
        }

        fun intoFile(file: File, callback: (success: Boolean) -> Unit =::defaultCallback) = apply {
            this.target = DownloadTargetWrapper(file, request, loader.imageCache, loader.coroutineScope, callback)
        }

        fun run() {
            safe(request, target, fetcher) { request, target, fetcher ->
                loader.load(request, target, fetcher)
            }
        }

        private inline fun <T1: Any, T2: Any, T3: Any, R: Any> safe(p1: T1?, p2: T2?, p3: T3?, block: (T1, T2, T3) -> R?): R? {
            return if (p1 != null && p2 != null && p3 != null) block(p1, p2, p3) else null
        }

        private fun defaultCallback(success: Boolean = true) {

        }
    }

    private fun load(
        request: Request,
        target: Target,
        fetcher: Fetcher,
    ) {
        coroutineScope.launch {
            synchronized(viewsSourcesMap) {
                viewsSourcesMap[target.getId()] = request.asString()
            }
            val cached = imageCache.contains(request)
            withContext(Dispatchers.Main) {
                target.onProcessing(cached)
            }
            val result: ImageResult<Bitmap> = imageCache.get(request)?.let {
                log("${request.asString()} from cache")
                ImageResult.Success(it, cached)
            } ?: run {
                log("${request.asString()} from source")
                fetcher.fetch(request)
            }
            submit(request, target, result)
        }
    }

    fun abort() {
        coroutineScope.coroutineContext.cancelChildren()
    }

    suspend fun clear() {
        imageCache.clear()
    }

    fun cache(): IImageCache {
        return imageCache
    }

    private suspend fun submit(
        request: Request,
        target: Target,
        result: ImageResult<Bitmap>,
    ) {
        when (result) {
            is ImageResult.Success -> {
                if (!result.fromCache) {
                    imageCache.put(request, result.value)
                }
                synchronized(viewsSourcesMap) {
                    if (targetMatchesRequest(target, request)) {
                        viewsSourcesMap.remove(target.getId())
                        CoroutineScope(Dispatchers.Main).launch {
                            target.onSuccess(result.value)
                        }
                    }
                }
            }
            is ImageResult.Error -> {
                synchronized(viewsSourcesMap) {
                    if (targetMatchesRequest(target, request)) {
                        viewsSourcesMap.remove(target.getId())
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        target.onError()
                    }
                }
            }
        }
    }

    fun logging(enabled: Boolean) {
        LOGGING_ENABLED = enabled
    }

    private fun log(message: String) {
        if (LOGGING_ENABLED) {
            Log.d(IMAGE_LOADER_TAG, message)
        }
    }

    private fun targetMatchesRequest(target: Target, request: Request) : Boolean {
        return viewsSourcesMap[target.getId()] == request.asString()
    }

    companion object {
        const val IMAGE_LOADER_TAG = "ImageLoader"

        var LOGGING_ENABLED = false
        lateinit var INSTANCE: ImageLoader

        fun get(): RequestBuilder {
            return RequestBuilder(INSTANCE)
        }
    }

}