package com.lightimageloaderdownloader.lild.cache.diskcache

import java.io.*
import java.nio.charset.StandardCharsets

class FaultHidingWriter(
    file: File,
    append: Boolean,
    val onError: (exception: Exception) -> Unit
): BufferedWriter(FileOutputStream(file, append).bufferedWriter(StandardCharsets.UTF_8)) {

    override fun append(csq: CharSequence?): Writer {
        try {
            return super.append(csq)
        } catch (e: Exception) {
            onError(e)
        }
        return this
    }

    override fun newLine() {
        try {
            super.newLine()
        } catch (e: IOException) {
            onError(e)
        }
    }

    override fun flush() {
        try {
            super.flush()
        } catch (e: Exception) {
            onError(e)
        }
    }

}