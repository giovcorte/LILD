package com.lightimageloaderdownloader.lild.compose

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import com.lightimageloaderdownloader.lild.ImageLoader
import com.lightimageloaderdownloader.lild.ImageRequest

@Composable
fun AsyncImage(
    imageLoader: ImageLoader,
    imageRequest: ImageRequest<Any>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {
    val asyncPainter = remember { AsyncImagePainter(imageLoader, imageRequest) }
    Image(painter = asyncPainter, contentDescription = contentDescription, modifier, alignment, contentScale, alpha, colorFilter)
}