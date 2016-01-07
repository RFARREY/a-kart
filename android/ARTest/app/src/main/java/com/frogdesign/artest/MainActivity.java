package com.frogdesign.artest;

import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.frogdesign.arsdk.Controller;
import com.frogdesign.arsdk.Discovery;
import com.frogdesign.arsdk.TestUtils;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;

import org.artoolkit.ar.base.ARToolKit;
import org.artoolkit.ar.base.NativeInterface;
import org.artoolkit.ar.base.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ImageView image;
    private Discovery discovery;
    private int markerID;
    private int markerIDHijo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = (ImageView) findViewById(R.id.image);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tutto);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        discovery = new Discovery(getBaseContext());
        TestUtils.constantProducer(byteArray, 16) //jumping sumo 15 FPS
                .sample(16, TimeUnit.MILLISECONDS) //our game can run at 30 fps
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(bmpConsumer);
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


        //markerID = ARToolKit.getInstance().addMarker("single;Data/patt.kanji;80");
        markerID = ARToolKit.getInstance().addMarker("single_barcode;1;80");
        if (markerID < 0) throw new RuntimeException("e' tutto finito1");
        //markerIDHijo = ARToolKit.getInstance().addMarker("single;Data/patt.hiro;80");
        markerIDHijo = ARToolKit.getInstance().addMarker("single_barcode;3;80");
        if (markerIDHijo < 0) throw new RuntimeException("e' tutto finito2");
        ARToolKit.getInstance().initialiseAR(640, 480, "Data/camera_para.dat", 0, true);
        discovery.discoverer().subscribe(new Observer<List<ARDiscoveryDeviceService>>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(List<ARDiscoveryDeviceService> deviceList) {
                Log.i(TAG, "--> SERVICES:");
                // Do what you want with the device list
                for (ARDiscoveryDeviceService service : deviceList) {
                    Log.i(TAG, "The service " + service);
                }
                Log.i(TAG, "<-- SERVICES.");
                Controller ctrl = new Controller(getBaseContext(), deviceList.get(0));
                ctrl.start();
                ctrl.mediaStreamer().observeOn(AndroidSchedulers.mainThread()).subscribe(bmpConsumer);
            }
        });
    }

    private Observer<byte[]> bmpConsumer = new Observer<byte[]>() {

        private Bitmap inBitmap = null;
        private int[] argbBuffer = null;
        private byte[] yuvBuffer = null;
        private BitmapFactory.Options opts =  new BitmapFactory.Options();
        {opts.inMutable = true;}

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }

        private void checkForBuffers(int w, int h) {
            final int argbLength = w*h;
            if (argbBuffer == null || argbBuffer.length != argbLength)
                argbBuffer = new int[argbLength];

            final int yuvLength = Utils.yuvByteLength(w,h);
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
            checkForBuffers(w,h);
            inBitmap.getPixels(argbBuffer, 0, w, 0, 0, w, h);

            Utils.encodeYUV420SP(yuvBuffer, argbBuffer, w,h);
            if (ARToolKit.getInstance().convertAndDetect(yuvBuffer)) {
                onFrameProcessed();
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        discovery.unbind();
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

        if (ARToolKit.getInstance().queryMarkerVisible(markerID)) {
            float[] canaliPercettivi = ARToolKit.getInstance().queryMarkerTransformation(markerID);
            printMatrix(canaliPercettivi);
        }
        if (ARToolKit.getInstance().queryMarkerVisible(markerIDHijo)) {
            float[] canaliPercettivi = ARToolKit.getInstance().queryMarkerTransformation(markerIDHijo);
            printMatrix(canaliPercettivi);
        }
    }
}
