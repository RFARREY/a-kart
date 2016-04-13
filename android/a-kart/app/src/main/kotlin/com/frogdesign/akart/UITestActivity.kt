package com.frogdesign.akart

import android.app.Activity
import android.os.Bundle
import com.frogdesign.akart.util.SteeringWheel
import com.frogdesign.akart.util.isMainThread
import kotlinx.android.synthetic.main.ui_test_activity.*
import timber.log.Timber

class UITestActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ui_test_activity)
        gasPedal.events.subscribe { event ->
            Timber.i("EVENT %s", gasPedal.level)
        }
        SteeringWheel(this).stream().subscribe { steer ->
            aim.horizonRotate(steer)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}

