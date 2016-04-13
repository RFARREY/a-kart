package com.frogdesign.akart.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import rx.Observable
import rx.Subscriber
import rx.Subscription
//import rx.lang.kotlin.observable
import rx.observables.ConnectableObservable
import rx.observers.Subscribers
import rx.subscriptions.Subscriptions
import timber.log.Timber

class SteeringWheel(context: Context) : SensorEventListener {

    val TAG: String = "SteeringWheel"

    val sensorManager: SensorManager
    val magnet: FloatArray
    val accel: FloatArray
    val orientation: FloatArray
    val rotation: FloatArray
    var sensorRegistered: Boolean = false

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnet = FloatArray(3)
        accel = FloatArray(3)
        orientation = FloatArray(3)
        rotation = FloatArray(9)
    }

    private fun setListeners() {
        if (!sensorRegistered) {
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL)
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_NORMAL)
            sensorRegistered = true
        }
    }

    private fun unregister() {
        if (sensorRegistered) sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, accel, 0, 3)
            Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, magnet, 0, 3)
        }

        SensorManager.getRotationMatrix(rotation, null, accel, magnet)
        SensorManager.getOrientation(rotation, orientation)
        var theValue = orientation[1] / PI * 180.0f
        steer?.onNext(theValue)
    }

    private val obs: ConnectableObservable<Float>
    private var steer: Subscriber<in Float>? = null
    private val refCount: Observable<Float>

    init {
        obs = Observable.create<Float> { sub ->
            steer = sub
            setListeners()
            sub.add(Subscriptions.create {
                Timber.tag(TAG).i( "unsub!")
                steer = sub
                unregister()
            })
            sub.onNext(0f)
        }.publish()

        refCount = obs.refCount()
    }

    public fun stream(): Observable<Float> = refCount
}