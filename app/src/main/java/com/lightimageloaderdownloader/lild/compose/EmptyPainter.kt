package com.lightimageloaderdownloader.lild.compose

import androidx.compose.runtime.RememberObserver
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

class EmptyPainter(override val intrinsicSize: Size = Size.Unspecified) : Painter(), RememberObserver {
    override fun DrawScope.onDraw() { }

    override fun onAbandoned() { }

    override fun onForgotten() { }

    override fun onRemembered() { }

}
