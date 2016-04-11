package com.frogdesign.akart

//import org.opencv.samples.colorblobdetect.ColorBlobsDetector
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import butterknife.bindView
import com.frogdesign.akart.model.BoxFaces
import com.frogdesign.akart.model.Cars
import com.frogdesign.akart.util.*
import com.frogdesign.akart.view.AimView
import com.frogdesign.akart.view.CameraView
import com.frogdesign.akart.view.GasPedal
import com.jakewharton.rxbinding.view.RxView
import com.jakewharton.rxbinding.widget.SeekBarProgressChangeEvent
import com.jakewharton.rxbinding.widget.SeekBarStopChangeEvent
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService
import kotlinx.android.synthetic.main.ui_test_activity.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
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
    private val gasPedal: GasPedal by bindView(R.id.gasPedal)
    private val battery: ProgressBar by bindView(R.id.batteryLevel)
    private val fireButton: ImageButton by bindView(R.id.fireButton)
    private val stoppedMask: View by bindView(R.id.stopped_mask)

    private var controller: Controller? = null

    private val trackedSubscriptions = TrackedSubscriptions()
    private var comm: Comm? = null

    private var isGameOn = false
    private var colorBlobsDetector: MarkerDetector? = null;

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.play_activity)

        val device = intent.getParcelableExtra<ARDiscoveryDeviceService>(EXTRA_DEVICE)
        controller = Controller(baseContext, device)
        comm = Comm(device.name, this)


        gasPedal.events.subscribe { event ->
            //Timber.i("EVENT %s", gasPedal.level)
            if (event is SeekBarProgressChangeEvent && isGameOn) {
                var progress = gasPedal.level
                if (progress != 0f) controller!!.speed(progress)
                else controller!!.neutral()
            } else if (event is SeekBarStopChangeEvent) {
                controller!!.neutral()
            }
        }

        trackedSubscriptions.track(comm?.subject?.andAsync()?.subscribe { event ->
            if (event is Comm.Hit) {
                controller!!.hitAnim()
            } else if (event is Comm.GameState) {
                isGameOn = event.on
                updateGameState()
            } else if (event is Comm.Speed) {
                controller!!.maxSpeed(event.percent)
            }
        })

        RxView.clicks(fireButton).subscribe {
            Timber.tag(TAG).i("fire?")
            var gotSomeone = false
            targets.targetedIds.map { id ->
                comm?.boom(id)
                Timber.i("Boom for: %s", id)
                gotSomeone = true
            }
            targets.boxIds.map { id ->
                comm?.boxHit(id)
                Timber.i("BoxHit for: %s", id)
                gotSomeone = true
            }
            if (!gotSomeone) Toast.makeText(this, "MISS!", Toast.LENGTH_SHORT).show()
        }

        comm?.connect()
        updateGameState()


        //    colorBlobsDetector = ColorBlobsDetector()
        colorBlobsDetector = ARMarkerDetector()
        application.registerActivityLifecycleCallbacks(colorBlobsDetector)
    }

    override fun onDestroy() {
        super.onDestroy()
        application.unregisterActivityLifecycleCallbacks(colorBlobsDetector)
    }

    private fun updateGameState() {
        Timber.tag(TAG).i("updateGameState(%s)", isGameOn)
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
        trackedSubscriptions.track(controller!!.status().subscribe { it ->
            if (!it) {
                finish()
            }
        })

        colorBlobsDetector?.setup(this, Cars.all)

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
                .sample(30, TimeUnit.MILLISECONDS)
                .filter({ p0 ->
                    colorBlobsDetector?.process(p0)
                    true
                })
                .andAsync()
                .subscribe { onFrameProcessed() }
        trackedSubscriptions.track(bitmapSubscription)

        var steerSubscription = SteeringWheel(this).stream().subscribe { steer ->
            trace("steer: %b", isMainThread())
            aim.horizonRotate(steer)
            if (isGameOn) controller?.turn(steer / 90f)
        }
        trackedSubscriptions.track(steerSubscription);

        var batterySubscription = controller!!.batteryLevel()
                .observeOn(AndroidSchedulers.mainThread()).subscribe { bat -> battery.progress = bat }
        trackedSubscriptions.track(batterySubscription)
        comm?.connect()
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
            colorBlobsDetector?.setTarget(c, camera.drawMatrix, targets)
        }
        for (i in BoxFaces.all.indices) {
            val c = BoxFaces.all[i]
            colorBlobsDetector?.setBoxFace(c, camera.drawMatrix, targets)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.hideNavbar()
    }

    private fun trace(s: String, vararg args: Any) {
        if (TRACE) Timber.tag(TAG).i(s.format(args))
    }
}
