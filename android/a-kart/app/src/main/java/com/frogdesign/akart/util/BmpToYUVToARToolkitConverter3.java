package com.frogdesign.akart.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Debug;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;

import com.frogdesign.akart.ScriptC_argb_to_yuv;

import org.artoolkit.ar.base.ARToolKit;
import org.artoolkit.ar.base.Utils;

import rx.functions.Func1;


public class BmpToYUVToARToolkitConverter3 implements Func1<Bitmap, Boolean> {
    private int[] argbBuffer = null;
    private byte[] yuvBuffer = null;

    private void checkForBuffers(int w,int h) {
        int argbLength = w * h;
        if (argbBuffer == null || argbBuffer.length != argbLength)
        argbBuffer = new int[argbLength];

        int yuvLength = Utils.yuvByteLength(w, h);
        if (yuvBuffer == null || yuvBuffer.length != yuvLength)
        yuvBuffer = new byte[yuvLength];
    }

    public Boolean call(Bitmap inBitmap) {
        if (inBitmap == null) return false;
        //Log.i(PlayActivity.TAG, "convert" + isMainThread())
        int w = inBitmap.getWidth();
        int h = inBitmap.getHeight();
        checkForBuffers(w, h);
        inBitmap.getPixels(argbBuffer, 0, w, 0, 0, w, h);
        Utils.encodeYUV420SP(yuvBuffer, argbBuffer, w, h);
        if (ARToolKit.getInstance().nativeInitialised()) ARToolKit.getInstance().convertAndDetect(yuvBuffer);
        return true;
    }
}