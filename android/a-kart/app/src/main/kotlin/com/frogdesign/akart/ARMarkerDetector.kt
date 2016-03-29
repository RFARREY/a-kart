package com.frogdesign.akart

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.frogdesign.akart.model.Car
import com.frogdesign.akart.model.Cars
import com.frogdesign.akart.view.AimView
import org.artoolkit.ar.base.ARToolKit
import org.artoolkit.ar.base.BmpToYUVToARToolkitConverterRS2
import org.artoolkit.ar.base.NativeInterface
import timber.log.Timber


class ARMarkerDetector : MarkerDetector {

    private val values = FloatArray(9)
    companion object {
        private val TAG = ARMarkerDetector::class.java.simpleName
    }

    private var converter: BmpToYUVToARToolkitConverterRS2? = null;

    override fun setup(ctx: Context, cars: List<Car>) {
    }

    override fun setTarget(forCar: Car, webcamToScreenTransf: Matrix, targets: AimView) {
        if (forCar.isDetected(ARToolKit.getInstance())) {
            val p = forCar.estimatePosition(ARToolKit.getInstance())
            Timber.i("Car visibile! " + forCar.id)
            Timber.i("Position: " + forCar.estimatePosition(ARToolKit.getInstance()))

            webcamToScreenTransf.getValues(values)
            val x = p.x + targets.width / 2 - values[2]
            val y = -p.y + targets.height / 2;
            targets.setTarget(forCar.id, x, y)
        }
    }

    override fun process(inBitmap: Bitmap?) {
        converter?.call(inBitmap)
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity is PlayActivity) converter = BmpToYUVToARToolkitConverterRS2(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity is PlayActivity) converter = null
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity !is PlayActivity) return
        super.onActivityResumed(activity)

        if (!ARToolKit.getInstance().initialiseNativeWithOptions(activity.cacheDir.absolutePath, 16, 25)) {
            throw RuntimeException("e' tutto finito")
        }

        NativeInterface.arwSetPatternDetectionMode(NativeInterface.AR_MATRIX_CODE_DETECTION)
        NativeInterface.arwSetMatrixCodeType(NativeInterface.AR_MATRIX_CODE_3x3_HAMMING63)


        for (c in Cars.all) {
            val leftId = ARToolKit.getInstance().addMarker("single_barcode;" + c.lrMarkers.first + ";80")
            val rightId = ARToolKit.getInstance().addMarker("single_barcode;" + c.lrMarkers.second + ";80")
            c.leftAR = leftId
            c.rightAR = rightId
            Timber.i("Car added!"+c.id)
        }
        ARToolKit.getInstance().initialiseAR(640, 480, "Data/camera_para.dat", 0, true)
        Timber.i("ARToolkit initialized!")
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity !is PlayActivity) return
        super.onActivityPaused(activity)
        ARToolKit.getInstance().cleanup()
    }
}