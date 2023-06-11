package com.lightimageloaderdownloader.lild

import android.graphics.drawable.Drawable
import com.lightimageloaderdownloader.lild.cache.IImageCache
import com.lightimageloaderdownloader.lild.exceptions.IllegalDataException
import java.io.File

interface ImageRequest<T> {

    val data : T
    val asString: String
    val cachingStrategy: IImageCache.CachingStrategy
    val cachingKey: String
    val requiredSize: Int
    val placeHolder: Drawable?
    val errorPlaceHolder: Drawable?

    object Factory {
        fun just(s: String): ImageRequest<String> {
            return object : ImageRequest<String> {
                override val data = s
                override val asString = s
                override val cachingStrategy: IImageCache.CachingStrategy = IImageCache.CachingStrategy.ALL
                override val cachingKey: String = s
                override val requiredSize: Int = 300
                override val placeHolder = null
                override val errorPlaceHolder = null
            }
        }
    }

}

fun Any?.asString(): String = when(this) {
    is String -> this
    is File -> this.absolutePath
    is Int -> this.toString()
    else -> throw IllegalDataException(this)
}