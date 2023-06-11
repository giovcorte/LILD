package com.lightimageloaderdownloader.lild

import android.graphics.drawable.Drawable
import com.lightimageloaderdownloader.lild.cache.IImageCache

data class RealImageRequest<T>(
    override val data: T,
    override val asString: String,
    override val cachingStrategy: IImageCache.CachingStrategy,
    override val cachingKey: String,
    override val requiredSize: Int,
    override val placeHolder: Drawable?,
    override val errorPlaceHolder: Drawable?
) : ImageRequest<T>

class ImageRequestBuilder<T>(private val data : T, private val asString: String = data.asString()) {
    private var cachingStrategy: IImageCache.CachingStrategy = IImageCache.CachingStrategy.ALL
    private var cachingKey: String = asString
    private var requiredSize: Int = 300
    private var placeHolder: Drawable? = null
    private var errorPlaceHolder: Drawable? = null

    fun cachingStrategy(cachingStrategy: IImageCache.CachingStrategy) = apply {
        this.cachingStrategy = cachingStrategy
    }

    fun cachingKey(key: String) = apply {
        this.cachingKey = key
    }

    fun requiredSize(requiredSize: Int) = apply {
        this.requiredSize = requiredSize
    }

    fun placeHolder(placeHolder: Drawable) = apply {
        this.placeHolder = placeHolder
    }

    fun errorPlaceHolder(errorPlaceHolder: Drawable) = apply {
        this.errorPlaceHolder = errorPlaceHolder
    }

    fun build() : ImageRequest<T> {
        return RealImageRequest(
            data = data,
            asString = asString,
            cachingStrategy = cachingStrategy,
            cachingKey = cachingKey,
            requiredSize = requiredSize,
            placeHolder = placeHolder,
            errorPlaceHolder = errorPlaceHolder,
        )
    }
}