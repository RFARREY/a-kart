package com.frogdesign.akart.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Log

import com.frogdesign.akart.ScriptC_argb_to_yuv

import org.artoolkit.ar.base.ARToolKit

import rx.functions.Func1


class BmpToYUVToARToolkitConverter2(ctx: Context) : Func1<Bitmap, Boolean> {
    private var yuvBuffer: ByteArray? = null

    private var inAllocation: Allocation? = null
    private var outAllocation: Allocation? = null
    private val rs: RenderScript
    private val script: ScriptC_argb_to_yuv

    init {
        rs = RenderScript.create(ctx)
        script = ScriptC_argb_to_yuv(rs)
    }

    private fun checkForBuffers(b: Bitmap) {

        val w = b.width
        val h = b.height
        val yuvLength = yuvByteLength(w, h)
        if (yuvBuffer == null || yuvBuffer!!.size != yuvLength) {

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
        //Log.i(PlayActivity.TAG, "convert" + isMainThread())
        checkForBuffers(inBitmap)

        inAllocation!!.copyFrom(inBitmap)

        //long startT = System.nanoTime();
        script.invoke_filter()
        //script.forEach_rgbToYuv(inAllocation);
        //long dt = System.nanoTime() - startT;
        //Log.i("TIME", Long.toString(dt));

        outAllocation!!.copyTo(yuvBuffer)

        if (ARToolKit.getInstance().nativeInitialised()) {
            ARToolKit.getInstance().convertAndDetect(yuvBuffer)
        }
        return true
    }
}