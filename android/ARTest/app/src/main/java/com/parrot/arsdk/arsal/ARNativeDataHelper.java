package com.parrot.arsdk.arsal;

import com.parrot.arsdk.arcontroller.ARFrame;

public final class ARNativeDataHelper {
    static {
        System.loadLibrary("akart");
    }

    public static void copyData(ARFrame src, byte[] dest) {
        copyData(src.pointer, src.capacity, src.used, dest);
    }
    public static native void copyData(long data, int capacity, int used, byte[] dest);
}
