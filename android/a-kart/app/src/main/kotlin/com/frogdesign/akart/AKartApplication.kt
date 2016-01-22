package com.frogdesign.akart

import android.app.Application
import android.util.Log

import org.artoolkit.ar.base.assets.AssetHelper

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
    }

    companion object {
        private val TAG = AKartApplication::class.java.simpleName

        init {
            try {
                System.loadLibrary("arsal")
                System.loadLibrary("arsal_android")
                System.loadLibrary("arnetworkal")
                System.loadLibrary("arnetworkal_android")
                System.loadLibrary("arnetwork")
                System.loadLibrary("arnetwork_android")
                System.loadLibrary("arcommands")
                System.loadLibrary("arcommands_android")
                System.loadLibrary("arstream")
                System.loadLibrary("arstream_android")
                System.loadLibrary("json")
                System.loadLibrary("ardiscovery")
                System.loadLibrary("ardiscovery_android")
                System.loadLibrary("arutils")
                System.loadLibrary("arutils_android")
                System.loadLibrary("ardatatransfer")
                System.loadLibrary("ardatatransfer_android")
                System.loadLibrary("armedia")
                System.loadLibrary("armedia_android")
                System.loadLibrary("arcontroller")
                System.loadLibrary("arcontroller_android")
            } catch (e: Exception) {
                Log.e(TAG, "Problem occured during native library loading", e)
            }
        }
    }
}
