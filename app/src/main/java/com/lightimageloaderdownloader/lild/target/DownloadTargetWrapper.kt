package com.lightimageloaderdownloader.lild.target

import android.graphics.Bitmap
import com.lightimageloaderdownloader.lild.Request
import com.lightimageloaderdownloader.lild.Target
import com.lightimageloaderdownloader.lild.cache.IImageCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadTargetWrapper(
    private val file: File,
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
            if (cached) {
                val succeed = withContext(this.coroutineContext) {
                    iImageCache.dumps(request, file)
                }
                callback(succeed)
            } else {
                callback(false)
            }
        }

    }

    override fun onError() {
        callback(false)
    }

    override fun getId(): Int {
        return this.hashCode()
    }


}