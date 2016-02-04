package com.frogdesign.akart

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import butterknife.bindView
import com.frogdesign.akart.model.Cars
import com.frogdesign.akart.util.*
import com.frogdesign.akart.view.AimView
import com.frogdesign.akart.view.CameraView
import com.frogdesign.akart.view.VerticalSeekBar
import com.jakewharton.rxbinding.view.RxView
import com.jakewharton.rxbinding.widget.RxSeekBar
import com.jakewharton.rxbinding.widget.SeekBarProgressChangeEvent
import com.jakewharton.rxbinding.widget.SeekBarStopChangeEvent
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService
import org.artoolkit.ar.base.ARToolKit
import org.artoolkit.ar.base.BmpToYUVToARToolkitConverterRS
import org.artoolkit.ar.base.BmpToYUVToARToolkitConverterRS2
import org.artoolkit.ar.base.NativeInterface
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class PlayActivity : AppCompatActivity() {

    companion object {
        private val TAG = PlayActivity::class.java.simpleName
        const val EXTRA_DEVICE = "PlayActivity.extra_device"
        const val TRACE = false
    }

    private val camera: CameraView by bindView(R.id.camera)
    private val targets: AimView by bindView(R.id.aim)
    private val gasPedal: VerticalSeekBar by bindView(R.id.gasPedal)
    private val battery: TextView by bindView(R.id.battery)
    private val rear: Button by bindView(R.id.rear)
    private val fireButton: ImageButton by bindView(R.id.fireButton)
    private val stoppedMask: View by bindView(R.id.stopped_mask)

    private var controller: Controller? = null

    private val trackedSubscriptions = TrackedSubscriptions();
    private var comm: Comm? = null

    private var isGameOn = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.play_activity)
        RxView.touches(rear).subscribe { event ->
            Log.i(TAG, "touch " + event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (isGameOn) {
                        controller?.speed(-0.4f)
                        rear.isPressed = true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    controller?.neutral()
                    rear.isPressed = false
                }
            }
        }

        val device = intent.getParcelableExtra<ARDiscoveryDeviceService>(EXTRA_DEVICE)
        controller = Controller(baseContext, device)
        comm = Comm(device.name, this)

        RxSeekBar.changeEvents(gasPedal).subscribe { event ->
            if (event is SeekBarProgressChangeEvent && isGameOn) {
                var progress = event.progress()
                if (progress != 0) controller!!.speed(progress / event.view().max.toFloat())
                else controller!!.neutral()
            } else if (event is SeekBarStopChangeEvent) {
                controller!!.neutral()
                event.view().progress = 0
            }
        }

        trackedSubscriptions.track(comm?.subject?.andAsync()?.subscribe { event ->
            if (event is Comm.Hit) {
                controller!!.hitAnim()
            } else if (event is Comm.GameState) {
                isGameOn = event.on
                updateGameState()
            }
        })

        RxView.clicks(fireButton).subscribe {
            Log.i(TAG, "fire?")
            var s = targets.targetedId
            if (s != null) {
                comm?.boom(s)
                Toast.makeText(this, "Hit "+s+"!", Toast.LENGTH_SHORT).show()
            }
            else Toast.makeText(this, "MISS!", Toast.LENGTH_SHORT).show()
        }

        comm?.connect()
        updateGameState()
    }

    private fun updateGameState() {
        Log.i(TAG, "updateGameState($isGameOn)")
        if (isGameOn) {
            stoppedMask.visibility = View.GONE
        } else {
            stoppedMask.visibility = View.VISIBLE
            controller!!.neutral()
        }
    }

    override fun onStart() {
        super.onStart()
        controller!!.start()

        val FAKE_PRODUCER = false
        var bitmapByteArrayProducer = if (!FAKE_PRODUCER) controller!!.mediaStreamer()
        else {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            TestUtils.constantProducer(byteArray, 60)
        }

        val byteArrayProducer: Observable<ByteArray> =
                bitmapByteArrayProducer.sample(33, TimeUnit.MILLISECONDS)

        val bitmapProducer: Observable<Bitmap> = byteArrayProducer.map(CachedBitmapDecoder())

        var bmpObs = bitmapProducer.andAsync()

        trackedSubscriptions.track(camera.link(bmpObs))

        var bitmapSubscription = bmpObs
                .sample(33, TimeUnit.MILLISECONDS)
                .filter(BmpToYUVToARToolkitConverterRS2(this))
                .andAsync()
                .subscribe { onFrameProcessed() }
        trackedSubscriptions.track(bitmapSubscription)

        var steerSubscription = SteeringWheel(this).stream().subscribe { steer ->
            trace("steer: %b", isMainThread())
            if (isGameOn) controller?.turn(steer / 90f)
        }
        trackedSubscriptions.track(steerSubscription);

        var batterySubscription = controller!!.batteryLevel()
                .observeOn(AndroidSchedulers.mainThread()).subscribe { bat -> battery.text = Integer.toString(bat) }
        trackedSubscriptions.track(batterySubscription)
        comm?.connect()
    }

    override fun onResume() {
        super.onResume()
        if (!ARToolKit.getInstance().initialiseNativeWithOptions(this.cacheDir.absolutePath, 16, 25)) {
            throw RuntimeException("e' tutto finito")
        }

        NativeInterface.arwSetPatternDetectionMode(NativeInterface.AR_MATRIX_CODE_DETECTION)
        NativeInterface.arwSetMatrixCodeType(NativeInterface.AR_MATRIX_CODE_3x3_HAMMING63)


        for (c in Cars.all) {
            val leftId = ARToolKit.getInstance().addMarker("single_barcode;" + c.lrMarkers.first + ";80")
            val rightId = ARToolKit.getInstance().addMarker("single_barcode;" + c.lrMarkers.second + ";80")
            c.leftAR = leftId
            c.rightAR = rightId
        }
        ARToolKit.getInstance().initialiseAR(640, 480, "Data/camera_para.dat", 0, true)
    }

    override fun onPause() {
        super.onPause()
        ARToolKit.getInstance().cleanup()
    }

    override fun onStop() {
        super.onStop()
        trackedSubscriptions.unsubAll()
        controller!!.stop()
        comm?.close()
    }

    fun onFrameProcessed() {
        targets.nullify()
        for (i in Cars.all.indices) {
            val c = Cars.all[i]
            if (c.isDetected(ARToolKit.getInstance())) {
//                Log.i(TAG, "Car visibile! " + c.id)
//                Log.i(TAG, "Position: " + c.estimatePosition(ARToolKit.getInstance()))
                val p = c.estimatePosition(ARToolKit.getInstance())
                targets.setTarget(c.id, p.x + targets.width / 2, -p.y + targets.height / 2)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.hideNavbar()
    }

    private fun trace(s: String, vararg args: Any) {
        if (TRACE) Log.d(TAG, s.format(args))
    }
}
