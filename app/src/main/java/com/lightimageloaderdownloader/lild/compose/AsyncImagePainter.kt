package com.lightimageloaderdownloader.lild.compose

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import com.lightimageloaderdownloader.lild.ImageLoader
import com.lightimageloaderdownloader.lild.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

class AsyncImagePainter(private val imageLoader: ImageLoader, private val imageRequest: ImageRequest<Any>) : Painter(),
    RememberObserver {

    private var rememberScope: CoroutineScope? = null

    override val intrinsicSize: Size
        get() = painter?.intrinsicSize ?: Size.Unspecified

    private var painter: Painter? by mutableStateOf(null)
    private var _painter: Painter? = null
        set(value) {
            field = value
            painter = value
        }

    override fun DrawScope.onDraw() {
        painter?.apply { draw(size) }
    }


    override fun onAbandoned() {
        clear()
        (_painter as? RememberObserver)?.onAbandoned()
    }

    override fun onForgotten() {
        clear()
        (_painter as? RememberObserver)?.onForgotten()
    }

    private fun clear() {
        rememberScope?.cancel()
        rememberScope = null
    }

    override fun onRemembered() {
        if (rememberScope != null) return

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        rememberScope = scope

        imageRequest.placeHolder?.let { _painter = it.toPainter() }
        (_painter as? RememberObserver)?.onRemembered()

        scope.launch {
            snapshotFlow {
                imageRequest
            }.mapLatest {
                imageLoader.load(imageRequest = imageRequest)
            }.collect(::updateState)
        }
    }

    private fun updateState(input: State) {
        val previous = _painter
        _painter = input.painter

        if (rememberScope != null && previous !== input.painter) {
            (previous as? RememberObserver)?.onForgotten()
            (input.painter as? RememberObserver)?.onRemembered()
        }

        //onState?.invoke(input)
    }

    private fun Drawable.toPainter() = when (this) {
        is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
        else -> DrawablePainter(mutate())
    }
}