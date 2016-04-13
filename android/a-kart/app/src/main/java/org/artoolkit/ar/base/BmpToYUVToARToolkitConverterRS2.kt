package org.artoolkit.ar.base

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import com.frogdesign.akart.ARMarkerDetector
import com.frogdesign.akart.ScriptC_argb_to_yuv
import com.frogdesign.akart.util.yuvByteLength
import rx.functions.Func1
import timber.log.Timber


class BmpToYUVToARToolkitConverterRS2(ctx: Context) : Func1<Bitmap, Boolean> {

    private var yuvBuffer: ByteArray? = null
    private var inAllocation: Allocation? = null
    private var outAllocation: Allocation? = null
    private val rs: RenderScript
    private val script: ScriptC_argb_to_yuv

    init {
        rs = RenderScript.create(ctx)
        script = ScriptC_argb_to_yuv(rs)
    }

    private fun checkForBuffers(c: Bitmap) {
        var x = (c.width - ARMarkerDetector.WIDTH) / 2
        var y = (c.height - ARMarkerDetector.HEIGHT) / 2
        val w = ARMarkerDetector.WIDTH
        val h = ARMarkerDetector.HEIGHT
        val yuvLength = yuvByteLength(w, h)
        if (yuvBuffer == null || yuvBuffer!!.size != yuvLength) {
            Timber.i("Creating buf x: %s, y: %s, w: %s, h: %s", x, y, w, h)
            var b = Bitmap.createBitmap(c, x, y, w, h)
            inAllocation = Allocation.createFromBitmap(rs, b,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)
            script._gInImage = inAllocation

            yuvBuffer = ByteArray(yuvLength)

            outAllocation = Allocation.createSized(rs, Element.U8(rs), yuvBuffer!!.size, Allocation.USAGE_SCRIPT)

            script.bind_outBytes(outAllocation)
            script._width = w
            script._height = h
            script._frameSize = w * h
        }
    }

    override fun call(inBitmap: Bitmap?): Boolean? {
        if (inBitmap == null) return false
        //Timber.i(PlayActivity.TAG, "convert" + isMainThread())
        checkForBuffers(inBitmap)

        inAllocation!!.copyFrom(inBitmap)

        script.invoke_filter()

        outAllocation!!.copyTo(yuvBuffer)

        if (ARToolKit.getInstance().nativeInitialised()) {
            ARToolKit.getInstance().convertAndDetect(yuvBuffer)
        }
        return true
    }
}