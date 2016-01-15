package com.frogdesign.akart.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import org.artoolkit.ar.base.ARToolKit
import org.artoolkit.ar.base.Utils
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers
import java.util.*


public fun dpToPx(context: Context, dp: Float): Int = Math.round(dp * pixelScaleFactor(context))

private fun pixelScaleFactor(context: Context): Float {
    var displayMetrics = context.resources.displayMetrics
    var mdpi = DisplayMetrics.DENSITY_DEFAULT.toFloat()
    return displayMetrics.densityDpi / mdpi
}

public fun threadName(): String = Thread.currentThread().name

public fun isMainThread(): Boolean = Looper.getMainLooper() == Looper.myLooper()

public val PI: Float = Math.PI.toFloat()

public fun clamp(value: Float, min: Float, max: Float): Float {
    if (value < min) return min
    if (value > max) return max
    return value
}

class CachedBitmapDecoder : Func1<ByteArray, Bitmap> {
    private var inBitmap: Bitmap? = null
    private val opts = BitmapFactory.Options()

    init {
        opts.inMutable = true
    }

    override fun call(data: ByteArray?): Bitmap? {
        opts.inBitmap = inBitmap
        if (data != null) inBitmap = BitmapFactory.decodeByteArray(data, 0, data.size, opts)
        return inBitmap
    }
}

class BmpToYUVToARtoolkitConverter : Func1<Bitmap, Boolean> {
    private var argbBuffer: IntArray? = null
    private var yuvBuffer: ByteArray? = null

    private fun checkForBuffers(w: Int, h: Int) {
        val argbLength = w * h
        if (argbBuffer == null || argbBuffer!!.size != argbLength)
            argbBuffer = IntArray(argbLength)

        val yuvLength = Utils.yuvByteLength(w, h)
        if (yuvBuffer == null || yuvBuffer!!.size != yuvLength)
            yuvBuffer = ByteArray(yuvLength)
    }

    override fun call(inBitmap: Bitmap?): Boolean {
        if (inBitmap == null) return false;
        //Log.i(PlayActivity.TAG, "convert" + isMainThread())
        val w = inBitmap.width
        val h = inBitmap.height
        checkForBuffers(w, h)
        inBitmap.getPixels(argbBuffer, 0, w, 0, 0, w, h)
        Utils.encodeYUV420SP(yuvBuffer, argbBuffer, w, h)
        if (ARToolKit.getInstance().nativeInitialised()) ARToolKit.getInstance().convertAndDetect(yuvBuffer)
        return true
    }
}

class TrackedSubscriptions : ArrayList<Subscription>() {
    public fun track(sub: Subscription): TrackedSubscriptions {
        super.add(sub)
        return this
    }

    public fun unsubAll(): TrackedSubscriptions {
        for (a in this)
            if (a.isUnsubscribed) a.unsubscribe()

        this.clear();
        return this
    }
}

fun <T> Observable<T>.andAsync(): Observable<T> {
    return this.subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
}

fun View.hideNavbar() {
    this.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    .or(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                    .or(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                    .or(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                    .or(View.SYSTEM_UI_FLAG_FULLSCREEN)
                    .or(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
}