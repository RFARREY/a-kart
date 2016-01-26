package org.artoolkit.ar.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;

import com.frogdesign.akart.ScriptC_argb_to_yuv;
import com.frogdesign.akart.util.UtilsKt;

import rx.functions.Func1;

/**
 * Created by emanuele.di.saverio on 26/01/16.
 */

public class Bmp2 implements Func1<Bitmap, Boolean> {
    private byte[] yuvBuffer = null;

    private Allocation inAllocation;
    private Allocation outAllocation;
    private RenderScript rs;
    private ScriptC_argb_to_yuv script;

    public Bmp2(Context ctx) {
        rs = RenderScript.create(ctx);
        script = new ScriptC_argb_to_yuv(rs);
    }

    private void checkForBuffers(Bitmap b) {

        int w = b.getWidth();
        int h = b.getHeight();
        int yuvLength = UtilsKt.yuvByteLength(w, h);
        if (yuvBuffer == null || yuvBuffer.length != yuvLength) {

            inAllocation = Allocation.createFromBitmap(rs, b,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            script.set_gInImage(inAllocation);

            yuvBuffer = new byte[yuvLength];

            outAllocation = Allocation.createSized(rs, Element.U8(rs), yuvBuffer.length, Allocation.USAGE_SCRIPT);

            script.bind_outBytes(outAllocation);
            script.set_width(w);
            script.set_height(h);
            script.set_frameSize(w * h);
        }
    }

    @Override
    public Boolean call(Bitmap inBitmap) {
        if (inBitmap == null) return false;
        //Log.i(PlayActivity.TAG, "convert" + isMainThread())
        checkForBuffers(inBitmap);

        inAllocation.copyFrom(inBitmap);

        script.invoke_filter();

        outAllocation.copyTo(yuvBuffer);

        if (ARToolKit.getInstance().nativeInitialised()) {
            ARToolKit.getInstance().convertAndDetect(yuvBuffer);
        }
        return true;
    }
}