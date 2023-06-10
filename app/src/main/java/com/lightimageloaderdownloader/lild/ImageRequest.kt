package com.lightimageloaderdownloader.lild

import com.lightimageloaderdownloader.lild.cache.IImageCache

interface ImageRequest<T> {

    val data : T
    val asString: String
    val cachingStrategy: IImageCache.CachingStrategy
    val cachingKey: String
    val requiredSize: Int

    companion object {
        @Suppress("unused")
        fun just(s: String) : ImageRequest<String> {
            return object : ImageRequest<String> {
                override val data = s
                override val asString = s
                override val cachingStrategy: IImageCache.CachingStrategy = IImageCache.CachingStrategy.ALL
                override val cachingKey: String = s
                override val requiredSize: Int = 300
            }
        }
    }

}