package com.lightimageloaderdownloader.lild

import com.lightimageloaderdownloader.lild.cache.IImageCache

interface Request {

    fun asString(): String

    fun cachingStrategy(): IImageCache.CachingStrategy = IImageCache.CachingStrategy.ALL

    fun cachingKey() : String = asString()

    fun requiredSize() : Int = 300

    companion object {
        @Suppress("unused")
        fun just(s: String) : Request {
            return object : Request {
                override fun asString(): String {
                    return s
                }
            }
        }
    }

}