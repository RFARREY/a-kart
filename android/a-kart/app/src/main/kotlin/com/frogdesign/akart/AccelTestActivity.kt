package com.frogdesign.akart

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

class AccelTestActivity : Activity() {
    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.accel_test_activity)

        val sensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val mValuesMagnet = FloatArray(3)
        val mValuesAccel = FloatArray(3)
        val mValuesOrientation = FloatArray(3)
        val mRotationMatrix = FloatArray(9)

        val btn_valider = findViewById(R.id.btn1) as Button
        val txt1 = findViewById(R.id.textView1) as TextView
        val mEventListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            }

            override fun onSensorChanged(event: SensorEvent) {
                // Handle the events for which we registered
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, mValuesAccel, 0, 3)

                    Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, mValuesMagnet, 0, 3)
                }
            }
        }

        // You have set the event lisetner up, now just need to register this with the
        // sensor manager along with the sensor wanted.
        setListners(sensorManager, mEventListener)

        btn_valider.setOnClickListener {
            SensorManager.getRotationMatrix(mRotationMatrix, null, mValuesAccel, mValuesMagnet)
            SensorManager.getOrientation(mRotationMatrix, mValuesOrientation)
            var test: CharSequence
            test = "results: " + mValuesOrientation[0] + " " + mValuesOrientation[1] + " " + mValuesOrientation[2]
            test = "deg " + mValuesOrientation[1] / Math.PI * 180
            txt1.text = test
        }

    }

    // Register the event listener and sensor type.
    fun setListners(sensorManager: SensorManager, mEventListener: SensorEventListener) {
        sensorManager.registerListener(mEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(mEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL)
    }
}

