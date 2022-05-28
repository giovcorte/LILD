package com.lightimageloaderdownloader.lild.target

import android.graphics.Bitmap
import com.lightimageloaderdownloader.lild.Request
import com.lightimageloaderdownloader.lild.Target
import com.lightimageloaderdownloader.lild.cache.IImageCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CacheTargetWrapper(
    private val request: Request,
    private val iImageCache: IImageCache,
    private val coroutineScope: CoroutineScope,
    private val callback : (success: Boolean) -> Unit
) : Target {
    override fun onProcessing(cached: Boolean) {

    }

    override fun onSuccess(bitmap: Bitmap) {
        coroutineScope.launch {
            val cached = withContext(this.coroutineContext) {
                iImageCache.put(request, bitmap)
            }
            callback(cached)
        }
    }

    override fun onError() {
        callback(false)
    }

    override fun getId(): Int {
        return this.hashCode()
    }

}