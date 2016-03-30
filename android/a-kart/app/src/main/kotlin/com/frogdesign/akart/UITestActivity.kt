package com.frogdesign.akart

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.frogdesign.akart.util.TrackedSubscriptions
import timber.log.Timber

class UITestActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ui_test_activity)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}

