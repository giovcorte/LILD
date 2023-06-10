package com.lightimageloaderdownloader.lild

import android.graphics.drawable.Drawable
import com.lightimageloaderdownloader.lild.cache.IImageCache

interface ImageRequest<T> {

    val data : T
    val asString: String
    val cachingStrategy: IImageCache.CachingStrategy
    val cachingKey: String
    val requiredSize: Int
    val placeHolder: Drawable?
    companion object {
        @Suppress("unused")
        fun just(s: String) : ImageRequest<String> {
            return object : ImageRequest<String> {
                override val data = s
                override val asString = s
                override val cachingStrategy: IImageCache.CachingStrategy = IImageCache.CachingStrategy.ALL
                override val cachingKey: String = s
                override val requiredSize: Int = 300
                override val placeHolder = null
            }
        }
    }

}