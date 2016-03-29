package com.frogdesign.akart

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService
import com.parrot.arsdk.ardiscovery.ARDiscoveryService
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate

import rx.Observable
import rx.Subscriber
import rx.functions.Action1
import timber.log.Timber

class Discovery(ctx: Context) {

    private val ctx: Context
    private var currentSubscriber: Subscriber<in Discovery>? = null
    private val mArdiscoveryServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Timber.tag(TAG).i("nexted" + currentSubscriber!!)
            mArdiscoveryService = (service as ARDiscoveryService.LocalBinder).service
            if (currentSubscriber != null) {
                currentSubscriber!!.onNext(this@Discovery)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mArdiscoveryService = null
        }
    }
    private var mArdiscoveryService: ARDiscoveryService? = null
    private var mArdiscoveryServicesDevicesListUpdatedReceiver: ARDiscoveryServicesDevicesListUpdatedReceiver? = null

    init {
        this.ctx = ctx.applicationContext
    }

    fun binder(): Observable<Discovery> {
        return Observable.create { subscriber ->
            Timber.i("Subscribed on binder " + mArdiscoveryService)
            // if the discoverer service doesn't exists, bind to it
            if (mArdiscoveryService == null) {
                currentSubscriber = subscriber
                val i = Intent(ctx, ARDiscoveryService::class.java)
                ctx.bindService(i, mArdiscoveryServiceConnection, Context.BIND_AUTO_CREATE)
            } else {
                subscriber.onNext(this@Discovery)
                subscriber.onCompleted()
            }
        }
    }

    private val discoverer = Observable.OnSubscribe<List<com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService>> {
        subscriber ->
        unbind()
        binder().subscribe(rx.functions.Action1<com.frogdesign.akart.Discovery> {
            Timber.tag(TAG).i("nexted call")
            val delegate = com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {
                Timber.tag(TAG).i( "onServicesDevicesListUpdated ...")
                if (mArdiscoveryService != null) {
                    val deviceList = mArdiscoveryService!!.deviceServicesArray
                    subscriber.onNext(deviceList)
                }
            }
            //first update is immediate
            delegate.onServicesDevicesListUpdated()
            mArdiscoveryServicesDevicesListUpdatedReceiver = ARDiscoveryServicesDevicesListUpdatedReceiver(delegate)
            val localBroadcastMgr = LocalBroadcastManager.getInstance(ctx)
            localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver,
                    IntentFilter(
                            ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated))
            mArdiscoveryService!!.start()
        })
    }

    fun discoverer(): Observable<List<ARDiscoveryDeviceService>> {
        return Observable.create(discoverer)
    }

    fun unbind() {
        unregisterReceivers()
        Timber.tag(TAG).i( "closeServices ...")
        if (mArdiscoveryService != null) {
            ctx.unbindService(mArdiscoveryServiceConnection)
            mArdiscoveryService!!.stop()
        }
        mArdiscoveryService = null
    }

    private fun unregisterReceivers() {
        val localBroadcastMgr = LocalBroadcastManager.getInstance(ctx)
        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver)
    }

    companion object {
        private val TAG = Discovery::class.java.simpleName
    }
}