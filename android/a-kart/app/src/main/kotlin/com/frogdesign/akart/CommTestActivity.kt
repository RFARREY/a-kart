package com.frogdesign.akart

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.frogdesign.akart.util.TrackedSubscriptions
import timber.log.Timber

class CommTestActivity : Activity() {

    private val trackedSubs = TrackedSubscriptions()
    private var comm: Comm = Comm("taxiguerrilla", this, "http://10.228.81.53:5000")

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.accel_test_activity)
        //comm = Comm("taxiguerrilla", this, "http://10.228.81.53:5000")
        trackedSubs.add(comm.pin.subscribe { event ->
            Timber.i("event " + event)
        })

        comm.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        trackedSubs.unsubAll()
        comm.close();
    }

}

