package com.frogdesign.akart.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import rx.Observable
import rx.Subscription
import rx.subjects.BehaviorSubject

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
    private val drawMatrix = Matrix()
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

    fun setImage(bmp: Bitmap?) {

        if (bmp != null) {
            if (!sameSize(image, bmp)) {
                var bmpBounds = RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
                var viewBounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
                drawMatrix.setRectToRect(bmpBounds, viewBounds, Matrix.ScaleToFit.CENTER)
                drawMatrix.getValues(values)
                xImageInsets.onNext(values[2])
            }
            image = bmp
            invalidate()
        }
    }


    val xImageInsets = BehaviorSubject.create<Float>()

    private fun sameSize(a :Bitmap?, b: Bitmap?) : Boolean {
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
