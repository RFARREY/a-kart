package com.frogdesign.akart

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import com.frogdesign.akart.model.Car
import com.frogdesign.akart.view.AimView

interface MarkerDetector : Application.ActivityLifecycleCallbacks {

    fun setup(ctx : Context, cars: List<Car>)
    fun process(inBitmap: Bitmap?)
    fun setTarget(forCar: Car, webcamToScreenTransf: Matrix, targets: AimView)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}