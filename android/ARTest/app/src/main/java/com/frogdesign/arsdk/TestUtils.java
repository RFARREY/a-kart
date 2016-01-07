package com.frogdesign.arsdk;

import android.os.SystemClock;
import android.util.Log;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;


public class TestUtils {
    public static Observable<byte[]> constantProducer(final byte[] bmp, final long interval) {
        return Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(final Subscriber<? super byte[]> subscriber) {
                while (!subscriber.isUnsubscribed()) {
                    SystemClock.sleep(interval);
                    subscriber.onNext(bmp);
                    //Log.d("SEND", "SEND");
                }
                subscriber.onCompleted();
            }
        });
    }

    private static List<ARDiscoveryDeviceService> fakeServices = Arrays.asList(
            new ARDiscoveryDeviceService("fake0", null, 0),
            new ARDiscoveryDeviceService("fake1", null, 1),
            new ARDiscoveryDeviceService("fake2", null, 2),
            new ARDiscoveryDeviceService("fake3", null, 3)
    );

    public static Observable<List<ARDiscoveryDeviceService>> constantDiscoverer() {
        return Observable.create(new Observable.OnSubscribe<List<ARDiscoveryDeviceService>>() {
            @Override
            public void call(final Subscriber<? super List<ARDiscoveryDeviceService>> subscriber) {
                int lastIdx = fakeServices.size() > 0 ? 1 : 0;
                while (lastIdx <= fakeServices.size()) {
                    SystemClock.sleep(1000);
                    subscriber.onNext(fakeServices.subList(0, lastIdx));
                    lastIdx++;
                }
                subscriber.onCompleted();
            }
        });
    }
}
