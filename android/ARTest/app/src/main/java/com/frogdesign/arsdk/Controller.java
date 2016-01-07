package com.frogdesign.arsdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARDeviceControllerStreamListener;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;

import java.io.ByteArrayInputStream;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;


/**
 *
 */
public class Controller implements ARDeviceControllerListener {
    private static String TAG = Controller.class.getSimpleName();
    private ARDeviceController deviceController;
    private static final byte ON = (byte) 1;
    private static final byte OFF = (byte) 0;

    public Controller(Context ctx, ARDiscoveryDeviceService service) {
        if (service == null) throw new IllegalArgumentException("Cannot connect to null service.");
        int productIdInt = service.getProductID();
        ARDISCOVERY_PRODUCT_ENUM productId = ARDiscoveryService.getProductFromProductID(productIdInt);
        Log.i(TAG, "ProductId: " + productId.toString());
        if (ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_JS == productId) {
            try {
                ARDiscoveryDevice device = new ARDiscoveryDevice();
                ARDiscoveryDeviceNetService netDeviceService =
                        (ARDiscoveryDeviceNetService) service.getDevice();

                device.initWifi(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_JS,
                        netDeviceService.getName(), netDeviceService.getIp(), netDeviceService.getPort());

                deviceController = new ARDeviceController(device);

                deviceController.addListener(this);
            } catch (ARDiscoveryException e) {
                e.printStackTrace();
                Log.e(TAG, "Error: " + e.getError());
            } catch (ARControllerException e) {
                e.printStackTrace();
            }
        } else {

        }
    }

    public void start() {
        ARCONTROLLER_ERROR_ENUM error = deviceController.start();
        if (error.getValue() != 0) {
            throw new RuntimeException("Cannot start Controller for");
        }
    }

    public void stop() {
        ARCONTROLLER_ERROR_ENUM error = deviceController.stop();
        if (error.getValue() != 0) {
            throw new RuntimeException("Cannot stop Controller for");
        }
    }

    public Observable<byte[]> mediaStreamer() {
        return Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(final Subscriber<? super byte[]> subscriber) {
                final ARDeviceControllerStreamListener listener = new ARDeviceControllerStreamListener() {
                    @Override
                    public void onFrameReceived(ARDeviceController arDeviceController, ARFrame arFrame) {
                        if (subscriber == null) return;
                        if (!arFrame.isIFrame())
                            return;
                        byte[] data = arFrame.getByteData();
                        subscriber.onNext(data);
                    }

                    @Override
                    public void onFrameTimeout(ARDeviceController arDeviceController) {
                        Log.i(TAG, "onFrameTimeout");
                    }
                };
                deviceController.addStreamListener(listener);
                deviceController.getFeatureJumpingSumo().sendMediaStreamingVideoEnable(ON);
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        deviceController.removeStreamListener(listener);
                        deviceController.getFeatureJumpingSumo().sendMediaStreamingVideoEnable(OFF);
                    }
                }));
            }
        });
    }@Override
    // called when the state of the device controller has changed
    public void onStateChanged(ARDeviceController deviceController,
                               ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error) {
        switch (newState) {
            case ARCONTROLLER_DEVICE_STATE_RUNNING:
                break;
            case ARCONTROLLER_DEVICE_STATE_STOPPED:
                break;
            case ARCONTROLLER_DEVICE_STATE_STARTING:
                break;
            case ARCONTROLLER_DEVICE_STATE_STOPPING:
                break;

            default:
                break;
        }
    }

    @Override
    public void onExtensionStateChanged(ARDeviceController arDeviceController,
                                        ARCONTROLLER_DEVICE_STATE_ENUM arcontroller_device_state_enum,
                                        ARDISCOVERY_PRODUCT_ENUM ardiscovery_product_enum, String s,
                                        ARCONTROLLER_ERROR_ENUM arcontroller_error_enum) {

    }

    @Override
    // called when a command has been received from the drone
    public void onCommandReceived(ARDeviceController deviceController,
                                  ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {
        if (elementDictionary != null) {
            // if the command received is a battery state changed
            if (commandKey
                    == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED) {
                ARControllerArgumentDictionary<Object> args =
                        elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);

                if (args != null) {
                    Integer batValue = (Integer) args.get(
                            ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED_PERCENT);

                    // do what you want with the battery level
                    Log.i(TAG, "Battery: " + batValue);
                }
            }
        } else {
            Log.e(TAG, "elementDictionary is null");
        }
    }
}
