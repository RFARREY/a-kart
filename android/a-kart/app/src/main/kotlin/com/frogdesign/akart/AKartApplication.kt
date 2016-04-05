package com.frogdesign.akart

import android.app.Application
import android.util.Log
import com.parrot.arsdk.ARSDK

import org.artoolkit.ar.base.assets.AssetHelper
import timber.log.Timber
import uk.co.chrisjenx.calligraphy.CalligraphyConfig

class AKartApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeInstance()
    }

    protected fun initializeInstance() {

        // Unpack assets to cache directory so native library can read them.
        // N.B.: If contents of assets folder changes, be sure to increment the
        // versionCode integer in the AndroidManifest.xml file.
        val assetHelper = AssetHelper(assets)
        assetHelper.cacheAssetFolder(this, "Data")

        CalligraphyConfig.initDefault(CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Menlo-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build())

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree());
        } else {
            Timber.plant(CrashReportingTree());
        }
    }

    companion object {
        private val TAG = AKartApplication::class.java.simpleName

        init {
            try {
                ARSDK.loadSDKLibs()
            } catch (e: Exception) {
                Timber.e(TAG, "Problem occured during native library loading", e)
            }
        }
    }

    /** A tree which logs important information for crash reporting. */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return;
            }

            when (priority) {
                Log.VERBOSE -> Timber.v(message, t)
                Log.DEBUG -> Timber.d(tag, message, t)
                Log.INFO -> Timber.i(tag, message, t)
                Log.WARN -> Timber.w(tag, message, t)
                Log.ERROR -> Timber.e(tag, message, t)
                Log.ASSERT -> Timber.wtf(tag, message, t)
            }
        }
    }
}
