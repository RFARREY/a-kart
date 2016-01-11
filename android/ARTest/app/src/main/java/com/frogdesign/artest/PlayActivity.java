package com.frogdesign.artest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.frogdesign.akart.model.Car;
import com.frogdesign.akart.model.Cars;
import com.frogdesign.arsdk.TestUtils;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import org.artoolkit.ar.base.ARToolKit;
import org.artoolkit.ar.base.NativeInterface;
import org.artoolkit.ar.base.Utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PlayActivity extends AppCompatActivity {
    private static final String TAG = PlayActivity.class.getSimpleName();

    private static final String EXTRA_DEVICE = TAG + ".extra_device";

    private ImageView image;

    //private Controller controller;

    private Subscription bitmapSubscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = (ImageView) findViewById(R.id.image);


        ARDiscoveryDeviceService device = getIntent().getParcelableExtra(EXTRA_DEVICE);
//        controller = new Controller(getBaseContext(), device);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!ARToolKit.getInstance()
                .initialiseNativeWithOptions(this.getCacheDir().getAbsolutePath(), 16, 25)) {
            throw new RuntimeException("e' tutto finito");
        }

        NativeInterface.arwSetPatternDetectionMode(NativeInterface.AR_MATRIX_CODE_DETECTION);
        NativeInterface.arwSetMatrixCodeType(NativeInterface.AR_MATRIX_CODE_3x3_HAMMING63);


        for (Car c : Cars.all) {
            int leftId = ARToolKit.getInstance().addMarker("single_barcode;" + c.getLrMarkers().getFirst() + ";80");
            int rightId = ARToolKit.getInstance().addMarker("single_barcode;" + c.getLrMarkers().getSecond() + ";80");
            c.setLeftAR(leftId);
            c.setRightAR(rightId);
        }
        ARToolKit.getInstance().initialiseAR(640, 480, "Data/camera_para.dat", 0, true);

        //       controller.start();
        bitmapSubscription = getBitmapProducer()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(bmpConsumer);
    }

    private boolean FAKE_PRODUCER = true;

    private Observable<byte[]> getBitmapProducer() {
        if (FAKE_PRODUCER) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            return TestUtils.constantProducer(byteArray, 60) //jumping sumo 15 FPS
                    .sample(300, TimeUnit.MILLISECONDS); //our game can run at 30 fps
        } else {
            //  return controller.mediaStreamer();
            return null;
        }
    }

    private Observer<byte[]> bmpConsumer = new Observer<byte[]>() {

        private Bitmap inBitmap = null;
        private int[] argbBuffer = null;
        private byte[] yuvBuffer = null;
        private BitmapFactory.Options opts = new BitmapFactory.Options();

        {
            opts.inMutable = true;
        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            Log.e(TAG, "Error in bitmap streaming!", e);
        }

        private void checkForBuffers(int w, int h) {
            final int argbLength = w * h;
            if (argbBuffer == null || argbBuffer.length != argbLength)
                argbBuffer = new int[argbLength];

            final int yuvLength = Utils.yuvByteLength(w, h);
            if (yuvBuffer == null || yuvBuffer.length != yuvLength)
                yuvBuffer = new byte[yuvLength];
        }

        @Override
        public void onNext(final byte[] data) {
            Log.i(TAG, "Next");

            image.setImageBitmap(null);
            opts.inBitmap = inBitmap;
            inBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
            image.setImageBitmap(inBitmap);
            final int w = inBitmap.getWidth();
            final int h = inBitmap.getHeight();
            checkForBuffers(w, h);
            inBitmap.getPixels(argbBuffer, 0, w, 0, 0, w, h);

            Utils.encodeYUV420SP(yuvBuffer, argbBuffer, w, h);
            if (ARToolKit.getInstance().convertAndDetect(yuvBuffer)) {
                onFrameProcessed();
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        ARToolKit.getInstance().cleanup();
        if (bitmapSubscription != null && !bitmapSubscription.isUnsubscribed()) {
            bitmapSubscription.unsubscribe();
            bitmapSubscription = null;
        }
        // controller.stop();
    }

    private void printMatrix(float[] matrix) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("[ ");
        for (int i = 0; i < 4; i++) {
            sb.append("[ ");
            for (int j = 0; j < 4; j++) {
                int index = i * 4 + j;
                sb.append(Float.toString(matrix[index]));
                if (j < 3) sb.append(", ");
            }
            sb.append(" ]\n");
        }
        sb.append(" ]");
        //Log.d("MATRIX", sb.toString());
        Log.i("POS", matrix[12] + ", " + matrix[13] + ", " + matrix[14]);
    }

    public void onFrameProcessed() {
        for (int i = 0; i < Cars.all.size(); i++) {
            Car c =  Cars.all.get(i);
            if (c.isDetected(ARToolKit.getInstance())) {
                Log.i(TAG, "Car visibile! " + c.getId());
                Log.i(TAG, "Position: "+c.estimatePosition(ARToolKit.getInstance()));
            }
        }
//        if (ARToolKit.getInstance().queryMarkerVisible(markerID)) {
//            float[] canaliPercettivi = ARToolKit.getInstance().queryMarkerTransformation(markerID);
//            printMatrix(canaliPercettivi);
//        }
//        if (ARToolKit.getInstance().queryMarkerVisible(markerIDHijo)) {
//            float[] canaliPercettivi = ARToolKit.getInstance().queryMarkerTransformation(markerIDHijo);
//            printMatrix(canaliPercettivi);
//        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }
}
