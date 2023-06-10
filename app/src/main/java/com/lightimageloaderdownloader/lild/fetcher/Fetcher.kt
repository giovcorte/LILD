package com.lightimageloaderdownloader.lild.fetcher

import android.content.Context
import com.lightimageloaderdownloader.lild.Result
import com.lightimageloaderdownloader.lild.ImageRequest
import java.io.File

interface Fetcher {

    fun fetch(imageRequest: ImageRequest<*>): Result

    object Factory {
        fun from(model: Any?, context: Context) : Fetcher? {
            return when(model) {
                is String -> URLFetcher()
                is File -> FileFetcher()
                is Int -> ResourceFetcher(context)
                else -> null
            }
        }
    }

}