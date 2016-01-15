package com.frogdesign.arsdk;

import android.content.Context;
import android.util.Log;

import com.frogdesign.akart.util.UtilsKt;
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
import com.parrot.arsdk.arcontroller.ARFeatureJumpingSumo;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.arsal.ARNativeDataHelper;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;


/**
 *
 */
public class Controller implements ARDeviceControllerListener {
    private static String TAG = Controller.class.getSimpleName();
    private static boolean DEBUG = true;


    private static final byte ON = (byte) 1;
    private static final byte OFF = (byte) 0;

    private static final byte TURN_MAX = 50;
    private static final byte TURN_DEADZONE = 10;
    private static final byte SPEED_MAX = 50;

    private final ARDeviceController deviceController;
    private final ARFeatureJumpingSumo jumpingSumo;

    private Integer batteryLevel = -1;

    public Controller(Context ctx, ARDiscoveryDeviceService service) {
        if (service == null) throw new IllegalArgumentException("Cannot connect to null service.");
        int productIdInt = service.getProductID();
        ARDISCOVERY_PRODUCT_ENUM productId = ARDiscoveryService.getProductFromProductID(productIdInt);
        trace("ProductId: %s", productId.toString());

        if (ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_JS == productId) {
            try {
                ARDiscoveryDevice device = new ARDiscoveryDevice();
                ARDiscoveryDeviceNetService netDeviceService =
                        (ARDiscoveryDeviceNetService) service.getDevice();

                device.initWifi(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_JS,
                        netDeviceService.getName(),
                        netDeviceService.getIp(),
                        netDeviceService.getPort());

                deviceController = new ARDeviceController(device);
                deviceController.addListener(this);
                jumpingSumo = deviceController.getFeatureJumpingSumo();

            } catch (ARDiscoveryException | ARControllerException e) { //multicatch
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("I can only drive JumpingSumos!");
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
                trace("mediaStreamer.OnSusbascribe: MAIN? ", UtilsKt.isMainThread());
                final ARDeviceControllerStreamListener listener = new ARDeviceControllerStreamListener() {

                    private byte[] data = new byte[80000];

                    @Override
                    public void onFrameReceived(ARDeviceController arDeviceController, ARFrame arFrame) {
                        trace("mediaStreamer.onFrameReceived: MAIN? %b", UtilsKt.isMainThread());
                        if (subscriber == null) return;
                        if (!arFrame.isIFrame())
                            return;
                        if (data == null) {
                            data = arFrame.getByteData();
                        } else ARNativeDataHelper.copyData(arFrame, data);

                        subscriber.onNext(data);
                    }

                    @Override
                    public void onFrameTimeout(ARDeviceController arDeviceController) {
                        Log.w(TAG, "onFrameTimeout");
                    }
                };
                deviceController.addStreamListener(listener);
                deviceController.getFeatureJumpingSumo().sendMediaStreamingVideoEnable(ON);
                subscriber.add(Subscriptions.create(() -> {
                    trace("mediastreamer.unsubscribe");
                    deviceController.removeStreamListener(listener);
                    deviceController.getFeatureJumpingSumo().sendMediaStreamingVideoEnable(OFF);
                }));
            }
        });
    }

    public void speed(float percentage) {
        byte actual = (byte) (SPEED_MAX * percentage);
        jumpingSumo.setPilotingPCMDSpeed(actual);
        jumpingSumo.setPilotingPCMDFlag(ON);
    }

    public void turn(float percentage) {
        byte dataToBeSent = (byte) (-TURN_MAX * percentage);
        trace("turn %d", dataToBeSent);
        if (dataToBeSent > -TURN_DEADZONE && dataToBeSent < TURN_DEADZONE) {
            jumpingSumo.setPilotingPCMDTurn(OFF);
            jumpingSumo.setPilotingPCMDFlag(ON);
            return;
        }
        jumpingSumo.setPilotingPCMDTurn(dataToBeSent);
        jumpingSumo.setPilotingPCMDFlag(ON);
    }

    public void neutral() {
        jumpingSumo.setPilotingPCMDSpeed(OFF);
        jumpingSumo.setPilotingPCMDTurn(OFF);
        jumpingSumo.setPilotingPCMDFlag(OFF);
    }


    @Override
    // called when the state of the device controller has changed
    public void onStateChanged(ARDeviceController deviceController,
                               ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error) {
        if (newState != null) Log.i(TAG, "onStateChanged: " + newState.toString());
        if (error != null) Log.e(TAG, "onStateChanged: " + error.toString());
    }

    @Override
    public void onExtensionStateChanged(ARDeviceController arDeviceController,
                                        ARCONTROLLER_DEVICE_STATE_ENUM newState,
                                        ARDISCOVERY_PRODUCT_ENUM product, String s,
                                        ARCONTROLLER_ERROR_ENUM error) {
        if (newState != null)
            Log.i(TAG, "onExtensionStateChanged: " + newState.toString() + ", " + composeDesc(product, s));
        if (error != null)
            Log.e(TAG, "onExtensionStateChanged: " + error.toString() + ", " + composeDesc(product, s));
    }

    private String composeDesc(ARDISCOVERY_PRODUCT_ENUM ardiscovery_product_enum, String s) {
        return "product: " + ardiscovery_product_enum.toString() + " , s: " + s;
    }

    @Override
    // called when a command has been received from the drone
    public void onCommandReceived(ARDeviceController deviceController,
                                  ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {
        if (commandKey == null) throw new RuntimeException("Received a null command!");
        if (commandKey
                == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED) {
            if (elementDictionary != null) {
                ARControllerArgumentDictionary<Object> args =
                        elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args == null) throw new RuntimeException("Received a null args dictionary!");
                Integer batValue = (Integer) args.get(
                        ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED_PERCENT);

                // do what you want with the battery level
                trace("Battery: %d", batValue);
                if (batValue != null) batteryLevel = batValue.intValue();
            } else {
                Log.e(TAG, "elementDictionary is null");
            }
            if (battery != null) battery.onNext(batteryLevel);
        } else {
            Log.i(TAG, "Command: "+ commandKey.name());
        }
    }

    private Subscriber<? super Integer> battery;

    public Observable<Integer> batteryLevel() {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                battery = subscriber;
                subscriber.onNext(batteryLevel);
                subscriber.add(Subscriptions.create(() -> battery = null));
            }
        });
    }

    private void trace(String s, Object... args) {
        if (DEBUG) Log.d(TAG, String.format(s, args));
    }
}
