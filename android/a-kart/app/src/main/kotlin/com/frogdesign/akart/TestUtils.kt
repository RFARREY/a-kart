package com.frogdesign.akart

import android.os.SystemClock
import android.util.Log

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService

import java.util.Arrays

import rx.Observable
import rx.Observer
import rx.Subscriber


object TestUtils {
    fun constantProducer(bmp: ByteArray, interval: Long): Observable<ByteArray> {
        return Observable.create { subscriber ->
            while (!subscriber.isUnsubscribed) {
                SystemClock.sleep(interval)
                subscriber.onNext(bmp)                    //Timber.d("SEND", "SEND");
            }
            subscriber.onCompleted()
        }
    }

    private val fakeServices = Arrays.asList(
            ARDiscoveryDeviceService("fake0", null, 0),
            ARDiscoveryDeviceService("fake1", null, 1),
            ARDiscoveryDeviceService("fake2", null, 2),
            ARDiscoveryDeviceService("fake3", null, 3))

    fun constantDiscoverer(): Observable<List<ARDiscoveryDeviceService>> {
        return Observable.create { subscriber ->
            var lastIdx = if (fakeServices.size > 0) 1 else 0
            while (lastIdx <= fakeServices.size) {
                SystemClock.sleep(1000)
                subscriber.onNext(fakeServices.subList(0, lastIdx))
                lastIdx++
            }
            subscriber.onCompleted()
        }
    }
}
