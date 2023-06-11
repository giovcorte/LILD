package com.lightimageloaderdownloader.lild.fetcher

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.content.res.ResourcesCompat
import com.lightimageloaderdownloader.lild.Result
import com.lightimageloaderdownloader.lild.ImageRequest

class ResourceFetcher(private val context: Context): ImageFetcher() {

    override fun fetch(imageRequest: ImageRequest<*>): Result {
        try {
            val resource = imageRequest.asString.toInt()

            val bitmap = BitmapFactory.decodeResource(context.resources, resource)
            if (bitmap != null) {
                return Result.BitmapData(bitmap)
            }

            val drawable = ResourcesCompat.getDrawable(context.resources, resource, null)
            return Result.DrawableData(drawable!!)
        } catch (e: Exception) {
            return Result.Error
        }
    }
}