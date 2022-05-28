package com.lightimageloaderdownloader.lild.target

import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.lightimageloaderdownloader.lild.Target
import java.lang.ref.Reference
import java.lang.ref.WeakReference

class ImageTargetWrapper(
    imageView: ImageView?,
    private val placeHolder: Drawable? = null,
    private val errorPlaceHolder: Drawable? = null
) : Target {

    private val view: Reference<ImageView> = WeakReference(imageView)

    override fun onProcessing(cached: Boolean) {
        if (!cached && placeHolder != null) {
            getView()?.setImageDrawable(placeHolder)
            if (placeHolder is Animatable) placeHolder.start()
        }
    }

    override fun onSuccess(bitmap: Bitmap) {
        getView()?.setImageBitmap(bitmap)
    }

    override fun onError() {
        errorPlaceHolder?.let {
            getView()?.setImageDrawable(errorPlaceHolder)
        } ?: run {
            getView()?.visibility = View.GONE
        }
    }

    override fun getId() = view.get().hashCode()

    private fun getView() = view.get()

}