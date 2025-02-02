package com.frogdesign.akart.view

//import org.opencv.core.Scalar
//import org.opencv.samples.colorblobdetect.ColorBlobDetectionActivity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.frogdesign.akart.util.scaleCenterCrop
import rx.Observable
import rx.Subscription
import rx.subjects.BehaviorSubject
import timber.log.Timber

//import rx.lang.kotlin.BehaviourSubject
//import rx.lang.kotlin.PublishSubject

class CameraView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : View(context, attrs, defStyleAttr, defStyleRes) {

    constructor(ctx: Context, attrs: AttributeSet, @Suppress("UNUSED_PARAMETER") defStyleAttr: Int) : this(ctx, attrs, 0, 0) {
    }

    constructor(ctx: Context, attrs: AttributeSet) : this(ctx, attrs, 0, 0) {
    }

    constructor(ctx: Context) : this(ctx, null, 0, 0) {
    }


    private val paint = Paint()
    private var image: Bitmap? = null
    val drawMatrix = Matrix()
    private val drawMatrixInverse = Matrix()
    private val values = FloatArray(9)

    init {
        //        paint.color = Color.RED
        //        paint.alpha = 50
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(0, widthMeasureSpec)
        val h = resolveSize(0, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas?) {
        //canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint);
        if (image != null) canvas?.drawBitmap(image, drawMatrix, paint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //        Timber.i("CONTOUR", "you touched "+event!!.x+", "+event.y)
        val point = floatArrayOf(event!!.x, event.y)
        drawMatrixInverse.mapPoints(point)
        //        Timber.i("CONTOUR", "you inverted "+point.get(0)+","+point.get(1))
        val color = image?.getPixel(point[0].toInt(), point[1].toInt());
        // val violet = ColorBlobDetectionActivity.converScalarRgba2Hsv(color);
        if (color != null) Timber.i("TOUCHED: (" + Integer.toHexString(color) + ")");
        //Timber.i("CameraView", "TOUCHED: (" + violet.`val`[0] + ", " + violet.`val`[1] +", " + violet.`val`[2] + ", " + violet.`val`[3] +")");
        return super.onTouchEvent(event)
    }

    fun setImage(bmp: Bitmap?) {

        if (bmp != null) {
            if (!sameSize(image, bmp)) {
                var viewBounds = scaleCenterCrop(bmp, width, height)
                drawMatrix.reset()
                drawMatrix.postScale(viewBounds.bottom, viewBounds.bottom)
                drawMatrix.postTranslate(viewBounds.left, viewBounds.top)
                drawMatrix.invert(drawMatrixInverse)
                drawMatrix.getValues(values)
                xImageInsets.onNext(values[2])
            }
            image = bmp
            invalidate()
        }
    }

    val xImageInsets = BehaviorSubject.create<Float>()

    private fun sameSize(a: Bitmap?, b: Bitmap?): Boolean {
        if (a != null && b != null) {
            //both non-null
            return (a.width == b.width) and (a.height == b.height)
        }
        if (a == null && b != null) return false
        if (a != null && b == null) return false
        /*a == null && b == null*/
        return true;
    }

    fun link(obs: Observable<Bitmap>): Subscription {
        return obs.subscribe { b ->
            setImage(b)
        }
    }
}
