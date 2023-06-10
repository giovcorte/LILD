package com.lightimageloaderdownloader.lild.compose

import androidx.compose.ui.graphics.painter.Painter

sealed class State {
    abstract val painter: Painter?

    /** The request has not been started. */
    object Empty : State() {
        override val painter: Painter? get() = null
    }

    /** The request is in-progress. */
    data class Loading(
        override val painter: Painter? = null
    ) : State()

    /** The request was successful. */
    data class Success(
        override val painter: Painter
    ) : State()

    data class Error(
        override val painter: Painter? = null
    ) : State()
}
