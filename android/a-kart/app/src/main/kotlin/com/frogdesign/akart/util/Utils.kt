package com.frogdesign.akart.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import org.artoolkit.ar.base.ARToolKit
import org.artoolkit.ar.base.ByteUtils
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.Executors


public fun dpToPx(context: Context, dp: Float): Int = Math.round(dp * pixelScaleFactor(context))

private fun pixelScaleFactor(context: Context): Float {
    var displayMetrics = context.resources.displayMetrics
    var mdpi = DisplayMetrics.DENSITY_DEFAULT.toFloat()
    return displayMetrics.densityDpi / mdpi
}

public fun threadName(): String = Thread.currentThread().name

public fun isMainThread(): Boolean = Looper.getMainLooper() == Looper.myLooper()

val PI: Float = Math.PI.toFloat()

fun clamp(value: Float, min: Float, max: Float): Float {
    if (value < min) return min
    else if (value > max) return max
    else return value
}

fun clamp(value: Double, min: Double, max: Double): Double {
    if (value < min) return min
    else if (value > max) return max
    else return value
}

fun inrange(value: Double, min: Double, max: Double): Boolean = value >= min && value <= max


class CachedBitmapDecoder : Func1<ByteArray, Bitmap> {
    private var inBitmap: Bitmap? = null
    private val opts = BitmapFactory.Options()

    init {
        opts.inMutable = true
        //opts.inPreferredConfig = Bitmap.Config.RGB_565;
    }


    override fun call(data: ByteArray?): Bitmap? {
        opts.inBitmap = inBitmap
        if (data != null) inBitmap = BitmapFactory.decodeByteArray(data, 0, data.size, null)
        var result = inBitmap
        return result
    }
}

class BmpToYUVToARToolkitConverterJava : Func1<Bitmap, Boolean> {
    private var argbBuffer: IntArray? = null
    private var yuvBuffer: ByteArray? = null

    private fun checkForBuffers(w: Int, h: Int) {
        val argbLength = w * h
        if (argbBuffer == null || argbBuffer!!.size != argbLength)
            argbBuffer = IntArray(argbLength)

        val yuvLength = yuvByteLength(w, h)
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
        //var startT = System.nanoTime();
        ByteUtils.encodeYUV420SP(yuvBuffer, argbBuffer, w, h)
        //var dt = System.nanoTime() - startT;
        //Log.i("TIME", dt.toString());
        if (ARToolKit.getInstance().nativeInitialised()) {
            var b = ARToolKit.getInstance().convertAndDetect(yuvBuffer)
            //Log.i("Utils", "ARToolkit $b");
        }
        return true
    }
}


class TrackedSubscriptions : ArrayList<Subscription>() {

    public fun track(sub: Subscription?): TrackedSubscriptions {
        if (sub != null) super.add(sub)
        return this
    }

    public fun unsubAll(): TrackedSubscriptions {
        for (a in this)
            if (a.isUnsubscribed) a.unsubscribe()

        this.clear();
        return this
    }
}

val executtor  by lazy {
    Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
}
val scheduler by lazy {
    Schedulers.from(executtor)
}

fun <T> Observable<T>.andAsync(): Observable<T> {
    return this.subscribeOn(scheduler)
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


fun yuvByteLength(w: Int, h: Int): Int = w * h * 3 / 2

fun encodeYUV420SP(yuv420sp: ByteArray?, argb: IntArray?, width: Int, height: Int) {
    if (argb == null || yuv420sp == null) return
    val frameSize = width * height

    var yIndex = 0
    var uvIndex = frameSize

    var /*a,*/ R: Int
    var G: Int
    var B: Int
    var Y: Int
    var U: Int
    var V: Int
    var index = 0

    var i = 0
    var j = 0
    while (j < height) {
        while (i < width) {

            //a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
            R = argb[index] and 16711680 shr 16
            G = argb[index] and 65280 shr 8
            B = argb[index] and 255

            // well known RGB to YUV algorithm
            Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
            U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
            V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128

            // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
            //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
            //    pixel AND every other scanline.
            yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
            if (j % 2 == 0 && index % 2 == 0) {
                yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
            }

            index++
            ++i
        }
        ++j
    }
}