package com.frogdesign.akart;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * Created by emanuele.di.saverio on 17/12/15.
 */
public class Discovery {
    private static String TAG = Discovery.class.getSimpleName();

    private Context ctx;
    private Subscriber<? super Discovery> currentSubscriber;
    private ServiceConnection mArdiscoveryServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "nexted" + currentSubscriber);
            mArdiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();
            if (currentSubscriber != null) {
                currentSubscriber.onNext(Discovery.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mArdiscoveryService = null;
        }
    };
    private ARDiscoveryService mArdiscoveryService;
    private ARDiscoveryServicesDevicesListUpdatedReceiver
            mArdiscoveryServicesDevicesListUpdatedReceiver;

    public Discovery(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public Observable<Discovery> binder() {
        return Observable.create(new Observable.OnSubscribe<Discovery>() {
            @Override
            public void call(Subscriber<? super Discovery> subscriber) {
                Log.i(TAG, "Subscribed on binder " + mArdiscoveryService);
                // if the discoverer service doesn't exists, bind to it
                if (mArdiscoveryService == null) {
                    currentSubscriber = subscriber;
                    Intent i = new Intent(ctx, ARDiscoveryService.class);
                    ctx.bindService(i, mArdiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
                } else {
                    subscriber.onNext(Discovery.this);
                    subscriber.onCompleted();
                }
            }
        });
    }

    private Observable.OnSubscribe<List<ARDiscoveryDeviceService>> discoverer =
            new Observable.OnSubscribe<List<ARDiscoveryDeviceService>>() {
                @Override
                public void call(final Subscriber<? super List<ARDiscoveryDeviceService>> subscriber) {
                    unbind();
                    binder().subscribe(new Action1<Discovery>() {
                        @Override
                        public void call(Discovery discovery) {
                            Log.i(TAG, "nexted call");
                            ARDiscoveryServicesDevicesListUpdatedReceiverDelegate delegate =
                                    new ARDiscoveryServicesDevicesListUpdatedReceiverDelegate() {
                                        @Override
                                        public void onServicesDevicesListUpdated() {
                                            Log.d(TAG, "onServicesDevicesListUpdated ...");
                                            if (mArdiscoveryService != null) {
                                                List<ARDiscoveryDeviceService> deviceList =
                                                        mArdiscoveryService.getDeviceServicesArray();
                                                subscriber.onNext(deviceList);
                                            }
                                        }
                                    };
                            //first update is immediate
                            delegate.onServicesDevicesListUpdated();
                            mArdiscoveryServicesDevicesListUpdatedReceiver =
                                    new ARDiscoveryServicesDevicesListUpdatedReceiver(delegate);
                            LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(ctx);
                            localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver,
                                    new IntentFilter(
                                            ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated)

                            );
                            mArdiscoveryService.start();
                        }
                    });
                }
            };

    public Observable<List<ARDiscoveryDeviceService>> discoverer() {
        return Observable.create(discoverer);
    }

    public void unbind() {
        unregisterReceivers();
        Log.d(TAG, "closeServices ...");
        if (mArdiscoveryService != null) {
            ctx.unbindService(mArdiscoveryServiceConnection);
            mArdiscoveryService.stop();
        }
        mArdiscoveryService = null;
    }

    private void unregisterReceivers() {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(ctx);
        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver);
    }
}